package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.cache.CacheRepository
import com.subia.shared.model.DashboardSummary
import com.subia.shared.model.ProximaRenovacion
import com.subia.shared.model.ProyeccionCobros
import com.subia.shared.model.Subscription
import com.subia.shared.model.calcularProyeccionCobros
import com.subia.shared.network.SessionExpiredException
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.repository.DashboardRepository
import com.subia.shared.repository.SubscriptionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class TopSuscripcion(
    val nombre: String,
    val gastoMensual: Double,
    val moneda: String
)

/** Gasto mensual normalizado de una categoría en una divisa, con su peso dentro de esa divisa. */
data class GastoCategoria(
    val categoriaId: Long?,
    val nombre: String,
    val moneda: String,
    val gastoMensual: Double,
    val numSuscripciones: Int,
    /** Porcentaje (0..100) sobre el total mensual de la misma divisa. */
    val porcentaje: Int
)

sealed interface DashboardUiState {
    /** Carga inicial sin ningún dato que mostrar. */
    data object Loading : DashboardUiState
    data class Success(val resumen: DashboardSummary) : DashboardUiState
    /** Refresco (pull-to-refresh o vuelta a la pantalla) manteniendo los datos anteriores visibles. */
    data class Refreshing(val resumen: DashboardSummary) : DashboardUiState
    data class Error(val error: ErrorRemoto) : DashboardUiState
    data class Offline(val resumenCacheado: DashboardSummary?) : DashboardUiState
    data object SesionExpirada : DashboardUiState
}

/** Resumen disponible en el estado, sea cual sea su fase (éxito, refresco u offline). */
val DashboardUiState.resumenDisponible: DashboardSummary?
    get() = when (this) {
        is DashboardUiState.Success -> resumen
        is DashboardUiState.Refreshing -> resumen
        is DashboardUiState.Offline -> resumenCacheado
        else -> null
    }

private const val CACHE_KEY_DASHBOARD = "dashboard_summary"
private const val CACHE_KEY_SUBS = "dashboard_subscriptions"

/**
 * ViewModel para el panel principal con totales de gasto y renovaciones próximas.
 *
 * Implementa la estrategia stale-while-revalidate usando [CacheRepository]:
 * - Al iniciarse, muestra inmediatamente los datos en caché (si existen).
 * - En paralelo lanza peticiones de red y actualiza la UI y la caché con datos frescos.
 * - Calcula [totalesPorMoneda], [gastosPorCategoria] y la [proyeccion] de cobros agrupando
 *   las suscripciones por divisa/categoría en el cliente.
 */
