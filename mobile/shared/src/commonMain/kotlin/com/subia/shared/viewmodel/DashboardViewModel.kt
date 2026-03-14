package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.DashboardSummary
import com.subia.shared.network.SessionExpiredException
import com.subia.shared.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val resumen: DashboardSummary) : DashboardUiState
    data class Error(val mensaje: String) : DashboardUiState
    data class Offline(val resumenCacheado: DashboardSummary?) : DashboardUiState
    data object SesionExpirada : DashboardUiState
}

/**
 * ViewModel para el panel principal con totales de gasto y renovaciones próximas.
 * Mantiene caché en memoria para mostrar datos cuando no hay conectividad.
 */
class DashboardViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var datosEnCache: DashboardSummary? = null

    init { cargarEstadisticas() }

    /** Carga las estadísticas del dashboard desde el servidor. */
    fun cargarEstadisticas() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            dashboardRepository.getStats()
                .onSuccess { resumen ->
                    datosEnCache = resumen
                    _uiState.value = DashboardUiState.Success(resumen)
                }
                .onFailure { error ->
                    when (error) {
                        is SessionExpiredException -> _uiState.value = DashboardUiState.SesionExpirada
                        else -> _uiState.value = DashboardUiState.Offline(datosEnCache)
                            .takeIf { datosEnCache != null }
                            ?: DashboardUiState.Error(error.message ?: "Error al cargar los datos")
                    }
                }
        }
    }

    /** Refresca los datos (p.ej. tras pull-to-refresh). */
    fun refrescar() = cargarEstadisticas()
}
