package com.subia.shared.repository

import com.subia.shared.model.AuthTokens
import com.subia.shared.model.LoginRequest
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiRoutes
import com.subia.shared.storage.TokenStorage

/**
 * Repositorio de autenticación: login, logout y consulta de sesión activa.
 */
class AuthRepository(
    private val apiClient: ApiClient,
    private val tokenStorage: TokenStorage
) {
    /**
     * Inicia sesión con email y contraseña.
     * En caso de éxito, guarda los tokens en almacenamiento seguro.
     */
    suspend fun login(email: String, password: String): Result<Unit> {
        val result = apiClient.post<AuthTokens, LoginRequest>(
            path = ApiRoutes.LOGIN,
            body = LoginRequest(email, password),
            authenticated = false
        )
        return result.map { tokens -> tokenStorage.saveTokens(tokens) }
    }

    /**
     * Cierra sesión. Llama al servidor de forma optimista y limpia los tokens locales siempre,
     * incluso si no hay conectividad.
     */
    suspend fun logout() {
        try {
            apiClient.post<Unit, Unit>(ApiRoutes.LOGOUT, Unit)
        } catch (_: Exception) {
            // Logout local garantizado incluso sin red
        } finally {
            tokenStorage.clearTokens()
        }
    }

    /** Comprueba si hay una sesión activa (tokens persistidos en el almacenamiento seguro). */
    fun hasValidSession(): Boolean = tokenStorage.hasTokens()

    /** Limpia la sesión local sin llamar al servidor. */
    fun clearSession() = tokenStorage.clearTokens()
}
