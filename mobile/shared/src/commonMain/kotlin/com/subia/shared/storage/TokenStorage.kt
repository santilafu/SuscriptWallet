package com.subia.shared.storage

import com.subia.shared.model.AuthTokens
import com.subia.shared.platform.PlatformContext

/**
 * Almacenamiento seguro de tokens JWT.
 * Android: EncryptedSharedPreferences (Android Keystore).
 * iOS: Keychain Services.
 */
expect class TokenStorage(context: PlatformContext) {
    /** Persiste los tokens JWT en almacenamiento seguro. */
    fun saveTokens(tokens: AuthTokens)
    /** Devuelve los tokens almacenados, o null si no existen. */
    fun getTokens(): AuthTokens?
    /** Elimina todos los tokens del almacenamiento. */
    fun clearTokens()
    /** Comprueba si hay tokens persistidos. */
    fun hasTokens(): Boolean
}
