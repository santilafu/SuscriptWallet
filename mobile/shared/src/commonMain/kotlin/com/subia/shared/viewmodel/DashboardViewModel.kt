package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.cache.CacheRepository
import com.subia.shared.model.DashboardSummary
import com.subia.shared.model.Subscription
import com.subia.shared.network.SessionExpiredException
import com.subia.shared.repository.DashboardRepository
import com.subia.shared.repository.SubscriptionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val resumen: DashboardSummary) : DashboardUiState
    data class Error(val mensaje: String) : DashboardUiState
    data class Offline(val resumenCacheado: DashboardSummary?) : DashboardUiState
    data object SesionExpirada : DashboardUiState
}

private const val CACHE_KEY_DASHBOARD = "dashboard_summary"
private const val CACHE_KEY_SUBS = "dashboard_subscriptions"

/**
 * ViewModel para el panel principal con totales de gasto y renovaciones próximas.
 *
 * Implementa la estrategia stale-while-revalidate usando [CacheRepository]:
 * - Al iniciarse, muestra inmediatamente los datos en caché (si existen).
 * - En paralelo lanza peticiones de red y actualiza la UI y la caché con datos frescos.
 * - Calcula [totalesPorMoneda] y [gastosPorCategoria] agrupando las suscripciones por divisa/categoría
 *   en el cliente.
 */
class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _totalesPorMoneda = MutableStateFlow<Map<String, Double>>(emptyMap())
    /** Mapa de divisa → gasto mensual normalizado. Se calcula en el cliente a partir de la lista de suscripciones. */
    val totalesPorMoneda: StateFlow<Map<String, Double>> = _totalesPorMoneda.asStateFlow()

    private val _gastosPorCategoria = MutableStateFlow<Map<String, Double>>(emptyMap())
    /**
     * Mapa de "nombre-categoría (moneda)" → gasto mensual normalizado.
     * Se calcula agrupando suscripciones por categoría y divisa para evitar conversiones de divisas.
     */
    val gastosPorCategoria: StateFlow<Map<String, Double>> = _gastosPorCategoria.asStateFlow()

    init { cargarEstadisticas() }

    /** Carga las estadísticas del dashboard y los totales por divisa en paralelo. */
    fun cargarEstadisticas() {
        viewModelScope.launch {
            // --- Stale-while-revalidate: emitir caché inmediatamente ---
            val cachedSummaryJson = cacheRepository.getString(CACHE_KEY_DASHBOARD)
            val cachedSubsJson = cacheRepository.getString(CACHE_KEY_SUBS)

            if (cachedSummaryJson != null) {
                val cachedSummary = runCatching { json.decodeFromString<DashboardSummary>(cachedSummaryJson) }.getOrNull()
                if (cachedSummary != null) {
                    _uiState.value = DashboardUiState.Success(cachedSummary)
                }
            }
            if (cachedSubsJson != null) {
                val cachedSubs = runCatching { json.decodeFromString<List<Subscription>>(cachedSubsJson) }.getOrNull()
                if (cachedSubs != null) {
                    _totalesPorMoneda.value = calcularTotalesPorMoneda(cachedSubs)
                    _gastosPorCategoria.value = calcularGastosPorCategoria(cachedSubs)
                }
            }

            // --- Peticiones de red en paralelo ---
            val statsDeferred = async { dashboardRepository.getStats() }
            val subsDeferred = async { subscriptionRepository.getAll() }

            val statsResult = statsDeferred.await()
            val subsResult = subsDeferred.await()

            statsResult
                .onSuccess { resumen ->
                    _uiState.value = DashboardUiState.Success(resumen)
                    cacheRepository.saveString(CACHE_KEY_DASHBOARD, json.encodeToString(resumen))
                    cacheRepository.saveTimestamp(CACHE_KEY_DASHBOARD)
                }
                .onFailure { error ->
                    when (error) {
                        is SessionExpiredException -> _uiState.value = DashboardUiState.SesionExpirada
                        else -> {
                            // Si ya emitimos caché, pasar a Offline; si no, Error
                            val estadoActual = _uiState.value
                            if (estadoActual !is DashboardUiState.Success) {
                                _uiState.value = DashboardUiState.Error(
                                    error.message ?: "Error al cargar los datos"
                                )
                            } else {
                                _uiState.value = DashboardUiState.Offline(
                                    (estadoActual as? DashboardUiState.Success)?.resumen
                                )
                            }
                        }
                    }
                }

            subsResult.onSuccess { subs ->
                _totalesPorMoneda.value = calcularTotalesPorMoneda(subs)
                _gastosPorCategoria.value = calcularGastosPorCategoria(subs)
                cacheRepository.saveString(CACHE_KEY_SUBS, json.encodeToString(subs))
                cacheRepository.saveTimestamp(CACHE_KEY_SUBS)
            }
        }
    }

    /** Refresca los datos (p.ej. tras pull-to-refresh). */
    fun refrescar() = cargarEstadisticas()

    /**
     * Agrupa las suscripciones por divisa sumando el gasto mensual normalizado.
     * Las suscripciones anuales se dividen entre 12 para obtener el equivalente mensual.
     * El orden de salida es: EUR primero, USD segundo, resto por orden alfabético.
     */
    internal fun calcularTotalesPorMoneda(subs: List<Subscription>): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (sub in subs) {
            val mensual = if (sub.periodoFacturacion == "YEARLY") sub.precio / 12.0 else sub.precio
            result[sub.moneda] = (result[sub.moneda] ?: 0.0) + mensual
        }
        return result.entries
            .sortedWith(compareBy { entry ->
                when (entry.key) {
                    "EUR" -> "0"
                    "USD" -> "1"
                    else -> "2${entry.key}"
                }
            })
            .associate { it.key to it.value }
    }

    /**
     * Agrupa las suscripciones por nombre de categoría y divisa, sumando el gasto mensual normalizado.
     * Si una categoría tiene gastos en varias divisas, aparece como entradas separadas
     * del tipo "NombreCategoria (EUR)", "NombreCategoria (USD)", etc.
     * Las categorías sin nombre se omiten.
     * El resultado se ordena de mayor a menor gasto.
     */
    internal fun calcularGastosPorCategoria(
        subs: List<Subscription>,
        nombreCategoria: Map<Long, String> = emptyMap()
    ): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (sub in subs) {
            val catId = sub.categoriaId ?: continue
            val catNombre = nombreCategoria[catId] ?: "Categoría $catId"
            val mensual = if (sub.periodoFacturacion == "YEARLY") sub.precio / 12.0 else sub.precio
            val clave = "$catNombre (${sub.moneda})"
            result[clave] = (result[clave] ?: 0.0) + mensual
        }
        return result.entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }
}
