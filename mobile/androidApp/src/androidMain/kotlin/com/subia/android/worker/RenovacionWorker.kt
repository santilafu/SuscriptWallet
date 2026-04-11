package com.subia.android.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.subia.shared.model.Subscription
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

/** Clave de SharedPreferences("subia_cache") con el umbral de días para avisar. */
const val KEY_NOTIFICATION_DAYS_BEFORE = "notification_days_before"
const val DEFAULT_NOTIFICATION_DAYS_BEFORE = 3

/**
 * Worker periódico que revisa las próximas renovaciones de suscripciones y lanza
 * una notificación local para aquellas que se renuevan exactamente en 3 días.
 *
 * Lee las suscripciones directamente desde SharedPreferences("subia_cache") para no
 * depender de inyección de dependencias en el contexto del Worker.
 *
 * El canal de notificaciones "renovaciones" se crea con importancia HIGH para que
 * las alertas se muestren como heads-up notifications.
 *
 * En Android 13+ (API 33+), si el permiso POST_NOTIFICATIONS no está concedido,
 * el worker devuelve [Result.success] silenciosamente (degradación elegante).
 */
class RenovacionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    override suspend fun doWork(): Result {
        crearCanalDeNotificaciones()

        // Leer suscripciones directamente desde SharedPreferences (misma store que CacheRepository)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val subsJson = prefs.getString(CACHE_KEY_SUBS, null) ?: return Result.success()

        val suscripciones = runCatching {
            json.decodeFromString<List<Subscription>>(subsJson)
        }.getOrNull() ?: return Result.success()

        val umbralDias = prefs.getInt(KEY_NOTIFICATION_DAYS_BEFORE, DEFAULT_NOTIFICATION_DAYS_BEFORE)
        val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Notificaciones de renovaciones próximas (excluye trials)
        suscripciones
            .filter { sub ->
                if (sub.esPrueba) return@filter false
                val fechaRenovacion = runCatching { LocalDate.parse(sub.fechaRenovacion) }.getOrNull()
                    ?: return@filter false
                val diasRestantes = hoy.daysUntil(fechaRenovacion)
                diasRestantes == umbralDias
            }
            .forEachIndexed { index, sub ->
                lanzarNotificacion(sub, index)
            }

        // Notificaciones de pruebas gratuitas por vencer en el mismo umbral
        suscripciones
            .filter { sub ->
                if (!sub.esPrueba) return@filter false
                val fechaFinPrueba = runCatching { LocalDate.parse(sub.fechaFinPrueba ?: return@filter false) }.getOrNull()
                    ?: return@filter false
                val diasRestantes = hoy.daysUntil(fechaFinPrueba)
                diasRestantes == umbralDias
            }
            .forEachIndexed { index, sub ->
                lanzarNotificacionPrueba(sub, index)
            }

        return Result.success()
    }

    /** Crea el canal de notificaciones "renovaciones" si aún no existe. */
    private fun crearCanalDeNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Renovaciones de suscripciones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas sobre suscripciones próximas a renovarse"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    /**
     * Lanza una notificación para la [suscripcion] indicada.
     * En Android 13+ (API 33), si el permiso POST_NOTIFICATIONS no está concedido,
     * omite la notificación sin lanzar excepción (degradación elegante).
     *
     * @param suscripcion Suscripción próxima a renovarse.
     * @param index       Índice para generar un ID de notificación único.
     */
    private fun lanzarNotificacion(suscripcion: Subscription, index: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permiso != PackageManager.PERMISSION_GRANTED) return
        }

        val fechaFormateada = runCatching {
            val localDate = LocalDate.parse(suscripcion.fechaRenovacion)
            "%02d/%02d/%04d".format(localDate.dayOfMonth, localDate.monthNumber, localDate.year)
        }.getOrElse { suscripcion.fechaRenovacion }

        val notificacion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Renovación próxima: ${suscripcion.nombre}")
            .setContentText(
                "${suscripcion.nombre} se renueva el $fechaFormateada " +
                "por ${"%.2f".format(suscripcion.precio)} ${suscripcion.moneda}"
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Tu suscripción a ${suscripcion.nombre} se renueva el $fechaFormateada. " +
                        "Importe: ${"%.2f".format(suscripcion.precio)} ${suscripcion.moneda}"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + index, notificacion)
    }

    /**
     * Lanza una notificación para una suscripción en período de prueba gratuita próxima a vencer.
     *
     * @param suscripcion Suscripción en prueba próxima a vencer.
     * @param index       Índice para generar un ID de notificación único (base 2500).
     */
    private fun lanzarNotificacionPrueba(suscripcion: Subscription, index: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permiso != PackageManager.PERMISSION_GRANTED) return
        }

        val fechaFormateada = runCatching {
            val localDate = LocalDate.parse(suscripcion.fechaFinPrueba ?: return)
            "%02d/%02d/%04d".format(localDate.dayOfMonth, localDate.monthNumber, localDate.year)
        }.getOrElse { suscripcion.fechaFinPrueba ?: "" }

        val notificacion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Prueba por vencer: ${suscripcion.nombre}")
            .setContentText(
                "Tu prueba gratuita de ${suscripcion.nombre} vence el $fechaFormateada. " +
                "Después se cobrará ${"%.2f".format(suscripcion.precio)} ${suscripcion.moneda}/mes"
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Tu período de prueba gratuita de ${suscripcion.nombre} vence el $fechaFormateada. " +
                        "Si no cancelas, se te cobrará ${"%.2f".format(suscripcion.precio)} ${suscripcion.moneda} al mes."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(TRIAL_NOTIFICATION_ID_BASE + index, notificacion)
    }

    companion object {
        /** Tag único para identificar y cancelar las tareas de WorkManager. */
        const val TAG = "renovaciones"
        private const val PREFS_NAME = "subia_cache"
        private const val CHANNEL_ID = "renovaciones"
        private const val NOTIFICATION_ID_BASE = 1000
        private const val TRIAL_NOTIFICATION_ID_BASE = 2500
        private const val CACHE_KEY_SUBS = "subscriptions"
    }
}
