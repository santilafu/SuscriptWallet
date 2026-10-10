package com.subia.shared.repository

import com.subia.shared.model.AuthTokens
import com.subia.shared.model.GoogleAuthRequest
import com.subia.shared.model.LoginRequest
import com.subia.shared.model.RefreshRequest
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiRoutes
import com.subia.shared.network.esAdmin
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
     * Inicia sesión con un idToken de Google ya obtenido por el cliente.
     * En caso de éxito, guarda los tokens en almacenamiento seguro.
     */
    suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        val result = apiClient.post<AuthTokens, GoogleAuthRequest>(
            path = ApiRoutes.GOOGLE_AUTH,
            body = GoogleAuthRequest(idToken),
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
            // El backend espera {"refreshToken": "..."} (LogoutRequest, @NotBlank) y no exige
            // access token. Antes se enviaba un cuerpo vacío: respondía 400 y el refresh token
            // seguía vivo en el servidor 30 días después de cerrar sesión.
            tokenStorage.getTokens()?.refreshToken?.let { refresh ->
                apiClient.post<Unit, RefreshRequest>(ApiRoutes.LOGOUT, RefreshRequest(refresh), authenticated = false)
            }
        } catch (_: Exception) {
            // Logout local garantizado incluso sin red
        } finally {
            tokenStorage.clearTokens()
        }
    }

    /** Comprueba si hay una sesión activa (tokens persistidos en el almacenamiento seguro). */
    fun hasValidSession(): Boolean = tokenStorage.hasTokens()

    /**
     * `true` si el usuario de la sesión es administrador (`role` de la respuesta de auth o, si no
     * llegó, claim `role` del access token). Las
     * categorías son globales y solo un admin puede crearlas: la UI oculta esas acciones al resto.
     * Tras un refresco el token nuevo trae el rol actualizado.
     */
    fun esAdmin(): Boolean = esAdmin(tokenStorage.getTokens())

    /** Limpia la sesión local sin llamar al servidor. */
    fun clearSession() = tokenStorage.clearTokens()
}
