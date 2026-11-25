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
    val periodoFacturacion: String,
    /** Intervalo en meses del gasto (ver [Periodicidad]); con [periodoFacturacion] da [periodo]. */
    val intervaloMeses: Int = 1
) {
    /** Cada cuánto se cobra, para textos como "12,99 € al mes" o "90 € cada 3 meses". */
    val periodo: PeriodoCobro get() = PeriodoCobro.de(periodoFacturacion, intervaloMeses)
}

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
 *   OJO: el mes 0 solo cuenta desde [hoy] (lo que QUEDA por pagar este mes).
 * - [totalMesCompleto]: "Este mes pagas", lo que vence en el mes natural ENTERO (del día 1 al
 *   último, incluidos los cobros ya pasados), por divisa y con la regla de redondeo de [Periodicidad].
 * - [porCategoriaMesActual]: cobros del mes 0 agrupados por categoría y divisa, de mayor a menor.
 * - [diasMes]: día a día del mes natural de [hoy] COMPLETO (del 1 al último), también los días ya
 *   pasados con lo que se cobró. Es la vista "Este mes" de la tira; sumada con [totalDeDias] da
 *   exactamente [totalMesCompleto] (misma serie de fechas y misma regla de redondeo).
 */
data class ProyeccionCobros(
    val hoy: LocalDate,
    val dias: List<DiaCobros>,
    val meses: List<MesCobros>,
    val totalMesActual: Map<String, Double>,
    val totalMesSiguiente: Map<String, Double>,
    val porCategoriaMesActual: List<GastoCategoriaMes>,
    val totalMesCompleto: Map<String, Double> = emptyMap(),
    val diasMes: List<DiaCobros> = emptyList()
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

/**
 * Ciclo de cobro de [sub]: cada [Periodicidad.mesesPorCobro] meses (intervalo del gasto, con la
 * interpretación robusta de YEARLY sin campo) o, si no va por meses, cada 7/14/1 días.
 */
private fun cicloDe(sub: Subscription): Ciclo {
    sub.mesesPorCobro()?.let { return Ciclo(meses = it) }
    return when (sub.periodoFacturacion.trim().uppercase()) {
        "BIWEEKLY" -> Ciclo(dias = 14)
        "DAILY" -> Ciclo(dias = 1)
        else -> Ciclo(dias = 7) // WEEKLY
    }
}

/**
 * Fecha del cobro número [k] (k = 0 es la fecha ancla). Se calcula SIEMPRE desde el ancla,
 * no acumulando: así un ancla del 31 cae el 30/28 en meses cortos y vuelve al 31 en los largos
 * (kotlinx-datetime recorta el día al último válido del mes).
 */
private fun fechaCobro(ancla: LocalDate, ciclo: Ciclo, k: Int): LocalDate =
    if (ciclo.meses > 0) ancla.plus(k * ciclo.meses, DateTimeUnit.MONTH)
    else ancla.plus(k * ciclo.dias, DateTimeUnit.DAY)

/**
 * Cobros de una suscripción en [hoy, fin], avanzando ciclos si la fecha de renovación es pasada.
 * Es el embudo de toda la proyección (calendario, "Este mes pagas", avisos, widget): aquí se
 * recorta la serie al plazo del gasto, sin cobros antes de `fechaInicio` (ni de la renovación
 * guardada, ver [cotaInferior]) ni después de `fechaFin`. Sin fechas de plazo, el de siempre.
 */
internal fun ocurrencias(sub: Subscription, hoy: LocalDate, fin: LocalDate): List<LocalDate> {
    val finPlazo = sub.fechaFinPlazo()
    val hasta = if (finPlazo != null && finPlazo < fin) finPlazo else fin
    return serieDeCobros(sub, cotaInferior(sub, hoy), hasta)
}

/**
 * Primer día en que puede caer un cobro visto desde [hoy]. Sin fecha de inicio, [hoy]. Con ella,
 * máx(hoy, inicio, renovación guardada): la serie va anclada en el inicio ([anclaDe]) y la
 * renovación guardada solo hace de cota inferior (misma regla que `RenewalDates` del backend).
 */
private fun cotaInferior(sub: Subscription, hoy: LocalDate): LocalDate {
    val inicio = sub.fechaInicioPlazo() ?: return hoy
    return listOfNotNull(hoy, inicio, renovacionGuardada(sub)).max()
}

/**
 * Fechas de la serie de cobros de [sub] en [desde, hasta], SIN recortar al plazo. La serie es
 * `ancla + k ciclos` con k >= 0 (no hay cobros antes del ancla, ver [anclaDe]).
 */
internal fun serieDeCobros(
    sub: Subscription,
    desde: LocalDate,
    hasta: LocalDate
): List<LocalDate> {
    if (desde > hasta) return emptyList()
    val ancla = anclaDe(sub) ?: return emptyList()
    val ciclo = cicloDe(sub)
    // Estimación del primer ciclo >= desde, para no iterar desde anclas muy antiguas; los dos
    // bucles la corrigen (la división trunca hacia cero y los meses cortos recortan el día).
    var k = (if (ciclo.meses > 0) ancla.monthsUntil(desde) / ciclo.meses else ancla.daysUntil(desde) / ciclo.dias)
        .coerceAtLeast(0)
    while (k > 0 && fechaCobro(ancla, ciclo, k - 1) >= desde) k--
    while (fechaCobro(ancla, ciclo, k) < desde) k++

    val resultado = mutableListOf<LocalDate>()
    // shortcut: tope de iteraciones; si hiciera falta, cálculo en forma cerrada
    // (10 000 cobros cubren un semanal en todo el rango que admite el backend, 2000-2100).
    var guardia = 0
    while (guardia++ < MAX_COBROS_SERIE) {
        val fecha = fechaCobro(ancla, ciclo, k)
        if (fecha > hasta) break
        resultado += fecha
        k++
    }
    return resultado
}

private const val MAX_COBROS_SERIE = 10_000

/**
 * Próxima fecha de cobro (>= [hoy]) de [sub]. El backend guarda la fecha de renovación tal y como
 * se dio de alta y no siempre la avanza, así que la lista y el detalle mostraban fechas pasadas
 * ("Renueva: 3 may" en octubre). Devuelve null si la fecha no es una ISO válida.
 * Respeta el inicio del plazo (no hay cobros antes de `fechaInicio`) pero NO la fecha de fin: si
 * cae después de ella, el gasto está [terminado] (como `RenewalDates.proximoCobro` del backend).
 */
fun proximaRenovacion(sub: Subscription, hoy: LocalDate): LocalDate? {
    val ancla = anclaDe(sub) ?: return null
    val desde = cotaInferior(sub, hoy)
    if (ancla >= desde) return ancla
    return serieDeCobros(sub, desde, desde.plus(2, DateTimeUnit.YEAR)).firstOrNull() ?: ancla
}

/**
 * Ancla desde la que se proyecta. Con fecha de inicio, SIEMPRE el inicio: así un préstamo que
 * empieza el 31 cobra el 31 (o el último día del mes) y no el 30 de una renovación guardada en
 * noviembre. Sin ella, la renovación guardada ([renovacionGuardada]).
 */
private fun anclaDe(sub: Subscription): LocalDate? = sub.fechaInicioPlazo() ?: renovacionGuardada(sub)

/**
 * Renovación guardada en el servidor (`renewalAnchor`) si llega, para que el día 31 / 29-feb no
 * se recorte y "hoy" sea el del MÓVIL, no el del servidor (R3-02); si no, `renewalDate`
 * (backends anteriores). Si el ancla no parsea se usa `renewalDate`.
 */
private fun renovacionGuardada(sub: Subscription): LocalDate? =
    runCatching { LocalDate.parse(sub.anclaRenovacion.trim()) }.getOrNull()
        ?: runCatching { LocalDate.parse(sub.fechaRenovacion.trim()) }.getOrNull()

/**
 * Calcula la [ProyeccionCobros] de las suscripciones **activas** a partir de [hoy].
 * Las inactivas y las que tienen fecha de renovación inválida se ignoran.
 */
fun calcularProyeccionCobros(subs: List<Subscription>, hoy: LocalDate): ProyeccionCobros {
    val inicioMes = LocalDate(hoy.year, hoy.month, 1)
    val finHorizonte = inicioMes.plus(ProyeccionCobros.MESES_HORIZONTE, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

    val cobros = cobrosPrevistos(subs, hoy, finHorizonte)

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
        porCategoriaMesActual = porCategoria,
        totalMesCompleto = totalMesCompleto(subs, inicioMes),
        diasMes = cobrosDelMes(subs, hoy)
    )
}

/**
 * Cobros de las suscripciones activas en [desde, hasta], en orden cronológico y, el mismo día,
 * de mayor a menor importe.
 */
private fun cobrosPrevistos(subs: List<Subscription>, desde: LocalDate, hasta: LocalDate): List<CobroPrevisto> =
    subs.asSequence()
        .filter { it.activa } // un gasto terminado no tiene cobros fuera de su plazo: lo recorta ocurrencias
        .flatMap { sub ->
            ocurrencias(sub, desde, hasta).map { fecha ->
                CobroPrevisto(
                    suscripcionId = sub.id,
                    nombre = sub.nombre,
                    importe = sub.precio,
                    moneda = sub.moneda,
                    fecha = fecha,
                    categoriaId = sub.categoriaId,
                    periodoFacturacion = sub.periodoFacturacion,
                    intervaloMeses = sub.intervaloMeses
                )
            }
        }
        .sortedWith(compareBy<CobroPrevisto> { it.fecha }.thenByDescending { it.importe })
        .toList()

/**
 * Días del mes natural de [hoy], del 1 al último, con los cobros de cada uno (también los de los
 * días ya pasados). Usa la misma serie que [totalMesCompleto] ([ocurrencias] desde el día 1),
 * así que la vista mensual de la tira y "Este mes pagas" no pueden contradecirse.
 */
fun cobrosDelMes(subs: List<Subscription>, hoy: LocalDate): List<DiaCobros> {
    val inicioMes = LocalDate(hoy.year, hoy.month, 1)
    val finMes = inicioMes.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    val porFecha = cobrosPrevistos(subs, inicioMes, finMes).groupBy { it.fecha }
    return (0 until inicioMes.daysUntil(finMes) + 1).map { i ->
        val fecha = inicioMes.plus(i, DateTimeUnit.DAY)
        DiaCobros(fecha, porFecha[fecha].orEmpty())
    }
}

/**
 * Total por divisa de los cobros de [dias], sumado en diezmilésimas y redondeado al final con
 * [Periodicidad] (la misma regla que [totalMesCompleto]). Orden EUR, USD, resto alfabético.
 */
fun totalDeDias(dias: List<DiaCobros>): Map<String, Double> {
    val internos = mutableMapOf<String, Long>()
    for (cobro in dias.asSequence().flatMap { it.cobros }) {
        internos[cobro.moneda] = (internos[cobro.moneda] ?: 0L) + Periodicidad.aDiezmilesimas(cobro.importe)
    }
    return internos.entries
        .sortedWith(compareBy { ordenDivisa(it.key) })
        .associate { it.key to Periodicidad.aCifraVisible(it.value) }
}

/**
 * "Este mes pagas": suma, por divisa, de los cobros de las suscripciones activas que vencen
 * en el mes natural que empieza en [inicioMes], del día 1 al último (también los ya pasados).
 * Un trimestral cuenta solo en los meses en que le toca cobrar. Se suma en diezmilésimas y se
 * redondea al final ([Periodicidad]) para no arrastrar errores de coma flotante.
 */
private fun totalMesCompleto(subs: List<Subscription>, inicioMes: LocalDate): Map<String, Double> {
    val finMes = inicioMes.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    val internos = mutableMapOf<String, Long>()
    for (sub in subs) {
        if (!sub.activa) continue
        val cobros = ocurrencias(sub, inicioMes, finMes).size
        if (cobros == 0) continue
        internos[sub.moneda] = (internos[sub.moneda] ?: 0L) + Periodicidad.aDiezmilesimas(sub.precio) * cobros
    }
    return internos.entries
        .sortedWith(compareBy { ordenDivisa(it.key) })
        .associate { it.key to Periodicidad.aCifraVisible(it.value) }
}
