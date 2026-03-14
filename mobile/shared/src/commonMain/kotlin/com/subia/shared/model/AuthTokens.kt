package com.subia.shared.model

import kotlinx.serialization.Serializable

/** Tokens JWT devueltos por el servidor tras autenticación o refresco. */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
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
