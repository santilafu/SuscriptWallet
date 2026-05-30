package com.subia.shared.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.subia.shared.model.AuthTokens
import com.subia.shared.platform.PlatformContext

actual class TokenStorage actual constructor(context: PlatformContext) : TokenStorageProvider {

    // EncryptedSharedPreferences.create() puede lanzar al arrancar si el Keystore del
    // dispositivo queda invalidado (cambio de huella/PIN, restauración, actualización de OS)
    // o si el almacén cifrado se corrompe. Antes eso provocaba un crash al abrir la app.
    // Estrategia tolerante a fallos: intentar crear; si falla, borrar el almacén y reintentar;
    // como último recurso, prefs sin cifrar para NO impedir el arranque (el usuario tendrá
    // que volver a iniciar sesión, pero la app abre).
    private val prefs: SharedPreferences = buildPrefs(context.context)

    actual override fun saveTokens(tokens: AuthTokens) {
        prefs.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .apply()
    }

    actual override fun getTokens(): AuthTokens? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        return AuthTokens(accessToken = access, refreshToken = refresh)
    }

    actual override fun clearTokens() {
        prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).apply()
    }

    actual override fun hasTokens(): Boolean =
        prefs.getString(KEY_ACCESS, null) != null && prefs.getString(KEY_REFRESH, null) != null

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val SECURE_PREFS = "subia_secure_prefs"
        private const val FALLBACK_PREFS = "subia_secure_prefs_fallback"

        /**
         * Crea el almacén de tokens sin posibilidad de crashear al arrancar.
         * 1) Intenta EncryptedSharedPreferences.
         * 2) Si falla (Keystore invalidado o fichero corrupto), borra el almacén y reintenta.
         * 3) Si vuelve a fallar, usa SharedPreferences sin cifrar como último recurso.
         */
        private fun buildPrefs(ctx: Context): SharedPreferences =
            try {
                createEncrypted(ctx)
            } catch (e: Throwable) {
                runCatching { ctx.deleteSharedPreferences(SECURE_PREFS) }
                try {
                    createEncrypted(ctx)
                } catch (e2: Throwable) {
                    ctx.getSharedPreferences(FALLBACK_PREFS, Context.MODE_PRIVATE)
                }
            }

        private fun createEncrypted(ctx: Context): SharedPreferences =
            EncryptedSharedPreferences.create(
                ctx,
                SECURE_PREFS,
                MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
    }
}
