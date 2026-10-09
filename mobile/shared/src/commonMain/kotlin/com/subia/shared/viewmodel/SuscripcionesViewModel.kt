package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.cache.CacheRepository
import com.subia.shared.model.Category
import com.subia.shared.model.Subscription
import com.subia.shared.network.SessionExpiredException
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.repository.SubscriptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface SuscripcionesUiState {
    /** Carga inicial sin ningún dato que mostrar (ni memoria ni caché). */
    data object Loading : SuscripcionesUiState
    /**
     * Datos disponibles. [isRefreshing] indica que hay una recarga en curso en segundo plano:
     * la UI mantiene la lista y muestra solo el indicador de pull-to-refresh, sin parpadeo.
     */
    data class Success(
        val suscripciones: List<Subscription>,
        val categorias: List<Category>,
        val categoriaSeleccionada: Long?,
        val isRefreshing: Boolean = false
    ) : SuscripcionesUiState
    data class Error(val error: SuscripcionesError) : SuscripcionesUiState
    data class Offline(
        val suscripciones: List<Subscription>,
        val categorias: List<Category>
    ) : SuscripcionesUiState
    data object SesionExpirada : SuscripcionesUiState
}

/**
 * Error tipado de la lista de suscripciones: qué operación falló y por qué. La UI elige el
 * texto localizado según la operación y añade la causa (sin conexión, código del servidor).
 */
sealed interface SuscripcionesError {
    val causa: ErrorRemoto
    /** No se pudo cargar la lista. */
    data class CargaFallida(override val causa: ErrorRemoto) : SuscripcionesError
    /** No se pudo eliminar una suscripción (la fila vuelve a la lista). */
    data class EliminacionFallida(override val causa: ErrorRemoto) : SuscripcionesError
}

private const val CACHE_KEY_SUBS = "subscriptions"
private const val CACHE_KEY_CATEGORIES = "categories"

/**
 * ViewModel para la lista de suscripciones con filtro por categoría y caché offline.
 *
 * - Stale-while-revalidate con [CacheRepository]: emite caché al instante y refresca por red.
 * - Distingue carga inicial ([SuscripcionesUiState.Loading]) de refresco
 *   ([SuscripcionesUiState.Success.isRefreshing]) para no parpadear al volver del detalle.
 * - Recarga cuando [SuscripcionesCambios] avisa de un alta/edición/borrado hecho en otra
 *   pantalla, en lugar de recargar en cada `RESUMED`.
 * - Borrado con deshacer: [eliminarConDeshacer] oculta la fila y difiere la petición
 *   [UNDO_WINDOW_MS]; [deshacerEliminacion] la cancela y la fila vuelve.
 */
class SuscripcionesViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val _uiState = MutableStateFlow<SuscripcionesUiState>(SuscripcionesUiState.Loading)
    val uiState: StateFlow<SuscripcionesUiState> = _uiState.asStateFlow()

    private var todasLasSuscripciones: List<Subscription> = emptyList()
    private var todasLasCategorias: List<Category> = emptyList()
    private var categoriaFiltro: Long? = null

    /** Ids ocultos a la espera de confirmar su borrado (ventana de deshacer). */
    private val pendientesDeBorrado = MutableStateFlow<Set<Long>>(emptySet())
    private val trabajosDeBorrado = mutableMapOf<Long, Job>()

    init {
        cargar()
        viewModelScope.launch {
            // drop(1): el valor actual al suscribirse no es un cambio nuevo.
            SuscripcionesCambios.version.drop(1).collect { cargar() }
        }
    }

    /** Carga suscripciones y categorías en paralelo, con stale-while-revalidate desde caché. */
    fun cargar() {
        viewModelScope.launch {
            // --- Stale-while-revalidate: emitir caché inmediatamente ---
            if (todasLasSuscripciones.isEmpty()) {
                val cachedSubsJson = cacheRepository.getString(CACHE_KEY_SUBS)
                val cachedCatsJson = cacheRepository.getString(CACHE_KEY_CATEGORIES)
                if (!cachedSubsJson.isNullOrBlank()) {
                    val cachedSubs = runCatching { json.decodeFromString<List<Subscription>>(cachedSubsJson) }.getOrNull()
                    val cachedCats = cachedCatsJson?.let {
                        runCatching { json.decodeFromString<List<Category>>(it) }.getOrNull()
                    } ?: emptyList()
                    if (cachedSubs != null) {
                        todasLasSuscripciones = cachedSubs
                        todasLasCategorias = cachedCats
                    }
                }
            }

            // Con datos en mano: refresco silencioso. Sin datos: carga inicial.
            if (todasLasSuscripciones.isNotEmpty() || _uiState.value is SuscripcionesUiState.Success) {
                emitirFiltradas(isRefreshing = true)
            } else {
                _uiState.value = SuscripcionesUiState.Loading
            }

            val (subsResult, catResult) = coroutineScope {
                val subsDeferred = async { subscriptionRepository.getAll() }
                val catDeferred = async { categoryRepository.getAll() }
                subsDeferred.await() to catDeferred.await()
            }

            when {
                subsResult.isFailure && subsResult.exceptionOrNull() is SessionExpiredException ->
                    _uiState.value = SuscripcionesUiState.SesionExpirada
                subsResult.isFailure || catResult.isFailure -> {
                    val enCache = todasLasSuscripciones.isNotEmpty()
                    _uiState.value = if (enCache) {
                        SuscripcionesUiState.Offline(visibles(todasLasSuscripciones), todasLasCategorias)
                    } else {
                        val error = (subsResult.exceptionOrNull() ?: catResult.exceptionOrNull())
                        SuscripcionesUiState.Error(SuscripcionesError.CargaFallida(ErrorRemoto.desde(error)))
                    }
                }
                else -> {
                    todasLasSuscripciones = subsResult.getOrThrow()
                    todasLasCategorias = catResult.getOrThrow()
                    cacheRepository.saveString(CACHE_KEY_SUBS, json.encodeToString(todasLasSuscripciones))
                    cacheRepository.saveTimestamp(CACHE_KEY_SUBS)
                    cacheRepository.saveString(CACHE_KEY_CATEGORIES, json.encodeToString(todasLasCategorias))
                    cacheRepository.saveTimestamp(CACHE_KEY_CATEGORIES)
                    emitirFiltradas()
                }
            }
        }
    }

    /** Filtra la lista de suscripciones por [categoriaId]. Pasar `null` muestra todas. */
    fun filtrarPorCategoria(categoriaId: Long?) {
        categoriaFiltro = categoriaId
        emitirFiltradas()
    }

    /** Invalida la caché de suscripciones y recarga desde la red. */
    fun invalidarCacheYRecargar() {
        cacheRepository.saveString(CACHE_KEY_SUBS, "")
        cargar()
    }

    /**
     * Oculta la suscripción y programa su borrado real dentro de [UNDO_WINDOW_MS].
     * Mientras tanto [deshacerEliminacion] puede cancelarlo sin tocar el servidor.
     */
    fun eliminarConDeshacer(id: Long) {
        trabajosDeBorrado.remove(id)?.cancel()
        pendientesDeBorrado.value = pendientesDeBorrado.value + id
        emitirFiltradas()
        trabajosDeBorrado[id] = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            confirmarEliminacion(id)
        }
    }

    /** Cancela un borrado pendiente: la fila vuelve a la lista. */
    fun deshacerEliminacion(id: Long) {
        trabajosDeBorrado.remove(id)?.cancel()
        pendientesDeBorrado.value = pendientesDeBorrado.value - id
        emitirFiltradas()
    }

    /** Ejecuta ya el borrado pendiente de [id] (p. ej. al salir de la pantalla). */
    fun confirmarEliminacion(id: Long) {
        trabajosDeBorrado.remove(id)?.cancel()
        if (id !in pendientesDeBorrado.value) return
        viewModelScope.launch {
            subscriptionRepository.delete(id)
                .onSuccess {
                    pendientesDeBorrado.value = pendientesDeBorrado.value - id
                    todasLasSuscripciones = todasLasSuscripciones.filterNot { it.id == id }
                    cacheRepository.saveString(CACHE_KEY_SUBS, json.encodeToString(todasLasSuscripciones))
                    // La marca de tiempo decide qué caché lee el aviso en segundo plano (la más reciente).
                    cacheRepository.saveTimestamp(CACHE_KEY_SUBS)
                    emitirFiltradas()
                    SuscripcionesCambios.notificar()
                }
                .onFailure { error ->
                    // Falló: la fila vuelve y se informa.
                    pendientesDeBorrado.value = pendientesDeBorrado.value - id
                    _uiState.value = SuscripcionesUiState.Error(errorDeEliminacion(error))
                }
        }
    }

    /** Elimina la suscripción con [id] de inmediato, en segundo plano. */
    fun eliminar(id: Long) {
        viewModelScope.launch { eliminarAhora(id) }
    }

    /**
     * Elimina la suscripción con [id] y espera a la respuesta del servidor (ruta del botón del
     * detalle). El detalle debe navegar atrás DESPUÉS de esto: si lo hacía justo tras llamar a
     * [eliminar], su ViewModel se destruía, la corrutina se cancelaba y el DELETE no llegaba a
     * salir (CancellationException en QA: la suscripción seguía en la lista).
     *
     * @return `true` si se borró; `false` si falló (el error queda en [uiState]).
     */
    suspend fun eliminarAhora(id: Long): Boolean =
        subscriptionRepository.delete(id).fold(
            onSuccess = {
                todasLasSuscripciones = todasLasSuscripciones.filterNot { it.id == id }
                cacheRepository.saveString(CACHE_KEY_SUBS, json.encodeToString(todasLasSuscripciones))
                // La marca de tiempo decide qué caché lee el aviso en segundo plano (la más reciente).
                cacheRepository.saveTimestamp(CACHE_KEY_SUBS)
                SuscripcionesCambios.notificar()
                emitirFiltradas()
                true
            },
            onFailure = { error ->
                _uiState.value = SuscripcionesUiState.Error(errorDeEliminacion(error))
                false
            }
        )

    private fun errorDeEliminacion(error: Throwable): SuscripcionesError =
        SuscripcionesError.EliminacionFallida(ErrorRemoto.desde(error))

    private fun visibles(lista: List<Subscription>): List<Subscription> {
        val ocultas = pendientesDeBorrado.value
        return if (ocultas.isEmpty()) lista else lista.filterNot { it.id in ocultas }
    }

    private fun emitirFiltradas(isRefreshing: Boolean = false) {
        val filtradas = if (categoriaFiltro == null) todasLasSuscripciones
        else todasLasSuscripciones.filter { it.categoriaId == categoriaFiltro }
        _uiState.value = SuscripcionesUiState.Success(visibles(filtradas), todasLasCategorias, categoriaFiltro, isRefreshing)
    }

    companion object {
        /**
         * Ventana de deshacer: algo mayor que la duración larga del Snackbar (10 s). Con la
         * corta (4 s) no daba tiempo a leer el aviso y llegar a "Deshacer" (visto en QA).
         */
        const val UNDO_WINDOW_MS = 10_500L
    }
}
