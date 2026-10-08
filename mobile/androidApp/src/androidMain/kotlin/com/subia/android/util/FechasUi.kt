package com.subia.android.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.datetime.todayIn

/**
 * Convierte una fecha ISO (`yyyy-MM-dd`, como la envía el backend) al formato medio del
 * idioma del usuario ("3 nov 2026", "Nov 3, 2026"). Si no se puede interpretar, devuelve el
 * texto tal cual para no romper la pantalla.
 */
fun fechaIsoLegible(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        LocalDate.parse(iso).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
        )
    }.getOrDefault(iso)
}

/**
 * Próxima renovación de [sub] en ISO (yyyy-MM-dd), avanzando ciclos si la fecha guardada ya pasó.
 * Si la fecha no es válida devuelve la original para no perder información.
 */
fun proximaRenovacionIso(sub: com.subia.shared.model.Subscription): String {
    val hoy = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
    return com.subia.shared.model.proximaRenovacion(sub, hoy)?.toString() ?: sub.fechaRenovacion
}

/**
 * Como [fechaIsoLegible] pero sin el año cuando es el año en curso ("5 nov"): en la fila de la
 * lista el año sobra y hacía saltar el subtítulo a dos líneas junto a importes largos.
 */
fun fechaIsoCorta(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        val fecha = LocalDate.parse(iso)
        if (fecha.year != LocalDate.now().year) return fechaIsoLegible(iso)
        fecha.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())).trimEnd('.')
    }.getOrDefault(iso)
}
