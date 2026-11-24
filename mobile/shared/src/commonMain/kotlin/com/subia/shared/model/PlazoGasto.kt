package com.subia.shared.model

import kotlinx.datetime.LocalDate

/**
 * Plazo de un gasto con fecha de fin (préstamo, financiación): cuántas cuotas quedan, cuánto
 * queda por pagar y, si además tiene fecha de inicio, cuántas cuotas tiene en total.
 * Copia de la lógica del backend (`RenewalDates`): los mismos casos de prueba en los dos lados.
 *
 * @property cuotasRestantes cobros de la serie en [max(hoy, inicio), fin]; el de hoy cuenta como
 *   pendiente, igual que en [proximaRenovacion].
 * @property pendiente cuotas restantes × importe (2 decimales, regla de [Periodicidad]).
 * @property cuotasTotales cobros de la serie en [inicio, fin] (la serie puede ir hacia atrás desde
 *   el ancla); `null` sin fecha de inicio, y entonces no hay barra de progreso.
 */
data class PlazoGasto(
    val inicio: LocalDate?,
    val fin: LocalDate,
    val cuotasRestantes: Int,
    val pendiente: Double,
    val cuotasTotales: Int?
) {
    /** Cuotas ya pagadas (totales − restantes), o `null` sin fecha de inicio. */
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
    val totales = inicio?.let { serieDeCobros(sub, it, fin, kMinimo = Int.MIN_VALUE).size }
    val pendiente = Periodicidad.aCifraVisible(Periodicidad.aDiezmilesimas(sub.precio) * restantes)
    return PlazoGasto(inicio, fin, restantes, pendiente, totales)
}
