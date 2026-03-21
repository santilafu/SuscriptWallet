package com.subia.shared.cache

import com.russhwolf.settings.Settings
import kotlinx.datetime.Clock

/**
 * Repositorio de caché persistente basado en [Settings] (multiplatform-settings).
 * Almacena pares clave-valor de tipo String con soporte de TTL por timestamp.
 *
 * En Android, [Settings] está respaldado por SharedPreferences("subia_cache").
 * En iOS, está respaldado por NSUserDefaults.
 */
class CacheRepository(private val settings: Settings) {

    /**
     * Guarda una cadena de texto bajo la [key] indicada.
     *
     * @param key   Clave de almacenamiento.
     * @param value Valor a persistir.
     */
    fun saveString(key: String, value: String) {
        settings.putString(key, value)
    }

    /**
     * Recupera la cadena almacenada bajo [key], o `null` si no existe.
     *
     * @param key Clave de almacenamiento.
     * @return El valor almacenado o `null`.
     */
    fun getString(key: String): String? =
        settings.getStringOrNull(key)

    /**
     * Guarda el timestamp actual (en milisegundos de epoch) bajo la clave "[key]_ts".
     * Debe llamarse inmediatamente después de actualizar los datos para registrar la hora
     * de la última escritura.
     *
     * @param key Clave base cuyos datos se acaban de actualizar.
     */
    fun saveTimestamp(key: String) {
        settings.putLong("${key}_ts", Clock.System.now().toEpochMilliseconds())
    }

    /**
     * Comprueba si los datos de la clave [key] están caducados (stale).
     * Los datos se consideran obsoletos cuando han transcurrido más de [ttlHours] horas
     * desde el último [saveTimestamp].
     *
     * @param key      Clave base de los datos a comprobar.
     * @param ttlHours Tiempo de vida en horas (por defecto: 24 h).
     * @return `true` si los datos están caducados o si nunca se guardó timestamp; `false` si están frescos.
     */
    fun isStale(key: String, ttlHours: Int = 24): Boolean {
        val ts = settings.getLongOrNull("${key}_ts") ?: return true
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val ttlMs = ttlHours * 60L * 60L * 1_000L
        return (nowMs - ts) > ttlMs
    }

    /**
     * Elimina todos los valores almacenados en esta instancia de [Settings].
     * Debe llamarse en el momento del logout para evitar que datos de un usuario
     * sean visibles por otro usuario en el mismo dispositivo.
     */
    fun clear() {
        settings.clear()
    }
}
