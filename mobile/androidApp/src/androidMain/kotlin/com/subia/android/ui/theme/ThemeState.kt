package com.subia.android.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val PREFS_NAME = "subia_cache"
private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"

/**
 * Estado de preferencias de tema, observable por Compose y persistido en SharedPreferences.
 *
 * Es un singleton de proceso para que el cambio hecho en Ajustes recomponga el tema raíz
 * aplicado en MainActivity sin necesidad de recrear la Activity. Inicializar una vez con
 * [load] al arrancar.
 */
object ThemeState {
    /** Si el usuario ha activado Material You (color dinámico). Solo tiene efecto en Android 12+. */
    var dynamicColor by mutableStateOf(false)
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
    }

    fun setDynamicColor(context: Context, enabled: Boolean) {
        dynamicColor = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DYNAMIC_COLOR, enabled)
            .apply()
    }
}
