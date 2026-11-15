package com.subia.android.util

import android.content.Context
import androidx.core.content.edit

/**
 * Preferencias de presentación del Inicio: el modo de la tira de cobros ("14 días" o "Este mes")
 * y la vista de "Gasto por categoría" ("Este mes" o "Al año"), para que la elección se mantenga
 * entre sesiones.
 */
object InicioPrefs {
    private const val PREFS = "subia_cache"
    private const val KEY_TIRA_MES = "inicio_tira_mes"
    private const val KEY_CATEGORIAS_ANIO = "inicio_categorias_anio"

    /** `true` si el usuario dejó la tira en "Este mes"; por defecto, los próximos 14 días. */
    fun tiraEnModoMes(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_TIRA_MES, false)

    fun guardarTiraEnModoMes(context: Context, mes: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_TIRA_MES, mes) }
    }

    /** `true` si el usuario dejó "Gasto por categoría" en "Al año"; por defecto, "Este mes". */
    fun categoriasEnAnio(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CATEGORIAS_ANIO, false)

    fun guardarCategoriasEnAnio(context: Context, anio: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_CATEGORIAS_ANIO, anio) }
    }
}
