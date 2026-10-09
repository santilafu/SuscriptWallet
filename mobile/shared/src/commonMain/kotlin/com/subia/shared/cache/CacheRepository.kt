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
     * Borra solo los datos de la cuenta (suscripciones, resumen, categorías y sus marcas de
     * tiempo) y conserva las preferencias del dispositivo que viven en el mismo fichero
     * (umbral de avisos, tema, onboarding…). Se llama al cerrar sesión: si no, el siguiente
     * usuario del móvil veía y recibía avisos de las suscripciones del anterior.
     */
    fun borrarDatosDeUsuario() {
        CLAVES_DATOS_USUARIO.forEach { clave ->
            settings.remove(clave)
            settings.remove("${clave}_ts")
        }
    }

    /**
     * Elimina todos los valores almacenados en esta instancia de [Settings].
     * Debe llamarse en el momento del logout para evitar que datos de un usuario
     * sean visibles por otro usuario en el mismo dispositivo.
     */
    fun clear() {
        settings.clear()
    }

    companion object {
        /** Claves con datos de la cuenta (ver [borrarDatosDeUsuario]). El catálogo es público y se conserva. */
        val CLAVES_DATOS_USUARIO = listOf(
            "subscriptions",
            "dashboard_subscriptions",
            "dashboard_summary",
            "categories"
        )
    }
}
