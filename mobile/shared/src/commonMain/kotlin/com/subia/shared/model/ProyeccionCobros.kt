package com.subia.shared.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plus

/** Un cobro concreto previsto para una fecha. */
data class CobroPrevisto(
    val suscripcionId: Long,
    val nombre: String,
    val importe: Double,
    val moneda: String,
    val fecha: LocalDate,
    val categoriaId: Long?,
    val periodoFacturacion: String
)

/** Cobros previstos para un día de la tira. */
data class DiaCobros(val fecha: LocalDate, val cobros: List<CobroPrevisto>) {
    val totalPorMoneda: Map<String, Double> get() = sumarPorMoneda(cobros)
    val tieneCobros: Boolean get() = cobros.isNotEmpty()
}

/** Cobros previstos para un mes natural (anio/mes 1..12). */
data class MesCobros(val anio: Int, val mes: Int, val cobros: List<CobroPrevisto>) {
    val totalPorMoneda: Map<String, Double> get() = sumarPorMoneda(cobros)
    fun total(moneda: String): Double = cobros.filter { it.moneda == moneda }.sumOf { it.importe }
}

/** Suma de cobros del mes en curso agrupados por categoría y divisa. */
data class GastoCategoriaMes(
    val categoriaId: Long?,
    val moneda: String,
    val total: Double,
    val numCobros: Int
)

/**
 * Proyección de cobros calculada en el cliente a partir de las suscripciones activas:
 * - [dias]: día a día desde [hoy] (índice 0) durante [DIAS_TIRA] días.
 * - [meses]: mes natural a mes natural desde el mes de [hoy] (índice 0) durante [MESES_HORIZONTE] meses.
 * - [totalMesActual] / [totalMesSiguiente]: suma de los cobros de los meses 0 y 1, por divisa.
 * - [porCategoriaMesActual]: cobros del mes 0 agrupados por categoría y divisa, de mayor a menor.
 */
data class ProyeccionCobros(
    val hoy: LocalDate,
    val dias: List<DiaCobros>,
    val meses: List<MesCobros>,
    val totalMesActual: Map<String, Double>,
    val totalMesSiguiente: Map<String, Double>,
    val porCategoriaMesActual: List<GastoCategoriaMes>
) {
    /** Primer cobro de la tira (hoy incluido), o null si no hay ninguno en 14 días. */
    val proximoCobro: CobroPrevisto? get() = dias.firstOrNull { it.tieneCobros }?.cobros?.firstOrNull()

    /** Todos los cobros de la tira en orden cronológico. */
    val cobrosTira: List<CobroPrevisto> get() = dias.flatMap { it.cobros }

    companion object {
        const val DIAS_TIRA = 14
        const val MESES_HORIZONTE = 12
    }
}

private fun sumarPorMoneda(cobros: List<CobroPrevisto>): Map<String, Double> =
    cobros.groupBy { it.moneda }
        .mapValues { (_, lista) -> lista.sumOf { it.importe } }
        .entries.sortedWith(compareBy { ordenDivisa(it.key) })
        .associate { it.key to it.value }

/** EUR primero, USD segundo, resto alfabético (mismo criterio que el resto del Dashboard). */
internal fun ordenDivisa(moneda: String): String = when (moneda) {
    "EUR" -> "0"
    "USD" -> "1"
    else -> "2$moneda"
}

/** Longitud del ciclo en meses (anual = 12, trimestral = 3, mensual = 1) o en días (semanal = 7). */
private data class Ciclo(val meses: Int = 0, val dias: Int = 0)

private fun cicloDe(periodo: String): Ciclo = when (periodo.trim().uppercase()) {
    "YEARLY", "ANNUAL", "ANNUALLY" -> Ciclo(meses = 12)
    "QUARTERLY" -> Ciclo(meses = 3)
    "WEEKLY" -> Ciclo(dias = 7)
    "BIWEEKLY" -> Ciclo(dias = 14)
    "DAILY" -> Ciclo(dias = 1)
    else -> Ciclo(meses = 1)
}

