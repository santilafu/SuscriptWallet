package com.subia.android.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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
