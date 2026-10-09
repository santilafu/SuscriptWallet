package com.subia.android.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

/**
 * Idioma elegido en Ajustes para el código que corre fuera de una Activity (worker de avisos,
 * canal de notificaciones).
 *
 * En Android 13+ el sistema aplica el idioma por app a todo el proceso y no hace falta nada.
 * En Android 8-12 AppCompat solo lo aplica a las Activities: el worker usaba los recursos del
 * sistema y los avisos (y el nombre del canal) salían en el idioma del móvil. Además
 * `AppCompatDelegate.getApplicationLocales()` no es fiable si el proceso lo arranca
 * WorkManager sin abrir ninguna Activity, así que guardamos una copia propia del tag.
 */
object IdiomaApp {

    private const val PREFS = "subia_idioma"
    private const val KEY_TAG = "tag"

    /** Guarda el idioma elegido ("" = el del sistema). Llamar al cambiarlo en Ajustes. */
    fun guardar(context: Context, tag: String) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TAG, tag).apply()
    }

    /**
     * Copia el idioma que tiene AppCompat (fiable dentro de una Activity) a nuestras prefs.
     * Cubre a quien eligió idioma en una versión anterior, antes de existir esta copia.
     * Una lista vacía no se copia: podría ser que AppCompat aún no la hubiera cargado, y no
     * queremos pisar una elección guardada (volver a "sistema" ya lo guarda Ajustes).
     */
    fun sincronizarDesdeAppCompat(context: Context) {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) guardar(context, locales.toLanguageTags())
    }

    /** Contexto cuyos recursos usan el idioma de la app; el mismo [context] si no hace falta. */
    fun contexto(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
        val tag = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, null)
        if (tag.isNullOrBlank()) return context
        val configuracion = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(tag))
        }
        return context.createConfigurationContext(configuracion)
    }
}
