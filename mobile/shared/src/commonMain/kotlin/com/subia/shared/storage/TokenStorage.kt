package com.subia.shared.storage

import com.subia.shared.model.AuthTokens
import com.subia.shared.platform.PlatformContext

/**
 * Almacenamiento seguro de tokens JWT.
 * Android: EncryptedSharedPreferences (Android Keystore).
 * iOS: Keychain Services.
 *
 * Implementa [TokenStorageProvider] para permitir su sustitución por fakes en tests.
 */
expect class TokenStorage(context: PlatformContext) : TokenStorageProvider {
    /** Persiste los tokens JWT en almacenamiento seguro. */
    override fun saveTokens(tokens: AuthTokens)
    /** Devuelve los tokens almacenados, o null si no existen. */
    override fun getTokens(): AuthTokens?
    /** Elimina todos los tokens del almacenamiento. */
    override fun clearTokens()
    /** Comprueba si hay tokens persistidos. */
    override fun hasTokens(): Boolean
}
