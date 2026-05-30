package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.GmailDetected
import com.subia.shared.repository.GmailScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados de la pantalla de detección por Gmail. */
sealed interface GmailScanUiState {
    data object Idle : GmailScanUiState
    /** Se ha pedido el ticket; la app debe abrir [connectUrl] en Custom Tab. */
    data class LaunchConsent(val connectUrl: String) : GmailScanUiState
    /** Custom Tab abierta; esperando el deep link de vuelta. */
    data object AwaitingReturn : GmailScanUiState
    data object LoadingResults : GmailScanUiState
    data class Results(val items: List<GmailDetected>) : GmailScanUiState
    data object Empty : GmailScanUiState
    data object Adding : GmailScanUiState
    data class Done(val added: Int) : GmailScanUiState
    data class Error(val message: String) : GmailScanUiState
}

class GmailScanViewModel(
    private val repository: GmailScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GmailScanUiState>(GmailScanUiState.Idle)
    val uiState: StateFlow<GmailScanUiState> = _uiState.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    /** Pide el ticket y emite LaunchConsent para que la UI abra la Custom Tab. */
    fun startScan(months: Int = 12) {
        viewModelScope.launch {
            repository.requestTicket(months)
                .onSuccess { _uiState.value = GmailScanUiState.LaunchConsent(it.connectUrl) }
                .onFailure { _uiState.value = GmailScanUiState.Error("No se pudo iniciar la conexión") }
        }
    }

    /** La UI llama esto cuando ha lanzado la Custom Tab, para mostrar "esperando". */
    fun onConsentLaunched() { _uiState.value = GmailScanUiState.AwaitingReturn }

    /** Llamado al volver por el deep link subia://gmail/done?status=... */
    fun onReturnedFromConsent(status: String?) {
        if (status == "error") {
            _uiState.value = GmailScanUiState.Error("No se completó la conexión con Gmail")
            return
        }
        viewModelScope.launch {
            _uiState.value = GmailScanUiState.LoadingResults
            repository.getResults()
                .onSuccess { items ->
                    if (items.isEmpty()) {
                        _uiState.value = GmailScanUiState.Empty
                    } else {
                        _selectedIds.value = items.map { it.id }.toSet()
                        _uiState.value = GmailScanUiState.Results(items)
                    }
                }
                .onFailure { _uiState.value = GmailScanUiState.Error("No se pudieron cargar los resultados") }
        }
    }

    fun toggle(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun addSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = GmailScanUiState.Adding
            repository.add(ids)
                .onSuccess { _uiState.value = GmailScanUiState.Done(it.added) }
                .onFailure { _uiState.value = GmailScanUiState.Error("No se pudieron añadir") }
        }
    }
}
