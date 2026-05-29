package com.subia.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Datos ya localizados para pintar la tarjeta del resumen anual. */
data class ResumenCompartible(
    val titulo: String,
    val totalLabel: String,
    val totalValor: String,
    val subsTexto: String,
    val servicioLabel: String,
    val servicioValor: String?,
    val categoriaLabel: String,
    val categoriaValor: String?,
    val marca: String
)

/**
 * Genera una imagen (1080×1350) de la tarjeta del resumen anual dibujándola con [Canvas]
 * nativo — robusto y sin depender de APIs de captura de Compose — y lanza el selector de
 * "Compartir" del sistema vía [FileProvider].
 */
fun compartirResumenAnual(
    context: Context,
    datos: ResumenCompartible,
    tituloSelector: String
) {
    val bitmap = generarBitmap(datos)
    compartirImagen(context, bitmap, "suscriptwallet_resumen.png", tituloSelector)
}

private fun generarBitmap(d: ResumenCompartible): Bitmap {
    val w = 1080
    val h = 1350
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Fondo degradado índigo → violet (misma paleta de marca).
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xFF6366F1.toInt(), 0xFF8B5CF6.toInt(), 0xFFA78BFA.toInt()),
            null, Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

    val bold = Typeface.create("sans-serif", Typeface.BOLD)
    val black = Typeface.create("sans-serif-black", Typeface.BOLD)
    val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val white = 0xFFFFFFFF.toInt()
    fun whiteAlpha(a: Int) = (a shl 24) or 0x00FFFFFF

    val cx = w / 2f

    fun text(s: String, y: Float, size: Float, tf: Typeface, alpha: Int = 255, letterSpacing: Float = 0f): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = whiteAlpha(alpha)
            textSize = size
            typeface = tf
            textAlign = Paint.Align.CENTER
            this.letterSpacing = letterSpacing
        }
        canvas.drawText(s, cx, y, p)
        return p
    }

    // Título superior
    text(d.titulo.uppercase(), 150f, 44f, bold, alpha = 220, letterSpacing = 0.12f)

    // Bloque cifra protagonista
    text(d.totalLabel, 340f, 42f, medium, alpha = 220)
    text(d.totalValor, 470f, 150f, black)
    text(d.subsTexto, 560f, 44f, medium, alpha = 235)

    // Cajas de servicio / categoría
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = whiteAlpha(38) }
    var top = 680f
    val boxW = 760f
    val boxH = 160f
    val left = (w - boxW) / 2f

    fun caja(label: String, valor: String) {
        canvas.drawRoundRect(RectF(left, top, left + boxW, top + boxH), 32f, 32f, boxPaint)
        text(label, top + 58f, 34f, medium, alpha = 220)
        text(valor, top + 112f, 50f, bold)
        top += boxH + 28f
    }

    d.servicioValor?.let { caja(d.servicioLabel, it) }
    d.categoriaValor?.let { caja(d.categoriaLabel, it) }

    // Marca al pie
    text(d.marca, h - 90f, 34f, bold, alpha = 210, letterSpacing = 0.04f)

    return bitmap
}

/**
 * Guarda [bitmap] como PNG en la caché de la app y lanza el selector de "Compartir".
 * Usa el [FileProvider] del manifiesto (authority `${applicationId}.fileprovider`).
 */
fun compartirImagen(
    context: Context,
    bitmap: Bitmap,
    nombreArchivo: String = "suscriptwallet_resumen.png",
    tituloSelector: String = "Compartir"
) {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(imagesDir, nombreArchivo)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, tituloSelector).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
