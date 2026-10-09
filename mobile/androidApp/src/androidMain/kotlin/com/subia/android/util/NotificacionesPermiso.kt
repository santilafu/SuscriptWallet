package com.subia.android.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.subia.android.worker.NotificadorAvisos

/**
 * Estado del permiso de notificaciones, pedido en contexto (onboarding, primer guardado,
 * Ajustes) y nunca al arrancar la app.
 */
object NotificacionesPermiso {

    /** Clave en "subia_cache": ya se ofreció el permiso al guardar la primera suscripción. */
    private const val PREFS = "subia_cache"
    private const val KEY_PEDIDO_AL_GUARDAR = "notif_perm_pedido_al_guardar"

    /** `true` en Android 13+ (donde POST_NOTIFICATIONS es un permiso de ejecución). */
    val requierePermiso: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Permiso de ejecución concedido (o no hace falta, Android 12 y anteriores). */
    fun permisoConcedido(context: Context): Boolean =
        !requierePermiso || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * `true` si los avisos pueden llegar de verdad: permiso concedido (API 33+), notificaciones
     * de la app activadas y canal de renovaciones no silenciado (antes Ajustes decía
     * "Activados" aunque el usuario hubiera apagado solo ese canal).
     */
    fun estanActivadas(context: Context): Boolean = NotificadorAvisos.puedePublicar(context)

    /**
     * `true` si hay que lanzar el diálogo del sistema (API 33+ y permiso aún no concedido). Si
     * el permiso está concedido pero la app o el canal están apagados, el diálogo no sirve:
     * hay que ir a los ajustes del sistema ([abrirAjustesDelSistema]).
     */
    fun hayQuePedir(context: Context): Boolean = requierePermiso && !permisoConcedido(context)

    /** Si ya se ofreció el permiso tras guardar la primera suscripción (para no insistir). */
    fun yaPedidoAlGuardar(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_PEDIDO_AL_GUARDAR, false)

    fun marcarPedidoAlGuardar(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PEDIDO_AL_GUARDAR, true).apply()
    }

    /** Abre los ajustes de notificaciones de la app en el sistema (para permisos denegados). */
    fun abrirAjustesDelSistema(context: Context) {
        val soloCanalApagado = permisoConcedido(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            !NotificadorAvisos.canalActivado(context)
        val intent = if (soloCanalApagado) {
            // Directo al canal de renovaciones, que es lo único que falta por activar.
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, NotificadorAvisos.CHANNEL_ID)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", context.packageName, null))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
