package com.subia.shared.model

/**
 * Cada cuánto se cobra un gasto, ya interpretado, para que la UI elija el texto
 * ("al mes", "cada 3 meses", "al año", "a la semana") sin repetir en cada pantalla las reglas
 * de [Periodicidad.mesesPorCobro] (YEARLY antiguo con intervalo 1, QUARTERLY, datos corruptos...).
 *
 * Solo describe el periodo: los importes se calculan con [Periodicidad].
 */
sealed interface PeriodoCobro {
    /** Cada semana (WEEKLY). */
    data object Semanal : PeriodoCobro

    /** Cada dos semanas (BIWEEKLY, ciclo antiguo que la app ya no ofrece al crear). */
    data object Quincenal : PeriodoCobro

    /** Cada día (DAILY, ciclo antiguo que la app ya no ofrece al crear). */
    data object Diario : PeriodoCobro

    /** Cada [meses] meses: 1 es mensual, 12 anual y 2, 3 o 6 los intermedios. */
    data class PorMeses(val meses: Int) : PeriodoCobro

    /** `true` si se cobra una vez al mes: su importe ya es el equivalente mensual. */
    val esMensual: Boolean get() = this == PorMeses(1)

    companion object {
        /**
         * Periodo de un gasto a partir de su ciclo e intervalo, con la misma interpretación que
         * los cálculos de dinero ([Periodicidad.mesesPorCobro]): así el texto "cada 3 meses"
         * y el equivalente mensual de 30 € nunca se contradicen.
         */
        fun de(periodoFacturacion: String, intervaloMeses: Int): PeriodoCobro {
            Periodicidad.mesesPorCobro(periodoFacturacion, intervaloMeses)?.let { return PorMeses(it) }
            // mesesPorCobro solo devuelve null para estos tres ciclos.
            return when (periodoFacturacion.trim().uppercase()) {
                "WEEKLY" -> Semanal
                "BIWEEKLY" -> Quincenal
                else -> Diario
            }
        }
    }
}

/** Periodo de cobro de esta suscripción (ver [PeriodoCobro.de]). */
fun Subscription.periodoCobro(): PeriodoCobro = PeriodoCobro.de(periodoFacturacion, intervaloMeses)
