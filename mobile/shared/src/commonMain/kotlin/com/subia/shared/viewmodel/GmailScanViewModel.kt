package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.GmailDetected
import com.subia.shared.network.NetworkException
import com.subia.shared.repository.GmailScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Error tipado de la detección por Gmail. Sin textos: cada plataforma lo traduce a su
 * recurso localizado (en Android, `R.string`).
 */
sealed interface GmailScanError {
    /** No se pudo pedir el ticket de conexión al servidor. */
    data object NoSePudoIniciar : GmailScanError
    /** El usuario no completó (o canceló) el consentimiento en el navegador. */
    data object ConsentimientoFallido : GmailScanError
    /** El servidor no devolvió los resultados del escaneo. */
    data object ResultadosNoDisponibles : GmailScanError
    /** El alta de las suscripciones seleccionadas falló. */
    data object AltaFallida : GmailScanError
    /** Sin red en cualquiera de los pasos. */
    data object SinConexion : GmailScanError
}

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
    data class Error(val error: GmailScanError) : GmailScanUiState
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
                .onFailure { _uiState.value = GmailScanUiState.Error(mapearError(it, GmailScanError.NoSePudoIniciar)) }
        }
    }

    /** La UI llama esto cuando ha lanzado la Custom Tab, para mostrar "esperando". */
    fun onConsentLaunched() { _uiState.value = GmailScanUiState.AwaitingReturn }

    /** Llamado al volver por el deep link subia://gmail/done?status=... */
    fun onReturnedFromConsent(status: String?) {
        if (status == "error") {
            _uiState.value = GmailScanUiState.Error(GmailScanError.ConsentimientoFallido)
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
                .onFailure { _uiState.value = GmailScanUiState.Error(mapearError(it, GmailScanError.ResultadosNoDisponibles)) }
        }
    }

    fun toggle(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    /** `true` si todos los resultados están marcados. */
    val todoSeleccionado: Boolean
        get() {
            val items = (_uiState.value as? GmailScanUiState.Results)?.items ?: return false
            return items.isNotEmpty() && items.all { it.id in _selectedIds.value }
        }

    /** Marca todos los resultados; si ya estaban todos marcados, los desmarca. */
    fun alternarSeleccionarTodo() {
        val items = (_uiState.value as? GmailScanUiState.Results)?.items ?: return
        _selectedIds.value = if (todoSeleccionado) emptySet() else items.map { it.id }.toSet()
    }

    fun addSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = GmailScanUiState.Adding
            repository.add(ids)
                .onSuccess {
                    SuscripcionesCambios.notificar()
                    _uiState.value = GmailScanUiState.Done(it.added)
                }
                .onFailure { _uiState.value = GmailScanUiState.Error(mapearError(it, GmailScanError.AltaFallida)) }
        }
    }

    companion object {
        /** Sin red → [GmailScanError.SinConexion]; cualquier otro fallo → el error del paso. */
        fun mapearError(error: Throwable, porDefecto: GmailScanError): GmailScanError =
            if (error is NetworkException) GmailScanError.SinConexion else porDefecto
    }
}
