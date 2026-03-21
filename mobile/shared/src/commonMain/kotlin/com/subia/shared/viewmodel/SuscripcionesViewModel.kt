package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.cache.CacheRepository
import com.subia.shared.model.Category
import com.subia.shared.model.Subscription
import com.subia.shared.network.NetworkException
import com.subia.shared.network.SessionExpiredException
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.repository.SubscriptionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface SuscripcionesUiState {
    data object Loading : SuscripcionesUiState
    data class Success(
        val suscripciones: List<Subscription>,
        val categorias: List<Category>,
        val categoriaSeleccionada: Long?
    ) : SuscripcionesUiState
    data class Error(val mensaje: String) : SuscripcionesUiState
    data class Offline(
        val suscripciones: List<Subscription>,
        val categorias: List<Category>
    ) : SuscripcionesUiState
    data object SesionExpirada : SuscripcionesUiState
}

private const val CACHE_KEY_SUBS = "subscriptions"
private const val CACHE_KEY_CATEGORIES = "categories"

/**
 * ViewModel para la lista de suscripciones con filtro por categoría y caché offline.
 *
 * Implementa la estrategia stale-while-revalidate usando [CacheRepository]:
 * - Al iniciarse, emite inmediatamente los datos en caché (si existen) para evitar
 *   pantalla en blanco durante el cold start.
 * - Lanza peticiones de red en paralelo y actualiza caché y UI al obtener datos frescos.
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

    init { cargar() }

    /** Carga suscripciones y categorías en paralelo, con stale-while-revalidate desde caché. */
    fun cargar() {
        viewModelScope.launch {
            // --- Stale-while-revalidate: emitir caché inmediatamente ---
            val cachedSubsJson = cacheRepository.getString(CACHE_KEY_SUBS)
            val cachedCatsJson = cacheRepository.getString(CACHE_KEY_CATEGORIES)

            if (cachedSubsJson != null) {
                val cachedSubs = runCatching { json.decodeFromString<List<Subscription>>(cachedSubsJson) }.getOrNull()
                val cachedCats = cachedCatsJson?.let {
                    runCatching { json.decodeFromString<List<Category>>(it) }.getOrNull()
                } ?: emptyList()
                if (cachedSubs != null) {
                    todasLasSuscripciones = cachedSubs
                    todasLasCategorias = cachedCats
                    emitirFiltradas()
                }
            }

            // --- Peticiones de red en paralelo ---
            _uiState.value = if (todasLasSuscripciones.isEmpty()) SuscripcionesUiState.Loading
                             else _uiState.value // mantener datos cacheados durante recarga

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
                        SuscripcionesUiState.Offline(todasLasSuscripciones, todasLasCategorias)
                    } else {
                        val error = (subsResult.exceptionOrNull() ?: catResult.exceptionOrNull())
                        SuscripcionesUiState.Error(error?.message ?: "Error al cargar las suscripciones")
                    }
                }
                else -> {
                    todasLasSuscripciones = subsResult.getOrThrow()
                    todasLasCategorias = catResult.getOrThrow()
                    // Actualizar caché con datos frescos
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

    /** Elimina la suscripción con [id] e invalida la caché para forzar recarga. */
    fun eliminar(id: Long) {
        viewModelScope.launch {
            subscriptionRepository.delete(id)
                .onSuccess {
                    cacheRepository.saveString(CACHE_KEY_SUBS, "")
                    cargar()
                }
                .onFailure { error ->
                    val mensaje = when (error) {
                        is NetworkException -> "Sin conexión. No es posible eliminar sin conexión"
                        else -> error.message ?: "Error al eliminar la suscripción"
                    }
                    _uiState.value = SuscripcionesUiState.Error(mensaje)
                }
        }
    }

    private fun emitirFiltradas() {
        val filtradas = if (categoriaFiltro == null) todasLasSuscripciones
        else todasLasSuscripciones.filter { it.categoriaId == categoriaFiltro }
        _uiState.value = SuscripcionesUiState.Success(filtradas, todasLasCategorias, categoriaFiltro)
    }
}
