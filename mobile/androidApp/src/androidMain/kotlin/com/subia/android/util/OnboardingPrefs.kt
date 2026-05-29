package com.subia.android.util

import android.content.Context

/** Persiste si el usuario ya ha visto el onboarding (se muestra una sola vez tras el primer login). */
object OnboardingPrefs {
    private const val PREFS = "subia_cache"
    private const val KEY = "onboarding_completed"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY, true)
            .apply()
    }
}
