package com.subia.shared.network

import com.subia.shared.model.AuthTokens
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** Rol de administrador tal y como lo emite el backend en el claim `role` del access token. */
const val ROL_ADMIN = "ADMIN"

private val jsonJwt = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Lee el claim `role` ("USER" | "ADMIN") del access token (JWT) sin verificar la firma.
 *
 * Solo sirve para decidir qué mostrar (p. ej. ocultar "Nueva categoría" a quien no es admin):
 * la autorización real la hace el servidor con el mismo claim, así que manipular el token en el
 * móvil solo mostraría un botón que acabaría en 403. Devuelve `null` si el token no es un JWT o
 * no lleva el claim (tokens emitidos antes de que el backend lo añadiera).
 */
@OptIn(ExperimentalEncodingApi::class)
fun rolDeAccessToken(accessToken: String): String? = runCatching {
    val partes = accessToken.split('.')
    if (partes.size != 3) return null
    // Base64url sin relleno: se completa con '=' para que el decodificador lo acepte.
    val payload = partes[1].let { it + "=".repeat((4 - it.length % 4) % 4) }
    val texto = Base64.UrlSafe.decode(payload).decodeToString()
    (jsonJwt.parseToJsonElement(texto).jsonObject["role"] as? JsonPrimitive)?.contentOrNull
}.getOrNull()

/**
 * `true` si la sesión es de administrador: `role` de la respuesta de login/refresh/Google
 * (`data.role`) y, si el backend no lo envió, el claim `role` del access token. Sin sesión, `false`.
 */
fun esAdmin(tokens: AuthTokens?): Boolean {
    if (tokens == null) return false
    return (tokens.role ?: rolDeAccessToken(tokens.accessToken)) == ROL_ADMIN
}
