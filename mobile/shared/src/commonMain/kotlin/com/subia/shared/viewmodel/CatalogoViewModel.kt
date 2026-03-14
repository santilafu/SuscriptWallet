package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

sealed interface CatalogoUiState {
    data object Loading : CatalogoUiState
    data class Success(val items: List<CatalogItem>) : CatalogoUiState
    data class Error(val mensaje: String) : CatalogoUiState
    data class Offline(val items: List<CatalogItem>) : CatalogoUiState
    data object SesionExpirada : CatalogoUiState
}

/**
 * ViewModel para el catálogo de servicios.
 * La búsqueda es client-side (filtra sobre los datos ya cargados) sin llamadas adicionales a la red.
 */
class CatalogoViewModel(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

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

    fun cargarCatalogo() {
        viewModelScope.launch {
            _uiState.value = CatalogoUiState.Loading
            catalogRepository.getAll()
                .onSuccess { items ->
                    todosLosItems = items
                    _uiState.value = CatalogoUiState.Success(items)
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
