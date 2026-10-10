package com.subia.shared.model

import kotlinx.serialization.Serializable

/**
 * Tokens JWT devueltos por el servidor tras autenticación o refresco. [role] ("USER" | "ADMIN")
 * lo envían login, refresh y Google desde la ronda 2 de la auditoría; `null` con backends
 * anteriores (la app lo lee entonces del claim `role` del access token).
 */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val role: String? = null
)

/** Wrapper genérico de la API: { "data": T, "error": null } */
@Serializable
data class ApiResponse<T>(
    val data: T? = null
)

/** Petición de login con credenciales. */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

/** Petición de refresco de token. */
@Serializable
data class RefreshRequest(
    val refreshToken: String
)

/** Petición de autenticación con Google idToken. */
@Serializable
data class GoogleAuthRequest(val idToken: String)
