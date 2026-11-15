package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.cache.CacheRepository
import com.subia.shared.model.DashboardSummary
import com.subia.shared.model.Periodicidad
import com.subia.shared.model.ProximaRenovacion
import com.subia.shared.model.ProyeccionCobros
import com.subia.shared.model.Subscription
import com.subia.shared.model.calcularProyeccionCobros
import com.subia.shared.model.equivalenteMensualInterno
import com.subia.shared.model.ordenDivisa
import com.subia.shared.model.totalAnualInterno
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
    /**
     * "Gasto mensual medio": mapa de divisa → suma de los equivalentes mensuales (importe /
     * intervalo; un trimestral de 90 € suma 30 €), con 2 decimales. Se calcula en el cliente a
     * partir de la lista de suscripciones. No es lo que se paga este mes: eso es
     * [totalesMesCompletoPorMoneda].
     */
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
     * Mapa de divisa → gasto anual total (importe × 12 / intervalo por gasto, 2 decimales).
     * El orden de salida es: EUR primero, USD segundo, resto por orden alfabético.
     */
    val totalesAnualesPorMoneda: StateFlow<Map<String, Double>> = _totalesAnualesPorMoneda.asStateFlow()

    private val _totalesMesCompletoPorMoneda = MutableStateFlow<Map<String, Double>>(emptyMap())
    /**
     * "Este mes pagas": mapa de divisa → suma de lo que vence en el mes natural entero (del día
     * 1 al último, incluidos los cobros ya pasados). Es [ProyeccionCobros.totalMesCompleto];
     * un trimestral solo suma en los meses en que cobra. Orden EUR, USD, resto alfabético.
     */
    val totalesMesCompletoPorMoneda: StateFlow<Map<String, Double>> = _totalesMesCompletoPorMoneda.asStateFlow()

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

    /**
     * Publica todos los cálculos derivados de la lista de suscripciones. Solo cuentan las
     * **activas**, igual que el backend (`findByUserIdAndActiveTrue`) y la proyección de cobros:
     * antes una suscripción desactivada en la web seguía sumando al gasto mensual, al anual, al
     * top y a las categorías, y el Inicio no cuadraba con "Suscripciones activas".
     */
    private fun publicarDerivados(todas: List<Subscription>, nombreCatMap: Map<Long, String>) {
        val subs = todas.filter { it.activa }
        _totalesPorMoneda.value = calcularTotalesPorMoneda(subs)
        _totalesAnualesPorMoneda.value = calcularTotalesAnualesPorMoneda(subs)
        _gastosPorCategoria.value = calcularGastosPorCategoria(subs, nombreCatMap)
        _gastosPorCategoriaDetalle.value = calcularGastosPorCategoriaDetalle(subs, nombreCatMap)
        _pruebasPorVencer.value = calcularPruebasPorVencer(subs)
        _topSuscripciones.value = calcularTopSuscripciones(subs)
        val proyeccion = calcularProyeccionCobros(subs, hoy())
        _proyeccion.value = proyeccion
        _totalesMesCompletoPorMoneda.value = proyeccion.totalMesCompleto
    }

    private fun hoy(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    /** Refresca los datos (p.ej. tras pull-to-refresh). */
    fun refrescar() = cargarEstadisticas()

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

    companion object {
        /**
         * "Gasto mensual medio" por divisa: suma de los equivalentes mensuales internos
         * (diezmilésimas, ver [Periodicidad]) redondeada a 2 decimales al final. Un trimestral de
         * 10 € suma 3,3333 y tres de ellos dan 10,00, no 9,99.
         * El orden de salida es: EUR primero, USD segundo, resto por orden alfabético.
         */
        internal fun calcularTotalesPorMoneda(subs: List<Subscription>): Map<String, Double> =
            sumarPorMonedaVisible(subs) { it.equivalenteMensualInterno() }

        /**
         * Gasto **anual** por divisa: suma de importe × 12 / intervalo de cada gasto (no
         * mensual × 12: con 10 € trimestral eso daría 39,96 en vez de 40,00), redondeada al final.
         * El orden de salida es: EUR primero, USD segundo, resto por orden alfabético.
         */
        internal fun calcularTotalesAnualesPorMoneda(subs: List<Subscription>): Map<String, Double> =
            sumarPorMonedaVisible(subs) { it.totalAnualInterno() }

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
            val internos = mutableMapOf<String, Long>()
            for (sub in subs) {
                val catId = sub.categoriaId ?: continue
                val catNombre = nombreCategoria[catId] ?: "Categoría $catId"
                val clave = "$catNombre (${sub.moneda})"
                internos[clave] = (internos[clave] ?: 0L) + sub.equivalenteMensualInterno()
            }
            return internos.entries
                .sortedByDescending { it.value }
                .associate { it.key to Periodicidad.aCifraVisible(it.value) }
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
                .mapValues { (_, lista) -> lista.sumOf { it.equivalenteMensualInterno() } }
            return activas
                .groupBy { it.categoriaId to it.moneda }
                .map { (clave, lista) ->
                    val (catId, moneda) = clave
                    val gasto = lista.sumOf { it.equivalenteMensualInterno() }
                    val total = totalPorMoneda[moneda] ?: 0L
                    GastoCategoria(
                        categoriaId = catId,
                        nombre = catId?.let { nombreCategoria[it] ?: "Categoría $it" } ?: "",
                        moneda = moneda,
                        gastoMensual = Periodicidad.aCifraVisible(gasto),
                        numSuscripciones = lista.size,
                        porcentaje = if (total > 0) (gasto.toDouble() / total * 100.0 + 0.5).toInt().coerceIn(0, 100) else 0
                    )
                }
                .sortedWith(
                    compareBy<GastoCategoria> { ordenDivisa(it.moneda) }.thenByDescending { it.gastoMensual }
                )
        }

        /**
         * Devuelve las 5 suscripciones con mayor gasto mensual normalizado (importe / intervalo,
         * 2 decimales), idéntico al cálculo usado en [calcularTotalesPorMoneda].
         */
        internal fun calcularTopSuscripciones(subs: List<Subscription>): List<TopSuscripcion> =
            subs
                .map { sub ->
                    TopSuscripcion(
                        nombre = sub.nombre,
                        gastoMensual = Periodicidad.aCifraVisible(sub.equivalenteMensualInterno()),
                        moneda = sub.moneda
                    )
                }
                .sortedByDescending { it.gastoMensual }
                .take(5)

        /** Suma por divisa los importes internos que da [interno] y redondea cada total al final. */
        private fun sumarPorMonedaVisible(
            subs: List<Subscription>,
            interno: (Subscription) -> Long
        ): Map<String, Double> {
            val internos = mutableMapOf<String, Long>()
            for (sub in subs) {
                internos[sub.moneda] = (internos[sub.moneda] ?: 0L) + interno(sub)
            }
            return internos.entries
                .sortedWith(compareBy { ordenDivisa(it.key) })
                .associate { it.key to Periodicidad.aCifraVisible(it.value) }
        }
    }
}
