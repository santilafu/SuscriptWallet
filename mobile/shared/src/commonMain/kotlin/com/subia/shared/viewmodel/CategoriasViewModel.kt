package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.Category
import com.subia.shared.network.ApiException
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
    /**
     * El servidor lo rechaza por permisos (403): las categorías son comunes a todos los usuarios
     * y en el backend solo un administrador puede crearlas.
     */
    data object SinPermiso : CrearCategoriaError
    /** Cualquier otro fallo del servidor. */
    data object CreacionFallida : CrearCategoriaError
}

/**
 * ViewModel para la lista de categorías y la creación de nuevas categorías.
 */
class CategoriasViewModel(
    private val categoryRepository: CategoryRepository,
    /**
     * Si el usuario es administrador. Las categorías son globales y en el backend solo un admin
     * puede crearlas (403 al resto): la UI oculta "Nueva categoría" a los demás. Por defecto
     * `false` (lo más seguro); en la app lo da [com.subia.shared.repository.AuthRepository.esAdmin].
     */
    private val esAdmin: () -> Boolean = { false }
) : ViewModel() {

    private val _puedeCrear = MutableStateFlow(esAdmin())
    /** `true` si se debe ofrecer "Nueva categoría" (solo administradores). */
    val puedeCrear: StateFlow<Boolean> = _puedeCrear.asStateFlow()

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
                // Tras la petición: si hubo refresco, el token nuevo trae el rol actual.
                .also { _puedeCrear.value = esAdmin() }
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

    fun crearCategoria(nombre: String, color: String = COLOR_POR_DEFECTO, icon: String = ICONO_POR_DEFECTO) {
        if (nombre.isBlank()) {
            _crearState.value = CrearCategoriaUiState.Error(CrearCategoriaError.NombreVacio)
            return
        }
        viewModelScope.launch {
            _crearState.value = CrearCategoriaUiState.Loading
            categoryRepository.create(
                com.subia.shared.model.NuevaCategoriaRequest(
                    nombre = nombre.trim(),
                    color = color.ifBlank { COLOR_POR_DEFECTO },
                    // El backend exige un icono no vacío (@NotBlank).
                    icon = icon.ifBlank { ICONO_POR_DEFECTO }
                )
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
        const val COLOR_POR_DEFECTO = "#6c757d"
        const val ICONO_POR_DEFECTO = "🏷️"

        /** Traduce el fallo al crear una categoría al error tipado. */
        fun mapearErrorCreacion(error: Throwable): CrearCategoriaError = when {
            error is NetworkException -> CrearCategoriaError.SinConexion
            error is ApiException && (error.statusCode == 401 || error.statusCode == 403) ->
                CrearCategoriaError.SinPermiso
            else -> CrearCategoriaError.CreacionFallida
        }
    }
}
