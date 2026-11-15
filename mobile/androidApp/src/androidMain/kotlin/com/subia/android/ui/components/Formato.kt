package com.subia.android.ui.components

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.subia.android.R
import com.subia.shared.model.PeriodoCobro
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
fun formatearImporte(
    valor: Double,
    moneda: String = "EUR",
    decimales: Int = 2,
    // Las notificaciones pasan el locale de los recursos de la app (idioma elegido en Ajustes),
    // que en segundo plano puede no coincidir con Locale.getDefault().
    locale: Locale = Locale.getDefault()
): String {
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

/**
 * Mes completo con la mayúscula que le toca dentro de una frase (1..12): "noviembre",
 * "novembre", "novembro", pero "November" en inglés. Para "Cobros de noviembre".
 */
fun nombreMesEnFrase(mes: Int): String =
    Month.of(mes).getDisplayName(JavaTextStyle.FULL, Locale.getDefault())

/** Fecha corta localizada sin año: "9 oct" / "Oct 9". */
fun formatearFechaCorta(fecha: LocalDate): String {
    val locale = Locale.getDefault()
    val patron = if (locale.language == "en") "MMM d" else "d MMM"
    return fecha.toJavaLocalDate().format(DateTimeFormatter.ofPattern(patron, locale)).replace(".", "")
}

/** Fecha localizada de estilo medio: "9 oct 2026" / "Oct 9, 2026". */
fun formatearFechaMedia(fecha: LocalDate): String =
    fecha.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))

/**
 * Sufijo corto del ciclo de cobro ("mes", "año", "sem.", "3 meses"); en el catálogo `price` es
 * el precio de ese ciclo. [intervaloMeses] solo cuenta en los ciclos por meses (ver [PeriodoCobro]).
 */
@Composable
fun sufijoPeriodo(ciclo: String, intervaloMeses: Int = 1): String =
    when (val periodo = PeriodoCobro.de(ciclo, intervaloMeses)) {
        PeriodoCobro.Semanal -> stringResource(R.string.period_week)
        is PeriodoCobro.PorMeses -> when (periodo.meses) {
            1 -> stringResource(R.string.period_month)
            12 -> stringResource(R.string.period_year)
            else -> pluralStringResource(R.plurals.frequency_n_months, periodo.meses, periodo.meses)
        }
        // El catálogo solo trae MONTHLY, YEARLY y WEEKLY; se mantiene el "mes" de siempre.
        PeriodoCobro.Quincenal, PeriodoCobro.Diario -> stringResource(R.string.period_month)
    }

/** Importe para celdas estrechas: los importes redondos van sin decimales ("30 €", "450 €"). */
fun formatearImporteCorto(valor: Double, moneda: String): String =
    formatearImporte(valor, moneda, if (valor % 1.0 == 0.0) 0 else 2)

/** "12,99 €/mes", "450 €/año" o "90 €/3 meses" para celdas estrechas (ver [formatearImporteCorto]). */
@Composable
fun importeConPeriodo(valor: Double, moneda: String, ciclo: String, intervaloMeses: Int = 1): String =
    formatearImporteCorto(valor, moneda) + "/" + sufijoPeriodo(ciclo, intervaloMeses)

/**
 * Texto de un recurso: un string fijo o un plural con su cantidad. Permite que la versión
 * Compose y la de las notificaciones (sin Compose) de [textoPeriodo] compartan el mismo reparto.
 */
private class RecursoTexto(val id: Int, val cantidad: Int? = null)

private fun recursoPeriodo(periodo: PeriodoCobro): RecursoTexto = when (periodo) {
    PeriodoCobro.Semanal -> RecursoTexto(R.string.notif_every_week)
    PeriodoCobro.Quincenal -> RecursoTexto(R.string.notif_every_two_weeks)
    PeriodoCobro.Diario -> RecursoTexto(R.string.notif_every_day)
    is PeriodoCobro.PorMeses -> when (periodo.meses) {
        1 -> RecursoTexto(R.string.notif_every_month)
        12 -> RecursoTexto(R.string.notif_every_year)
        // 2, 3 y 6: "cada 3 meses". Plural porque en algunos idiomas cambia con el número.
        else -> RecursoTexto(R.plurals.notif_every_n_months, periodo.meses)
    }
}

/** Periodo en una frase, tras un importe: "al mes", "cada 3 meses", "al año", "a la semana". */
@Composable
fun textoPeriodo(periodo: PeriodoCobro): String {
    val recurso = recursoPeriodo(periodo)
    val cantidad = recurso.cantidad
    return if (cantidad == null) stringResource(recurso.id) else pluralStringResource(recurso.id, cantidad, cantidad)
}

/**
 * Igual que la versión Compose, para textos fuera de pantalla (notificaciones). Recibe los
 * [Resources] del idioma de la app, que en segundo plano puede no ser el del sistema.
 */
fun textoPeriodo(resources: Resources, periodo: PeriodoCobro): String {
    val recurso = recursoPeriodo(periodo)
    val cantidad = recurso.cantidad
    return if (cantidad == null) resources.getString(recurso.id) else resources.getQuantityString(recurso.id, cantidad, cantidad)
}

/** "90,00 € cada 3 meses", "9,99 € al mes", "5,00 € a la semana". */
@Composable
fun importeCadaPeriodo(valor: Double, moneda: String, periodo: PeriodoCobro): String =
    formatearImporte(valor, moneda) + " " + textoPeriodo(periodo)
