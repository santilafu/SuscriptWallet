package com.subia.android.util

import android.content.Context
import androidx.core.content.edit

/**
 * Preferencias de presentación del Inicio. De momento, el modo de la tira de cobros
 * ("14 días" o "Este mes"), para que la elección se mantenga entre sesiones.
 */
object InicioPrefs {
    private const val PREFS = "subia_cache"
    private const val KEY_TIRA_MES = "inicio_tira_mes"

    /** `true` si el usuario dejó la tira en "Este mes"; por defecto, los próximos 14 días. */
    fun tiraEnModoMes(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_TIRA_MES, false)

    fun guardarTiraEnModoMes(context: Context, mes: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_TIRA_MES, mes) }
    }
}
