package com.subia.android.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

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

    /**
     * `true` si la app puede mostrar notificaciones: permiso concedido (API 33+) y
     * notificaciones no desactivadas por el usuario en el sistema (cualquier API).
     */
    fun estanActivadas(context: Context): Boolean {
        if (requierePermiso) {
            val concedido = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!concedido) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** `true` si hay que lanzar el diálogo del sistema (API 33+ y aún no concedido). */
    fun hayQuePedir(context: Context): Boolean = requierePermiso && !estanActivadas(context)

    /** Si ya se ofreció el permiso tras guardar la primera suscripción (para no insistir). */
    fun yaPedidoAlGuardar(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_PEDIDO_AL_GUARDAR, false)

    fun marcarPedidoAlGuardar(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PEDIDO_AL_GUARDAR, true).apply()
    }

    /** Abre los ajustes de notificaciones de la app en el sistema (para permisos denegados). */
    fun abrirAjustesDelSistema(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
