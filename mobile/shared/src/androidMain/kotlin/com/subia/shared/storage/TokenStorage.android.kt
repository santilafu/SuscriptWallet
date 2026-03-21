package com.subia.shared.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.subia.shared.model.AuthTokens
import com.subia.shared.platform.PlatformContext

actual class TokenStorage actual constructor(context: PlatformContext) : TokenStorageProvider {

    private val prefs = EncryptedSharedPreferences.create(
        context.context,
        "subia_secure_prefs",
        MasterKey.Builder(context.context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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
    }
}
