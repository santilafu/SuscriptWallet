package com.subia.android.worker

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.subia.android.MainActivity
import com.subia.android.R
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.theme.Violet600
import com.subia.android.util.NotificacionesPermiso
import com.subia.shared.model.AvisoRenovacion
import com.subia.shared.model.Subscription
import com.subia.shared.model.TipoAviso
import com.subia.shared.model.idNotificacion
import com.subia.shared.model.proximaRenovacion
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.todayIn
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** Textos ya resueltos de un aviso (título, línea corta y texto expandido). */
data class TextosAviso(val titulo: String, val texto: String, val textoLargo: String)

/**
 * Construye y publica las notificaciones de cobro y de fin de prueba. Sin lógica de "qué toca
 * hoy" (eso es `avisosPendientes` en shared): solo presentación y canal.
 */
object NotificadorAvisos {

    /** Mismo id que en versiones anteriores: cambiarlo dejaría un canal huérfano en Ajustes. */
    const val CHANNEL_ID = "renovaciones"

    private const val GRUPO = "com.subia.android.AVISOS"
    private const val TAG_COBRO = "cobro"
    private const val TAG_PRUEBA = "prueba"
    private const val TAG_RESUMEN = "resumen"
    private const val TAG_TEST = "test"
    private const val ID_RESUMEN = 1
    private const val ID_TEST = 1
    private const val MAX_LINEAS_RESUMEN = 5