/**
 * Fecha del cobro número [k] (k = 0 es la fecha ancla). Se calcula SIEMPRE desde el ancla,
 * no acumulando: así un ancla del 31 cae el 30/28 en meses cortos y vuelve al 31 en los largos
 * (kotlinx-datetime recorta el día al último válido del mes).
 */
private fun fechaCobro(ancla: LocalDate, ciclo: Ciclo, k: Int): LocalDate =
    if (ciclo.meses > 0) ancla.plus(k * ciclo.meses, DateTimeUnit.MONTH)
    else ancla.plus(k * ciclo.dias, DateTimeUnit.DAY)

/** Cobros de una suscripción en [hoy, fin], avanzando ciclos si la fecha de renovación es pasada. */
private fun ocurrencias(sub: Subscription, hoy: LocalDate, fin: LocalDate): List<LocalDate> {
    val ancla = runCatching { LocalDate.parse(sub.fechaRenovacion.trim()) }.getOrNull() ?: return emptyList()
    val ciclo = cicloDe(sub.periodoFacturacion)
    // Estimación (por defecto) del primer ciclo >= hoy, para no iterar desde anclas muy antiguas.
    var k = if (ancla >= hoy) 0 else {
        if (ciclo.meses > 0) ancla.monthsUntil(hoy) / ciclo.meses else ancla.daysUntil(hoy) / ciclo.dias
    }.coerceAtLeast(0)
    while (fechaCobro(ancla, ciclo, k) < hoy) k++

    val resultado = mutableListOf<LocalDate>()
    var guardia = 0
    while (guardia++ < 400) {
        val fecha = fechaCobro(ancla, ciclo, k)
        if (fecha > fin) break
        resultado += fecha
        k++
    }
    return resultado
}

/**
 * Calcula la [ProyeccionCobros] de las suscripciones **activas** a partir de [hoy].
 * Las inactivas y las que tienen fecha de renovación inválida se ignoran.
 */
fun calcularProyeccionCobros(subs: List<Subscription>, hoy: LocalDate): ProyeccionCobros {
    val inicioMes = LocalDate(hoy.year, hoy.month, 1)
    val finHorizonte = inicioMes.plus(ProyeccionCobros.MESES_HORIZONTE, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

    val cobros = subs.asSequence()
        .filter { it.activa }
        .flatMap { sub ->
            ocurrencias(sub, hoy, finHorizonte).map { fecha ->
                CobroPrevisto(
                    suscripcionId = sub.id,
                    nombre = sub.nombre,
                    importe = sub.precio,
                    moneda = sub.moneda,
                    fecha = fecha,
                    categoriaId = sub.categoriaId,
                    periodoFacturacion = sub.periodoFacturacion
                )
            }
        }
        .sortedWith(compareBy<CobroPrevisto> { it.fecha }.thenByDescending { it.importe })
        .toList()

    val dias = (0 until ProyeccionCobros.DIAS_TIRA).map { i ->
        val fecha = hoy.plus(i, DateTimeUnit.DAY)
        DiaCobros(fecha, cobros.filter { it.fecha == fecha })
    }
    val meses = (0 until ProyeccionCobros.MESES_HORIZONTE).map { m ->
        val primero = inicioMes.plus(m, DateTimeUnit.MONTH)
        MesCobros(
            anio = primero.year,
            mes = primero.monthNumber,
            cobros = cobros.filter { it.fecha.year == primero.year && it.fecha.monthNumber == primero.monthNumber }
        )
    }
    val porCategoria = meses[0].cobros
        .groupBy { it.categoriaId to it.moneda }
        .map { (clave, lista) ->
            GastoCategoriaMes(clave.first, clave.second, lista.sumOf { it.importe }, lista.size)
        }
        .sortedWith(compareBy<GastoCategoriaMes> { ordenDivisa(it.moneda) }.thenByDescending { it.total })

    return ProyeccionCobros(
        hoy = hoy,
        dias = dias,
        meses = meses,
        totalMesActual = meses[0].totalPorMoneda,
        totalMesSiguiente = meses[1].totalPorMoneda,
        porCategoriaMesActual = porCategoria
    )
}
