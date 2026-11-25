package com.subia.shared.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Plazo de un gasto con fecha de fin (préstamo, financiación): cuántas cuotas quedan, cuánto
 * queda por pagar y, si además tiene fecha de inicio, cuántas cuotas tiene en total.
 * Copia de la lógica del backend (`RenewalDates`): los mismos casos de prueba en los dos lados.
 *
 * @property cuotasRestantes cobros de la serie en [próximo cobro, fin]; con fecha de inicio la
 *   serie va anclada en el inicio y el próximo cobro es el primero >= máx(hoy, inicio, renovación
 *   guardada). El de hoy cuenta como pendiente, igual que en [proximaRenovacion].
 * @property pendiente cuotas restantes × importe (2 decimales, regla de [Periodicidad]).
 * @property cuotasTotales pagadas + restantes; pagadas son los cobros de la serie en
 *   [inicio, mín(fin, ayer)], así que con la renovación guardada en el futuro los cobros entre
 *   hoy y ella no cuentan. `null` sin fecha de inicio, y entonces no hay barra de progreso.
 * @property total cuotas totales × importe (2 decimales), `null` sin fecha de inicio.
 */
data class PlazoGasto(
    val inicio: LocalDate?,
    val fin: LocalDate,
    val cuotasRestantes: Int,
    val pendiente: Double,
    val cuotasTotales: Int?,
    val total: Double? = null
) {
    /** Cuotas ya pagadas (cobros anteriores a hoy), o `null` sin fecha de inicio. */
    val cuotasPagadas: Int? get() = cuotasTotales?.let { (it - cuotasRestantes).coerceAtLeast(0) }

    /** Progreso 0..1 (pagadas / totales) para la barra, o `null` sin fecha de inicio o sin cuotas. */
    val progreso: Float?
        get() {
            val totales = cuotasTotales?.takeIf { it > 0 } ?: return null
            return (cuotasPagadas ?: 0).toFloat() / totales
        }
}

/** Fecha de inicio del plazo, o `null` si no tiene o no es una fecha ISO válida. */
fun Subscription.fechaInicioPlazo(): LocalDate? = fechaPlazo(fechaInicio)

/** Fecha de fin del plazo, o `null` si no tiene o no es una fecha ISO válida. */
fun Subscription.fechaFinPlazo(): LocalDate? = fechaPlazo(fechaFin)

private fun fechaPlazo(texto: String?): LocalDate? =
    texto?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/**
 * `true` si el gasto ya no tiene cobros pendientes: tiene fecha de fin y su próximo cobro
 * ([proximaRenovacion], que respeta el inicio) cae después. Cubre fin < hoy, "fin el 15 con cobros
 * el día 5" después del último cobro y un plazo sin ningún cobro dentro. Se deriva, no se guarda
 * (como la prueba vencida). Misma regla que `RenewalDates.isTerminada` del backend.
 */
fun Subscription.terminado(hoy: LocalDate): Boolean {
    val fin = fechaFinPlazo() ?: return false
    val proximo = proximaRenovacion(this, hoy) ?: return false
    return proximo > fin
}

/** Si el gasto suma en los totales: activo y no terminado. */
fun Subscription.cuentaEnTotales(hoy: LocalDate): Boolean = activa && !terminado(hoy)

/** Plazo del gasto a fecha [hoy], o `null` si no tiene fecha de fin (no hay bloque de plazo). */
fun plazoGasto(sub: Subscription, hoy: LocalDate): PlazoGasto? {
    val fin = sub.fechaFinPlazo() ?: return null
    val inicio = sub.fechaInicioPlazo()
    val restantes = ocurrencias(sub, hoy, fin).size
    val ayer = hoy.minus(1, DateTimeUnit.DAY)
    val pagadas = inicio?.let { serieDeCobros(sub, it, minOf(fin, ayer)).size }
    val totales = pagadas?.let { it + restantes }
    val cuota = Periodicidad.aDiezmilesimas(sub.precio)
    return PlazoGasto(
        inicio = inicio,
        fin = fin,
        cuotasRestantes = restantes,
        pendiente = Periodicidad.aCifraVisible(cuota * restantes),
        cuotasTotales = totales,
        total = totales?.let { Periodicidad.aCifraVisible(cuota * it) }
    )
}

/**
 * Resultado de la calculadora de préstamo del formulario: [cuotas] cobros de [cuota]
 * (diezmilésimas, redondeada a céntimos). [exacta] es `false` si cuota × cuotas no da el total,
 * y entonces la última cuota puede variar unos céntimos.
 */
data class CuotaPrestamo(val cuotas: Int, val cuota: Long, val exacta: Boolean)

/**
 * Cuota de un préstamo a partir de su importe [total] (intereses incluidos) y las fechas de la
 * primera ([inicio]) y la última ([fin]) cuota: total / nº de cobros en [inicio, fin], ambos
 * incluidos, con la periodicidad del gasto. Cuenta con la misma serie que las cuotas totales de
 * [plazoGasto], anclada en [inicio]. `null` si total <= 0, falta una fecha, fin < inicio o no
 * cae ningún cobro. El total no se guarda: solo rellena el importe por cuota.
 */
fun calcularCuotaPrestamo(
    total: Double,
    periodoFacturacion: String,
    intervaloMeses: Int,
    inicio: LocalDate?,
    fin: LocalDate?
): CuotaPrestamo? {
    if (inicio == null || fin == null || fin < inicio || !(total > 0) || total > Periodicidad.IMPORTE_MAXIMO) return null
    val n = serieDeCobros(serieDesde(inicio, periodoFacturacion, intervaloMeses), inicio, fin).size
    if (n == 0) return null
    val totalD = Periodicidad.aDiezmilesimas(total)
    // Céntimos HALF_UP (1 céntimo = 100 diezmilésimas) y de vuelta a diezmilésimas.
    val cuota = Periodicidad.dividirHalfUp(totalD, n * 100L) * 100
    return CuotaPrestamo(cuotas = n, cuota = cuota, exacta = cuota * n == totalD)
}

/**
 * Próxima renovación para un préstamo de la calculadora: el primer cobro de la serie anclada en
 * [inicio] que cae en [máx(hoy, inicio), fin]; si ya no queda ninguno (préstamo terminado), [fin].
 * Con esta fecha como ancla, las cuotas totales de [plazoGasto] coinciden con las de
 * [calcularCuotaPrestamo] (con renovación = hoy, la barra contaba otra serie: "9 de 11").
 */
fun proximaRenovacionPrestamo(
    periodoFacturacion: String,
    intervaloMeses: Int,
    inicio: LocalDate,
    fin: LocalDate,
    hoy: LocalDate
): LocalDate =
    serieDeCobros(serieDesde(inicio, periodoFacturacion, intervaloMeses), maxOf(inicio, hoy), fin).firstOrNull() ?: fin

/** Gasto mínimo cuya serie de cobros parte de [inicio], para contar con [serieDeCobros]. */
private fun serieDesde(inicio: LocalDate, periodoFacturacion: String, intervaloMeses: Int) = Subscription(
    nombre = "", precio = 0.0, periodoFacturacion = periodoFacturacion,
    fechaRenovacion = inicio.toString(), intervaloMeses = intervaloMeses
)
