package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.network.ApiException
import com.subia.shared.network.NetworkException
import com.subia.shared.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Error tipado de autenticación. La UI de cada plataforma lo traduce a su recurso
 * localizado; el ViewModel no contiene textos.
 */
sealed interface AuthError {
    /** Email o contraseña vacíos. */
    data object CredencialesVacias : AuthError
    /** El servidor rechazó las credenciales (401/403). */
    data object CredencialesIncorrectas : AuthError
    /** No se recibió idToken de Google. */
    data object TokenGoogleVacio : AuthError
    /** El servidor no pudo verificar la cuenta de Google. */
    data object GoogleNoVerificado : AuthError
    /** Fallo de red o timeout. */
    data object SinConexion : AuthError
    /** Cualquier otro fallo; [detalle] solo para depuración, no para mostrar. */
    data class Desconocido(val detalle: String?) : AuthError
    /** Mensaje ya localizado por la UI (p. ej. errores de CredentialManager en Android). */
    data class Mensaje(val texto: String) : AuthError
}

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val error: AuthError) : AuthUiState
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
        validarCredenciales(email, password)?.let { error ->
            _uiState.value = AuthUiState.Error(error)
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.login(email, password)
                .onSuccess {
                    _isLoggedIn.value = true
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(mapearErrorLogin(it)) }
        }
    }

    /** Inicia sesión con un idToken de Google obtenido por la UI (vía CredentialManager en Android). */
    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = AuthUiState.Error(AuthError.TokenGoogleVacio)
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.loginWithGoogle(idToken)
                .onSuccess {
                    _isLoggedIn.value = true
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { _uiState.value = AuthUiState.Error(mapearErrorGoogle(it)) }
        }
    }

    /**
     * Expone un error surgido fuera del repositorio (p. ej. CredentialManager en Android).
     * El texto llega ya localizado por la UI.
     */
    fun showGoogleError(mensaje: String) {
        _uiState.value = AuthUiState.Error(AuthError.Mensaje(mensaje))
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

    companion object {
        /** Validación pura de credenciales: `null` si son aceptables para enviar. */
        fun validarCredenciales(email: String, password: String): AuthError? =
            if (email.isBlank() || password.isBlank()) AuthError.CredencialesVacias else null

        /** Traduce el fallo del login con contraseña al error tipado. */
        fun mapearErrorLogin(error: Throwable): AuthError = when {
            error is NetworkException -> AuthError.SinConexion
            error is ApiException && (error.statusCode == 401 || error.statusCode == 403) ->
                AuthError.CredencialesIncorrectas
            else -> AuthError.Desconocido(error.message)
        }

        /** Traduce el fallo del login con Google al error tipado. */
        fun mapearErrorGoogle(error: Throwable): AuthError = when (error) {
            is NetworkException -> AuthError.SinConexion
            is ApiException -> AuthError.GoogleNoVerificado
            else -> AuthError.Desconocido(error.message)
        }
    }
}
