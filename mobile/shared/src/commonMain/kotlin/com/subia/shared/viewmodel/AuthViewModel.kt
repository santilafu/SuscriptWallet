package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val mensaje: String) : AuthUiState
}

/**
 * ViewModel para el flujo de autenticación: login, logout y restauración de sesión.
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /** Comprueba si hay sesión activa en el almacenamiento seguro (sin red). */
    fun checkSession() {
        _isLoggedIn.value = authRepository.hasValidSession()
    }

    /** Inicia sesión con email y contraseña. */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("El correo electrónico y la contraseña son obligatorios")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.login(email, password)
                .onSuccess {
                    _isLoggedIn.value = true
                    _uiState.value = AuthUiState.Success
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(
                        it.message ?: "Correo electrónico o contraseña incorrectos"
                    )
                }
        }
    }

    /** Cierra sesión. Garantiza limpieza local incluso sin red. */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedIn.value = false
            _uiState.value = AuthUiState.Idle
        }
    }

    /** Restablece el estado UI a Idle (p.ej. al volver a la pantalla de login). */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
