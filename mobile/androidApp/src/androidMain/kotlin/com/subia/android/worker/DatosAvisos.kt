package com.subia.android.worker

import android.content.Context
import com.subia.shared.model.Subscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Clave de SharedPreferences("subia_cache") con el umbral de días para avisar. */
const val KEY_NOTIFICATION_DAYS_BEFORE = "notification_days_before"
const val DEFAULT_NOTIFICATION_DAYS_BEFORE = 3

/**
 * Lectura de la caché de suscripciones que escriben los ViewModels (misma store que
 * `CacheRepository`: SharedPreferences "subia_cache").
 */
internal object CacheAvisos {

    private const val PREFS_CACHE = "subia_cache"

    /**
     * Las dos claves donde la app guarda la lista completa: la pantalla Suscripciones y el
     * Inicio. Antes el aviso solo leía la primera, que no se actualizaba si el usuario no
     * entraba en esa pestaña (N-07).
     */
    const val CLAVE_SUSCRIPCIONES = "subscriptions"
    private const val CLAVE_SUSCRIPCIONES_INICIO = "dashboard_subscriptions"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    /** Umbral de días elegido en Ajustes (por defecto [DEFAULT_NOTIFICATION_DAYS_BEFORE]). */
    fun umbralDias(context: Context): Int =
        context.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE)
            .getInt(KEY_NOTIFICATION_DAYS_BEFORE, DEFAULT_NOTIFICATION_DAYS_BEFORE)

    /**
     * Lista de suscripciones más reciente de las dos cachés (por su marca de tiempo "_ts"),
     * o `null` si no hay ninguna legible.
     */
    fun leerSuscripciones(context: Context): List<Subscription>? {
        val prefs = context.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE)
        return listOf(CLAVE_SUSCRIPCIONES, CLAVE_SUSCRIPCIONES_INICIO)
            .sortedByDescending { clave -> runCatching { prefs.getLong("${clave}_ts", 0L) }.getOrDefault(0L) }
            .firstNotNullOfOrNull { clave ->
                val texto = prefs.getString(clave, null)
                if (texto.isNullOrBlank()) null
                else runCatching { json.decodeFromString<List<Subscription>>(texto) }.getOrNull()
            }
    }

    fun codificar(subs: List<Subscription>): String = json.encodeToString(subs)
}

/**
 * Registro persistente de avisos ya mostrados (claves "TIPO:id@fechaCobro"), en un fichero
 * propio para que limpiar la caché de datos no lo borre por accidente ni al revés.
 */
internal object RegistroAvisos {

    private const val PREFS = "subia_avisos"
    private const val KEY_AVISADOS = "avisados"

    fun leer(context: Context): Set<String> =
        // Copia: el Set que devuelve SharedPreferences no debe modificarse.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_AVISADOS, emptySet())?.toSet().orEmpty()

    /** `commit` (síncrono): el worker puede terminar y el proceso morir justo después. */
    fun guardar(context: Context, claves: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_AVISADOS, HashSet(claves)).commit()
    }

    fun borrar(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }
}
