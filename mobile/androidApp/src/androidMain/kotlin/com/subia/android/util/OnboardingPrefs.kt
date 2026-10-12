package com.subia.android.util

import android.content.Context
import androidx.core.content.edit

/**
 * Persiste si el usuario ya ha terminado (o saltado) el onboarding. Se consulta al arrancar con
 * sesión y tras el login: mientras no esté marcado, el onboarding vuelve a aparecer.
 */
object OnboardingPrefs {
    private const val PREFS = "subia_cache"
    private const val KEY = "onboarding_completed"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY, true) }
    }
}
