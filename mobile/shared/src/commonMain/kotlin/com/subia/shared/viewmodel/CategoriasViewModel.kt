package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.Category
import com.subia.shared.network.NetworkException
import com.subia.shared.network.SessionExpiredException
import com.subia.shared.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CategoriasUiState {
    data object Loading : CategoriasUiState
    data class Success(val categorias: List<Category>) : CategoriasUiState
    data class Error(val error: ErrorRemoto) : CategoriasUiState
    data class Offline(val categorias: List<Category>) : CategoriasUiState
    data object SesionExpirada : CategoriasUiState
}

sealed interface CrearCategoriaUiState {
    data object Idle : CrearCategoriaUiState
    data object Loading : CrearCategoriaUiState
    data class Success(val categoria: Category) : CrearCategoriaUiState
    data class Error(val error: CrearCategoriaError) : CrearCategoriaUiState
}

/** Error tipado al crear una categoría; la UI lo traduce a su recurso localizado. */
sealed interface CrearCategoriaError {
    /** El nombre está vacío. */
    data object NombreVacio : CrearCategoriaError
    /** Fallo de red: sin conexión no se pueden crear categorías. */
    data object SinConexion : CrearCategoriaError
    /** Cualquier otro fallo del servidor. */
    data object CreacionFallida : CrearCategoriaError
}

/**
 * ViewModel para la lista de categorías y la creación de nuevas categorías.
 */
class CategoriasViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriasUiState>(CategoriasUiState.Loading)
    val uiState: StateFlow<CategoriasUiState> = _uiState.asStateFlow()

    private val _crearState = MutableStateFlow<CrearCategoriaUiState>(CrearCategoriaUiState.Idle)
    val crearState: StateFlow<CrearCategoriaUiState> = _crearState.asStateFlow()

    private var categoriasEnCache: List<Category> = emptyList()

    init { cargarCategorias() }

    fun cargarCategorias() {
        viewModelScope.launch {
            _uiState.value = CategoriasUiState.Loading
            categoryRepository.getAll()
                .onSuccess { categorias ->
                    categoriasEnCache = categorias
                    _uiState.value = CategoriasUiState.Success(categorias)
                }
                .onFailure { error ->
                    when (error) {
                        is SessionExpiredException -> _uiState.value = CategoriasUiState.SesionExpirada
                        else -> _uiState.value = if (categoriasEnCache.isNotEmpty()) {
                            CategoriasUiState.Offline(categoriasEnCache)
                        } else {
                            CategoriasUiState.Error(ErrorRemoto.desde(error))
                        }
                    }
                }
        }
    }

    fun crearCategoria(nombre: String, color: String = "#6c757d", icon: String = "") {
        if (nombre.isBlank()) {
            _crearState.value = CrearCategoriaUiState.Error(CrearCategoriaError.NombreVacio)
            return
        }
        viewModelScope.launch {
            _crearState.value = CrearCategoriaUiState.Loading
            categoryRepository.create(
                com.subia.shared.model.NuevaCategoriaRequest(nombre = nombre.trim(), color = color, icon = icon)
            )
                .onSuccess { nueva ->
                    _crearState.value = CrearCategoriaUiState.Success(nueva)
                    cargarCategorias()
                }
                .onFailure { error ->
                    _crearState.value = CrearCategoriaUiState.Error(mapearErrorCreacion(error))
                }
        }
    }

    fun resetCrearState() { _crearState.value = CrearCategoriaUiState.Idle }

    companion object {
        /** Traduce el fallo al crear una categoría al error tipado. */
        fun mapearErrorCreacion(error: Throwable): CrearCategoriaError = when (error) {
            is NetworkException -> CrearCategoriaError.SinConexion
            else -> CrearCategoriaError.CreacionFallida
        }
    }
}