    /**
     * Crea (o actualiza el nombre y la descripción de) el canal. Idempotente: se llama al
     * arrancar la app para que el canal exista en los ajustes del sistema desde el primer
     * momento, y de nuevo antes de notificar por si cambió el idioma.
     */
    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.notif_channel_desc) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    /** `false` si el usuario ha silenciado solo el canal de renovaciones en el sistema. */
    fun canalActivado(context: Context): Boolean {
        val canal = NotificationManagerCompat.from(context).getNotificationChannelCompat(CHANNEL_ID)
            ?: return true // aún no creado: se creará con importancia alta
        return canal.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    /** Permiso (Android 13+), notificaciones de la app y canal activados. */
    fun puedePublicar(context: Context): Boolean =
        NotificacionesPermiso.permisoConcedido(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            canalActivado(context)

    /**
     * Publica [avisos] y devuelve las claves de los que se han mostrado de verdad (para no
     * marcar como avisado lo que no llegó a salir).
     */
    @SuppressLint("MissingPermission") // comprobado en puedePublicar (lint no lo ve a través de la llamada)
    fun mostrar(context: Context, avisos: List<AvisoRenovacion>): Set<String> {
        if (avisos.isEmpty() || !puedePublicar(context)) return emptySet()
        crearCanal(context)
        val manager = NotificationManagerCompat.from(context)
        val mostrados = mutableSetOf<String>()
        for (aviso in avisos) {
            val tag = if (aviso.tipo == TipoAviso.COBRO) TAG_COBRO else TAG_PRUEBA
            // tag + id estable por suscripción: el aviso de mañana no pisa el de hoy de otra
            // suscripción (N-08), y un ciclo nuevo de la misma sustituye al anterior.
            val ok = runCatching {
                manager.notify(tag, idNotificacion(aviso.suscripcion.id), construir(context, aviso, enGrupo = true))
            }.isSuccess
            if (ok) mostrados += aviso.clave
        }
        actualizarResumen(context)
        return mostrados
    }

    /**
     * Aviso de prueba desde Ajustes: usa una suscripción real de la caché (o un ejemplo) con el
     * mismo formato que los avisos de verdad. No se registra como avisado.
     *
     * @return `false` si los avisos están desactivados y no se ha podido mostrar.
     */
    @SuppressLint("MissingPermission") // comprobado en puedePublicar
    fun mostrarPrueba(context: Context): Boolean {
        if (!puedePublicar(context)) return false
        crearCanal(context)
        val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val sub = CacheAvisos.leerSuscripciones(context)?.firstOrNull { it.activa }
            ?: Subscription(
                id = 0, nombre = "Netflix", precio = 12.99, moneda = "EUR",
                periodoFacturacion = "MONTHLY", fechaRenovacion = hoy.plus(3, DateTimeUnit.DAY).toString()
            )
        val fecha = proximaRenovacion(sub, hoy) ?: hoy.plus(3, DateTimeUnit.DAY)
        val aviso = AvisoRenovacion(sub, TipoAviso.COBRO, fecha, hoy.daysUntil(fecha))
        return runCatching {
            NotificationManagerCompat.from(context).notify(TAG_TEST, ID_TEST, construir(context, aviso, enGrupo = false))
        }.isSuccess
    }

    /**
     * Quita de la bandeja los avisos de suscripciones que ya no están en [subs] (borradas en la
     * app o en la web): al tocarlos abrirían el detalle de algo que no existe.
     */
    fun retirarAvisosHuerfanos(context: Context, subs: List<Subscription>) {
        val sistema = context.getSystemService(NotificationManager::class.java) ?: return
        val vigentes = subs.map { idNotificacion(it.id) }.toSet()
        val huerfanos = runCatching { sistema.activeNotifications.toList() }.getOrDefault(emptyList())
            .filter { (it.tag == TAG_COBRO || it.tag == TAG_PRUEBA) && it.id !in vigentes }
        if (huerfanos.isEmpty()) return
        val manager = NotificationManagerCompat.from(context)
        huerfanos.forEach { manager.cancel(it.tag, it.id) }
        actualizarResumen(context)
    }

    /** Quita todos los avisos de la bandeja (al cerrar sesión: pueden ser de otra cuenta). */
    fun cancelarTodos(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    /** Textos del aviso en el idioma de la app; también los usa la vista previa del onboarding. */
    fun textos(context: Context, aviso: AvisoRenovacion): TextosAviso {
        val res = context.resources
        val locale = res.configuration.locales[0] ?: Locale.getDefault()
        val sub = aviso.suscripcion
        val importe = formatearImporte(sub.precio, sub.moneda, decimalesDe(sub.moneda), locale)
        val periodo = context.getString(periodoRes(sub.periodoFacturacion))
        val fecha = fechaAviso(aviso.fecha, locale)
        val dias = aviso.diasRestantes.coerceAtLeast(0)
        return when (aviso.tipo) {
            TipoAviso.COBRO -> TextosAviso(
                titulo = when (dias) {
                    0 -> context.getString(R.string.notif_charge_title_today, sub.nombre)
                    1 -> context.getString(R.string.notif_charge_title_tomorrow, sub.nombre)
                    else -> res.getQuantityString(R.plurals.notif_charge_title_in_days, dias, sub.nombre, dias)
                },
                texto = context.getString(R.string.notif_charge_text, importe, periodo, fecha),
                textoLargo = context.getString(R.string.notif_charge_big, importe, periodo, fecha)
            )
            TipoAviso.FIN_PRUEBA -> TextosAviso(
                titulo = when (dias) {
                    0 -> context.getString(R.string.notif_trial_title_today, sub.nombre)
                    1 -> context.getString(R.string.notif_trial_title_tomorrow, sub.nombre)
                    else -> res.getQuantityString(R.plurals.notif_trial_title_in_days, dias, sub.nombre, dias)
                },
                texto = context.getString(R.string.notif_trial_text, importe, periodo),
                textoLargo = context.getString(R.string.notif_trial_big, sub.nombre, fecha, importe, periodo)
            )
        }
    }

    private fun construir(context: Context, aviso: AvisoRenovacion, enGrupo: Boolean): Notification {
        val textos = textos(context, aviso)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_renovacion)
            .setColor(Violet600.toArgb())
            .setContentTitle(textos.titulo)
            .setContentText(textos.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(textos.textoLargo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(intentAbrir(context, aviso.suscripcion.id))
            .setAutoCancel(true)
            // Si el mismo aviso se vuelve a publicar (reintento), se actualiza sin volver a sonar.
            .setOnlyAlertOnce(true)
            .apply { if (enGrupo) setGroup(GRUPO) }
            .build()
    }

    /**
     * Resumen del grupo cuando hay 2 o más avisos en la bandeja (por su cuenta, Android solo
     * los agrupa a partir de 4). Se recalcula con lo que hay activo, no solo con lo publicado ahora.
     */
    @SuppressLint("MissingPermission") // solo se llama tras puedePublicar
    private fun actualizarResumen(context: Context) {
        val sistema = context.getSystemService(NotificationManager::class.java) ?: return
        val hijos = runCatching { sistema.activeNotifications.toList() }.getOrDefault(emptyList())
            .filter { it.notification.group == GRUPO && (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
        val manager = NotificationManagerCompat.from(context)
        if (hijos.size < 2) {
            manager.cancel(TAG_RESUMEN, ID_RESUMEN)
            return
        }
        val titulo = context.resources.getQuantityString(R.plurals.notif_summary_title, hijos.size, hijos.size)
        val lineas = hijos.mapNotNull { it.notification.extras.getCharSequence(Notification.EXTRA_TITLE) }
        val estilo = NotificationCompat.InboxStyle().setBigContentTitle(titulo)
        lineas.take(MAX_LINEAS_RESUMEN).forEach { estilo.addLine(it) }
        val resumen = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_renovacion)
            .setColor(Violet600.toArgb())
            .setContentTitle(titulo)
            .setContentText(lineas.joinToString(" · "))
            .setStyle(estilo)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(GRUPO)
            .setGroupSummary(true)
            // Suenan los avisos individuales; el resumen no duplica el sonido.
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(intentAbrir(context, null))
            .build()
        runCatching { manager.notify(TAG_RESUMEN, ID_RESUMEN, resumen) }
    }

    /**
     * Abre la app; con [suscripcionId] válido, en el detalle de esa suscripción (lo resuelve
     * MainActivity). FLAG_IMMUTABLE es obligatorio desde Android 12.
     */
    private fun intentAbrir(context: Context, suscripcionId: Long?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (suscripcionId != null && suscripcionId > 0) putExtra(MainActivity.EXTRA_SUSCRIPCION_ID, suscripcionId)
        }
        // requestCode distinto por suscripción: con el mismo, Android reutilizaría el
        // PendingIntent (los extras no cuentan para distinguirlos) y todos abrirían la misma.
        val requestCode = if (suscripcionId != null && suscripcionId > 0) idNotificacion(suscripcionId) else 0
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** Decimales propios de la divisa (JPY = 0); 2 si no es un código ISO válido. */
    private fun decimalesDe(moneda: String): Int =
        runCatching { Currency.getInstance(moneda).defaultFractionDigits }.getOrNull()?.takeIf { it >= 0 } ?: 2

    /** Mismo criterio de ciclos que la proyección de cobros de shared (`cicloDe`). */
    private fun periodoRes(periodo: String): Int = when (periodo.trim().uppercase()) {
        "YEARLY", "ANNUAL", "ANNUALLY" -> R.string.notif_every_year
        "QUARTERLY" -> R.string.notif_every_quarter
        "WEEKLY" -> R.string.notif_every_week
        "BIWEEKLY" -> R.string.notif_every_two_weeks
        "DAILY" -> R.string.notif_every_day
        else -> R.string.notif_every_month
    }

    /**
     * "jueves 15 oct" / "Thursday, Oct 15" en el idioma de la app; con año si no es el actual
     * (aviso de un cobro de enero hecho en diciembre).
     */
    private fun fechaAviso(fecha: LocalDate, locale: Locale): String {
        val ingles = locale.language == "en"
        val conAnio = fecha.year != Clock.System.todayIn(TimeZone.currentSystemDefault()).year
        val patron = when {
            ingles && conAnio -> "EEEE, MMM d, yyyy"
            ingles -> "EEEE, MMM d"
            conAnio -> "EEEE d MMM yyyy"
            else -> "EEEE d MMM"
        }
        return fecha.toJavaLocalDate().format(DateTimeFormatter.ofPattern(patron, locale)).replace(".", "")
    }
}
