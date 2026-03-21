package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.cache.CacheRepository
import com.subia.shared.model.CatalogItem
import com.subia.shared.network.SessionExpiredException
import com.subia.shared.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface CatalogoUiState {
    data object Loading : CatalogoUiState
    data class Success(val items: List<CatalogItem>) : CatalogoUiState
    data class Error(val mensaje: String) : CatalogoUiState
    data class Offline(val items: List<CatalogItem>) : CatalogoUiState
    data object SesionExpirada : CatalogoUiState
}

private const val CACHE_KEY_CATALOG = "catalog"
/** TTL del catálogo: 72 horas. El catálogo raramente cambia. */
private const val CATALOG_TTL_HOURS = 72

/**
 * ViewModel para el catálogo de servicios.
 *
 * La búsqueda es client-side (filtra sobre los datos ya cargados) sin llamadas adicionales a la red.
 * El catálogo se almacena en caché con un TTL de 72 horas mediante [CacheRepository],
 * ya que el contenido del catálogo raramente cambia.
 */
class CatalogoViewModel(
    private val catalogRepository: CatalogRepository,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val _uiState = MutableStateFlow<CatalogoUiState>(CatalogoUiState.Loading)
    val uiState: StateFlow<CatalogoUiState> = _uiState.asStateFlow()

    val busqueda = MutableStateFlow("")

    private var todosLosItems: List<CatalogItem> = emptyList()

    /** Items filtrados por el término de búsqueda (actualización reactiva). */
    val itemsFiltrados: StateFlow<List<CatalogItem>> = combine(
        _uiState, busqueda
    ) { state, query ->
        val items = when (state) {
            is CatalogoUiState.Success -> state.items
            is CatalogoUiState.Offline -> state.items
            else -> emptyList()
        }
        if (query.isBlank()) items
        else items.filter { it.nombre.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { cargarCatalogo() }

    /** Carga el catálogo con stale-while-revalidate: emite caché inmediatamente, luego actualiza desde red. */
    fun cargarCatalogo() {
        viewModelScope.launch {
            // --- Stale-while-revalidate: emitir caché inmediatamente ---
            val cachedJson = cacheRepository.getString(CACHE_KEY_CATALOG)
            if (cachedJson != null) {
                val cachedItems = runCatching { json.decodeFromString<List<CatalogItem>>(cachedJson) }.getOrNull()
                if (cachedItems != null) {
                    todosLosItems = cachedItems
                    _uiState.value = CatalogoUiState.Success(cachedItems)
                }
            }

            // Si la caché está fresca (dentro del TTL de 72 h) y tenemos datos, no recargamos la red
            if (todosLosItems.isNotEmpty() && !cacheRepository.isStale(CACHE_KEY_CATALOG, CATALOG_TTL_HOURS)) {
                return@launch
            }

            catalogRepository.getAll()
                .onSuccess { items ->
                    todosLosItems = items
                    _uiState.value = CatalogoUiState.Success(items)
                    cacheRepository.saveString(CACHE_KEY_CATALOG, json.encodeToString(items))
                    cacheRepository.saveTimestamp(CACHE_KEY_CATALOG)
                }
                .onFailure { error ->
                    when (error) {
                        is SessionExpiredException -> _uiState.value = CatalogoUiState.SesionExpirada
                        else -> _uiState.value = if (todosLosItems.isNotEmpty()) {
                            CatalogoUiState.Offline(todosLosItems)
                        } else {
                            CatalogoUiState.Error(error.message ?: "Error al cargar el catálogo")
                        }
                    }
                }
        }
    }
}
