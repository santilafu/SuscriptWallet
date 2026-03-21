package com.subia.shared.cache

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Factoría de [Settings] respaldada por [android.content.SharedPreferences] en Android.
 * El nombre del fichero de preferencias es "subia_cache" para aislarlo de otras
 * preferencias de la aplicación.
 *
 * @param context Contexto de aplicación necesario para obtener SharedPreferences.
 */
class PlatformSettingsFactory(private val context: Context) {

    /**
     * Crea e instancia [Settings] usando [SharedPreferencesSettings] con el fichero "subia_cache".
     *
     * @return Implementación de [Settings] lista para ser inyectada en [CacheRepository].
     */
    fun create(): Settings =
        SharedPreferencesSettings(
            context.getSharedPreferences("subia_cache", Context.MODE_PRIVATE)
        )
}
