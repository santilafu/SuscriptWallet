package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.Category
import com.subia.shared.model.Subscription
import com.subia.shared.network.NetworkException
import com.subia.shared.network.SessionExpiredException
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

/**
 * ViewModel para la lista de suscripciones con filtro por categoría y caché offline.
 */
class SuscripcionesViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SuscripcionesUiState>(SuscripcionesUiState.Loading)
    val uiState: StateFlow<SuscripcionesUiState> = _uiState.asStateFlow()

    private var todasLasSuscripciones: List<Subscription> = emptyList()
    private var todasLasCategorias: List<Category> = emptyList()
    private var categoriaFiltro: Long? = null

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _uiState.value = SuscripcionesUiState.Loading
            val subsResult = subscriptionRepository.getAll()
            val catResult = categoryRepository.getAll()

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
                    emitirFiltradas()
                }
            }
        }
    }

    fun filtrarPorCategoria(categoriaId: Long?) {
        categoriaFiltro = categoriaId
        emitirFiltradas()
    }

    fun eliminar(id: Long) {
        viewModelScope.launch {
            subscriptionRepository.delete(id)
                .onSuccess { cargar() }
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
