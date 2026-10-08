package com.subia.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.subia.android.R
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.text.NumberFormat
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import java.time.format.TextStyle as JavaTextStyle

/**
 * Formatea un importe con el símbolo de su divisa según el locale del usuario
 * ("47,96 €", "$12.00"). Si el código de divisa no es ISO válido, cae a "valor CÓDIGO"
 * para no tirar la pantalla por un dato raro del servidor.
 */
fun formatearImporte(valor: Double, moneda: String = "EUR", decimales: Int = 2): String {
    val locale = Locale.getDefault()
    return runCatching {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(moneda)
            minimumFractionDigits = decimales
            maximumFractionDigits = decimales
        }.format(valor)
    }.getOrElse { "%.${decimales}f %s".format(locale, valor, moneda) }
}

/**
 * Importe compacto para etiquetas de gráfico y celdas estrechas: sin decimales por debajo
 * de 1000 ("48 €", "$300") y en miles con un decimal a partir de ahí ("1,2 k€").
 */
fun formatearImporteCompacto(valor: Double, moneda: String = "EUR"): String {
    if (abs(valor) < 1000) return formatearImporte(valor, moneda, decimales = 0)
    val locale = Locale.getDefault()
    val simbolo = runCatching { Currency.getInstance(moneda).getSymbol(locale) }.getOrDefault(moneda)
    val miles = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }.format(valor / 1000.0)
    return "$miles k$simbolo"
}

/** Símbolo de la divisa en el locale del usuario ("€", "$", "GBP" si no es ISO). */
fun simboloDivisa(moneda: String): String =
    runCatching { Currency.getInstance(moneda).getSymbol(Locale.getDefault()) }.getOrDefault(moneda)

/** Cifras tabulares (`tnum`): los dígitos ocupan el mismo ancho y las columnas de importes alinean. */
val TextStyle.tabular: TextStyle
    get() = copy(fontFeatureSettings = "tnum")

/** "Hoy", "Mañana", "En 3 días". Para valores negativos devuelve "Hoy" (cobro vencido hoy mismo). */
@Composable
fun textoDiasRelativo(dias: Int): String = when {
    dias <= 0 -> stringResource(R.string.day_today)
    dias == 1 -> stringResource(R.string.day_tomorrow)
    else -> pluralStringResource(R.plurals.in_n_days, dias, dias)
}

private fun String.sinPuntoYCapital(): String =
    trimEnd('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

/** Día de la semana abreviado y localizado: "Jue", "Thu", "Jeu". */
fun nombreDiaSemanaCorto(fecha: LocalDate): String =
    fecha.toJavaLocalDate().dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).sinPuntoYCapital()

/** Día de la semana completo y localizado: "Jueves". */
fun nombreDiaSemanaLargo(fecha: LocalDate): String =
    fecha.toJavaLocalDate().dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()).sinPuntoYCapital()

/** Mes abreviado a 3 letras y localizado (1..12): "Oct", "Sep". */
fun nombreMesCorto(mes: Int): String =
    Month.of(mes).getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).sinPuntoYCapital().take(3)

/** Mes completo y localizado (1..12): "Octubre". */
fun nombreMesLargo(mes: Int): String =
    Month.of(mes).getDisplayName(JavaTextStyle.FULL, Locale.getDefault()).sinPuntoYCapital()

/** Fecha corta localizada sin año: "9 oct" / "Oct 9". */
fun formatearFechaCorta(fecha: LocalDate): String {
    val locale = Locale.getDefault()
    val patron = if (locale.language == "en") "MMM d" else "d MMM"
    return fecha.toJavaLocalDate().format(DateTimeFormatter.ofPattern(patron, locale)).replace(".", "")
}

/** Fecha localizada de estilo medio: "9 oct 2026" / "Oct 9, 2026". */
fun formatearFechaMedia(fecha: LocalDate): String =
    fecha.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))

/** Sufijo corto del ciclo de cobro ("mes", "año", "sem."); en el catálogo `price` es el precio de ese ciclo. */
@Composable
fun sufijoPeriodo(ciclo: String): String = when (ciclo) {
    "YEARLY" -> stringResource(R.string.period_year)
    "WEEKLY" -> stringResource(R.string.period_week)
    else -> stringResource(R.string.period_month)
}

/** Importe para celdas estrechas: los importes redondos van sin decimales ("30 €", "450 €"). */
fun formatearImporteCorto(valor: Double, moneda: String): String =
    formatearImporte(valor, moneda, if (valor % 1.0 == 0.0) 0 else 2)

/** "12,99 €/mes" o "450 €/año" para celdas estrechas (ver [formatearImporteCorto]). */
@Composable
fun importeConPeriodo(valor: Double, moneda: String, ciclo: String): String =
    formatearImporteCorto(valor, moneda) + "/" + sufijoPeriodo(ciclo)