class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
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

    private val _gastosPorCategoriaDetalle = MutableStateFlow<List<GastoCategoria>>(emptyList())
    /** Mismo desglose que [gastosPorCategoria] pero tipado (nombre, divisa, nº subs, porcentaje), ordenado de mayor a menor. */
    val gastosPorCategoriaDetalle: StateFlow<List<GastoCategoria>> = _gastosPorCategoriaDetalle.asStateFlow()

    private val _totalesAnualesPorMoneda = MutableStateFlow<Map<String, Double>>(emptyMap())
    /**
     * Mapa de divisa → gasto anual total (gasto mensual × 12).
     * El orden de salida es: EUR primero, USD segundo, resto por orden alfabético.
     */
    val totalesAnualesPorMoneda: StateFlow<Map<String, Double>> = _totalesAnualesPorMoneda.asStateFlow()

    private val _pruebasPorVencer = MutableStateFlow<List<ProximaRenovacion>>(emptyList())
    /**
     * Lista de suscripciones en prueba gratuita que vencen en los próximos 7 días.
     * Se calcula en el cliente a partir de la lista de suscripciones activas.
     */
    val pruebasPorVencer: StateFlow<List<ProximaRenovacion>> = _pruebasPorVencer.asStateFlow()

    private val _topSuscripciones = MutableStateFlow<List<TopSuscripcion>>(emptyList())
    /**
     * Top 5 suscripciones con mayor gasto mensual normalizado.
     * Las anuales se dividen entre 12 para obtener el equivalente mensual,
     * idéntico al cálculo usado en [calcularTotalesPorMoneda].
     */
    val topSuscripciones: StateFlow<List<TopSuscripcion>> = _topSuscripciones.asStateFlow()

    private val _proyeccion = MutableStateFlow<ProyeccionCobros?>(null)
    /**
     * Proyección de cobros (tira de 14 días, 12 meses, totales del mes actual y siguiente)
     * calculada en el cliente con [calcularProyeccionCobros]. `null` hasta que haya suscripciones.
     */
    val proyeccion: StateFlow<ProyeccionCobros?> = _proyeccion.asStateFlow()

    init {
        cargarEstadisticas()
        // Alta, edición o borrado en otra pantalla: el Inicio seguía mostrando los totales y la
        // tira de antes (visto en QA tras crear una suscripción). drop(1): el valor actual al
        // suscribirse no es un cambio nuevo.
        viewModelScope.launch {
            SuscripcionesCambios.version.drop(1).collect { cargarEstadisticas() }
        }
    }

    /** Carga las estadísticas del dashboard y los totales por divisa en paralelo. */
    fun cargarEstadisticas() {
        viewModelScope.launch {
            // Si ya hay datos en pantalla, pasamos a Refreshing para no vaciarla (D-05).
            _uiState.value.resumenDisponible?.let { _uiState.value = DashboardUiState.Refreshing(it) }

            // Lanzamos la carga de categorías en paralelo para poder mapear id→nombre
            // en calcularGastosPorCategoria y evitar el fallback "Categoría N".
            val categoriasDeferred = async { categoryRepository.getAll() }

            // --- Stale-while-revalidate: emitir caché inmediatamente ---
            val cachedSummaryJson = cacheRepository.getString(CACHE_KEY_DASHBOARD)
            val cachedSubsJson = cacheRepository.getString(CACHE_KEY_SUBS)

            if (cachedSummaryJson != null && _uiState.value is DashboardUiState.Loading) {
                val cachedSummary = runCatching { json.decodeFromString<DashboardSummary>(cachedSummaryJson) }.getOrNull()
                if (cachedSummary != null) {
                    _uiState.value = DashboardUiState.Refreshing(cachedSummary)
                }
            }
            // Esperamos el mapa de categorías una sola vez: las llamadas de caché y de red
            // comparten el mismo mapa id→nombre para resolver los nombres reales.
            val nombreCatMap = categoriasDeferred.await().getOrNull().orEmpty()
                .associate { it.id to it.nombre }

            if (cachedSubsJson != null && _proyeccion.value == null) {
                val cachedSubs = runCatching { json.decodeFromString<List<Subscription>>(cachedSubsJson) }.getOrNull()
                if (cachedSubs != null) publicarDerivados(cachedSubs, nombreCatMap)
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
                            // Si ya tenemos datos (caché o carga anterior), pasar a Offline; si no, Error
                            val previo = _uiState.value.resumenDisponible
                            _uiState.value = if (previo == null) {
                                DashboardUiState.Error(ErrorRemoto.desde(error))
                            } else {
                                DashboardUiState.Offline(previo)
                            }
                        }
                    }
                }

            subsResult.onSuccess { subs ->
                publicarDerivados(subs, nombreCatMap)
                cacheRepository.saveString(CACHE_KEY_SUBS, json.encodeToString(subs))
                cacheRepository.saveTimestamp(CACHE_KEY_SUBS)
            }
        }
    }

    /** Publica todos los cálculos derivados de la lista de suscripciones. */
    private fun publicarDerivados(subs: List<Subscription>, nombreCatMap: Map<Long, String>) {
        _totalesPorMoneda.value = calcularTotalesPorMoneda(subs)
        _totalesAnualesPorMoneda.value = calcularTotalesAnualesPorMoneda(subs)
        _gastosPorCategoria.value = calcularGastosPorCategoria(subs, nombreCatMap)
        _gastosPorCategoriaDetalle.value = calcularGastosPorCategoriaDetalle(subs, nombreCatMap)
        _pruebasPorVencer.value = calcularPruebasPorVencer(subs)
        _topSuscripciones.value = calcularTopSuscripciones(subs)
        _proyeccion.value = calcularProyeccionCobros(subs, hoy())
    }

    private fun hoy(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

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
            result[sub.moneda] = (result[sub.moneda] ?: 0.0) + gastoMensualNormalizado(sub)
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
     * Agrupa las suscripciones por divisa sumando el gasto **anual** total (gasto mensual × 12).
     * Las suscripciones anuales se usan directamente; las mensuales se multiplican por 12.
     * El orden de salida es: EUR primero, USD segundo, resto por orden alfabético.
     */
    internal fun calcularTotalesAnualesPorMoneda(subs: List<Subscription>): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (sub in subs) {
            val anual = gastoMensualNormalizado(sub) * 12.0
            result[sub.moneda] = (result[sub.moneda] ?: 0.0) + anual
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
     * Filtra las suscripciones en período de prueba gratuita que vencen en los próximos 7 días
     * y las devuelve como [ProximaRenovacion] para reutilizar el mismo modelo de UI.
     */
    internal fun calcularPruebasPorVencer(subs: List<Subscription>): List<ProximaRenovacion> {
        val hoy = hoy()
        return subs
            .filter { it.esPrueba && !it.fechaFinPrueba.isNullOrBlank() }
            .mapNotNull { sub ->
                val fecha = runCatching { LocalDate.parse(sub.fechaFinPrueba!!) }.getOrNull() ?: return@mapNotNull null
                val dias = hoy.daysUntil(fecha)
                if (dias in 0..7) ProximaRenovacion(
                    id = sub.id,
                    nombre = sub.nombre,
                    precio = sub.precio,
                    fechaRenovacion = sub.fechaFinPrueba!!,
                    diasRestantes = dias
                ) else null
            }
            .sortedBy { it.diasRestantes }
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
            val clave = "$catNombre (${sub.moneda})"
            result[clave] = (result[clave] ?: 0.0) + gastoMensualNormalizado(sub)
        }
        return result.entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }

    /**
     * Desglose tipado por categoría y divisa del gasto mensual normalizado, con el porcentaje
     * sobre el total de su divisa. Las suscripciones sin categoría se agrupan bajo `categoriaId = null`
     * con nombre vacío (la UI pone la etiqueta localizada). Orden: EUR primero y, dentro, de mayor a menor.
     */
    internal fun calcularGastosPorCategoriaDetalle(
        subs: List<Subscription>,
        nombreCategoria: Map<Long, String> = emptyMap()
    ): List<GastoCategoria> {
        val activas = subs.filter { it.activa }
        val totalPorMoneda = activas.groupBy { it.moneda }
            .mapValues { (_, lista) -> lista.sumOf { gastoMensualNormalizado(it) } }
        return activas
            .groupBy { it.categoriaId to it.moneda }
            .map { (clave, lista) ->
                val (catId, moneda) = clave
                val gasto = lista.sumOf { gastoMensualNormalizado(it) }
                val total = totalPorMoneda[moneda] ?: 0.0
                GastoCategoria(
                    categoriaId = catId,
                    nombre = catId?.let { nombreCategoria[it] ?: "Categoría $it" } ?: "",
                    moneda = moneda,
                    gastoMensual = gasto,
                    numSuscripciones = lista.size,
                    porcentaje = if (total > 0) (gasto / total * 100.0 + 0.5).toInt().coerceIn(0, 100) else 0
                )
            }
            .sortedWith(
                compareBy<GastoCategoria> {
                    when (it.moneda) { "EUR" -> "0"; "USD" -> "1"; else -> "2${it.moneda}" }
                }.thenByDescending { it.gastoMensual }
            )
    }

    /**
     * Devuelve las 5 suscripciones con mayor gasto mensual normalizado.
     * Las anuales se dividen entre 12 para obtener el equivalente mensual,
     * idéntico al cálculo usado en [calcularTotalesPorMoneda].
     */
    internal fun calcularTopSuscripciones(subs: List<Subscription>): List<TopSuscripcion> =
        subs
            .map { sub ->
                TopSuscripcion(nombre = sub.nombre, gastoMensual = gastoMensualNormalizado(sub), moneda = sub.moneda)
            }
            .sortedByDescending { it.gastoMensual }
            .take(5)

    /** Equivalente mensual de una suscripción según su ciclo (anual /12, trimestral /3, semanal ×52/12). */
    private fun gastoMensualNormalizado(sub: Subscription): Double = when (sub.periodoFacturacion.uppercase()) {
        "YEARLY" -> sub.precio / 12.0
        "QUARTERLY" -> sub.precio / 3.0
        "WEEKLY" -> sub.precio * 52.0 / 12.0
        else -> sub.precio
    }
}
