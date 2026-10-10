package com.subia.service

import com.subia.model.BillingCycle
import com.subia.model.Subscription
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Cálculo de la próxima renovación real de una suscripción (N-02 / N-12).
 *
 * El backend guarda `renewalDate` tal como se dio de alta y no la avanza nunca, así que una
 * mensual dada de alta en mayo tenía en octubre una "fecha de renovación" de mayo y desaparecía
 * de las próximas renovaciones. Aquí se proyecta desde esa fecha (el ancla) el primer cobro que
 * cae hoy o después.
 *
 * Siempre se calcula desde el ancla (`ancla + n ciclos`), no encadenando sumas, para que un día
 * 31 caiga el 30/28 en los meses cortos y vuelva al 31 después (misma regla que la app Android,
 * `ProyeccionCobros`).
 */
object RenewalDates {

    /** Primer cobro en [today] o después, partiendo del ancla [anchor] y el ciclo [cycle]. */
    fun nextRenewal(anchor: LocalDate, cycle: BillingCycle, today: LocalDate): LocalDate {
        if (!anchor.isBefore(today)) return anchor
        // Estimación directa del número de ciclos transcurridos para no iterar años de semanas.
        var n = when (cycle) {
            BillingCycle.WEEKLY -> ChronoUnit.WEEKS.between(anchor, today)
            BillingCycle.MONTHLY -> ChronoUnit.MONTHS.between(anchor, today)
            BillingCycle.YEARLY -> ChronoUnit.YEARS.between(anchor, today)
        }
        var candidate = plusCycles(anchor, cycle, n)
        while (candidate.isBefore(today)) {
            n++
            candidate = plusCycles(anchor, cycle, n)
        }
        return candidate
    }

    /** Próxima renovación de una suscripción a partir de su `renewalDate` guardada. */
    fun nextRenewal(sub: Subscription, today: LocalDate): LocalDate =
        nextRenewal(sub.renewalDate, sub.billingCycle, today)

    /**
     * Fecha que hay que GUARDAR al editar una suscripción (ronda 3, R3-01).
     *
     * La API y la web muestran la próxima renovación proyectada, no el ancla guardada. Una
     * mensual del día 31 vista en noviembre llega como `2026-11-30`; si el cliente la devuelve
     * tal cual en el PUT (sin que el usuario toque la fecha), guardarla movía el ancla al 30 para
     * siempre (y un anual del 29-feb al 28-feb).
     *
     * Regla: si el ciclo no cambia y la fecha recibida es exactamente la que el servidor habría
     * proyectado desde el ancla guardada (hoy, o ayer para cubrir el paso de medianoche y un
     * cliente cuya zona horaria va por detrás), es la misma serie de cobros → se conserva el
     * ancla. Cualquier otra fecha es un cambio deliberado del usuario y se respeta. La única
     * ambigüedad (elegir a propósito justo la fecha proyectada) no cambia los cobros futuros.
     *
     * No se tolera "mañana": el día de un cobro, mover la fecha al ciclo siguiente (saltarse un
     * cobro) es un cambio deliberado que debe respetarse.
     */
    fun anchorForUpdate(
        storedAnchor: LocalDate,
        storedCycle: BillingCycle,
        receivedDate: LocalDate,
        receivedCycle: BillingCycle,
        today: LocalDate
    ): LocalDate {
        if (receivedCycle != storedCycle || receivedDate == storedAnchor) return receivedDate
        val sameSeries = receivedDate == nextRenewal(storedAnchor, storedCycle, today) ||
            receivedDate == nextRenewal(storedAnchor, storedCycle, today.minusDays(1))
        return if (sameSeries) storedAnchor else receivedDate
    }

    /** [anchorForUpdate] a partir de la suscripción guardada. */
    fun anchorForUpdate(stored: Subscription, receivedDate: LocalDate, receivedCycle: BillingCycle, today: LocalDate): LocalDate =
        anchorForUpdate(stored.renewalDate, stored.billingCycle, receivedDate, receivedCycle, today)

    /**
     * true si la suscripción está en una prueba gratuita que sigue vigente hoy. Una prueba
     * con fecha de fin ya pasada se trata como de pago (N-10): nada cambia `isTrial` al vencer
     * y, si no, quedaba fuera de los totales para siempre.
     */
    fun isActiveTrial(sub: Subscription, today: LocalDate): Boolean {
        if (!sub.isTrial) return false
        val ends = sub.trialEndsAt ?: return true
        return !ends.isBefore(today)
    }

    private fun plusCycles(anchor: LocalDate, cycle: BillingCycle, n: Long): LocalDate = when (cycle) {
        BillingCycle.WEEKLY -> anchor.plusWeeks(n)
        BillingCycle.MONTHLY -> anchor.plusMonths(n)
        BillingCycle.YEARLY -> anchor.plusYears(n)
    }
}
