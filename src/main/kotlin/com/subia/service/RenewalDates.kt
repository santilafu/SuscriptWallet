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

    /**
     * Primer cobro en [today] o después, partiendo del ancla [anchor] y el ciclo [cycle].
     *
     * Para MONTHLY/YEARLY cada ciclo dura [intervaloMeses] meses (normalizado con
     * [Periodicidad.mesesPorCobro]: YEARLY con el valor por defecto 1 cuenta como 12, así que
     * las llamadas que no pasan el intervalo siguen igual que antes). WEEKLY lo ignora.
     */
    fun nextRenewal(anchor: LocalDate, cycle: BillingCycle, today: LocalDate, intervaloMeses: Int = 1): LocalDate {
        if (!anchor.isBefore(today)) return anchor
        val meses = if (cycle == BillingCycle.WEEKLY) 0 else Periodicidad.mesesPorCobro(cycle, intervaloMeses)
        // Estimación directa del número de ciclos transcurridos para no iterar años de semanas.
        var n = when (cycle) {
            BillingCycle.WEEKLY -> ChronoUnit.WEEKS.between(anchor, today)
            else -> ChronoUnit.MONTHS.between(anchor, today) / meses
        }
        var candidate = plusCycles(anchor, cycle, meses, n)
        while (candidate.isBefore(today)) {
            n++
            candidate = plusCycles(anchor, cycle, meses, n)
        }
        return candidate
    }

    /** Próxima renovación de una suscripción a partir de su `renewalDate` guardada. */
    fun nextRenewal(sub: Subscription, today: LocalDate): LocalDate =
        nextRenewal(sub.renewalDate, sub.billingCycle, today, sub.intervaloMeses)

    /**
     * Fecha que hay que GUARDAR al editar una suscripción (ronda 3, R3-01).
     *
     * La API y la web muestran la próxima renovación proyectada, no el ancla guardada. Una
     * mensual del día 31 vista en noviembre llega como `2026-11-30`; si el cliente la devuelve
     * tal cual en el PUT (sin que el usuario toque la fecha), guardarla movía el ancla al 30 para
     * siempre (y un anual del 29-feb al 28-feb).
     *
     * Regla: si la periodicidad (ciclo e intervalo de meses) no cambia y la fecha recibida es
     * exactamente la que el servidor habría proyectado desde el ancla guardada (hoy, o ayer para
     * cubrir el paso de medianoche y un cliente cuya zona horaria va por detrás), es la misma
     * serie de cobros → se conserva el ancla. Cualquier otra fecha es un cambio deliberado del
     * usuario y se respeta. La única ambigüedad (elegir a propósito justo la fecha proyectada) no
     * cambia los cobros futuros.
     *
     * Si cambia el ciclo O el intervalo (p. ej. mensual → trimestral) la serie de cobros es otra
     * y la fecha recibida se toma como nueva ancla.
     *
     * No se tolera "mañana": el día de un cobro, mover la fecha al ciclo siguiente (saltarse un
     * cobro) es un cambio deliberado que debe respetarse.
     *
     * Los intervalos van al final con valor por defecto 1 para que las llamadas antiguas sigan
     * igual (YEARLY + 1 se normaliza a 12, ver [Periodicidad.normalizar]).
     */
    fun anchorForUpdate(
        storedAnchor: LocalDate,
        storedCycle: BillingCycle,
        receivedDate: LocalDate,
        receivedCycle: BillingCycle,
        today: LocalDate,
        storedIntervalo: Int = 1,
        receivedIntervalo: Int = 1
    ): LocalDate {
        // Se comparan las parejas normalizadas: YEARLY+1 y YEARLY+12 son la misma periodicidad.
        val samePeriodicity = Periodicidad.normalizar(storedCycle, storedIntervalo) ==
            Periodicidad.normalizar(receivedCycle, receivedIntervalo)
        if (!samePeriodicity || receivedDate == storedAnchor) return receivedDate
        val sameSeries = receivedDate == nextRenewal(storedAnchor, storedCycle, today, storedIntervalo) ||
            receivedDate == nextRenewal(storedAnchor, storedCycle, today.minusDays(1), storedIntervalo)
        return if (sameSeries) storedAnchor else receivedDate
    }

    /**
     * [anchorForUpdate] a partir de la suscripción guardada. [receivedCycle] y
     * [receivedIntervalo] deben ser los que se van a guardar (ya resueltos con
     * [Periodicidad.resolver]).
     */
    fun anchorForUpdate(
        stored: Subscription,
        receivedDate: LocalDate,
        receivedCycle: BillingCycle,
        receivedIntervalo: Int,
        today: LocalDate
    ): LocalDate = anchorForUpdate(
        stored.renewalDate, stored.billingCycle, receivedDate, receivedCycle, today,
        storedIntervalo = stored.intervaloMeses, receivedIntervalo = receivedIntervalo
    )

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

    // ── Plazo (préstamos y financiaciones, V17) ──────────────────────────────────────────
    // Misma tabla de casos que `PlazoGasto` en shared: si cambias una regla, cambia las dos.

    /**
     * Número de cobros de la serie (ancla ± n ciclos, n también negativo) que caen en
     * [desde, hasta], ambos incluidos. La serie es estrictamente creciente, así que basta con
     * colocarse en el primer cobro ≥ [desde] y contar.
     */
    fun cobrosEntre(anchor: LocalDate, cycle: BillingCycle, intervaloMeses: Int, desde: LocalDate, hasta: LocalDate): Int {
        if (hasta.isBefore(desde)) return 0
        val meses = if (cycle == BillingCycle.WEEKLY) 0 else Periodicidad.mesesPorCobro(cycle, intervaloMeses)
        var n = when (cycle) {
            BillingCycle.WEEKLY -> ChronoUnit.WEEKS.between(anchor, desde)
            else -> ChronoUnit.MONTHS.between(anchor, desde) / meses
        }
        while (!plusCycles(anchor, cycle, meses, n).isBefore(desde)) n--
        while (plusCycles(anchor, cycle, meses, n).isBefore(desde)) n++
        var count = 0
        while (!plusCycles(anchor, cycle, meses, n).isAfter(hasta)) { count++; n++ }
        return count
    }

    private fun cobrosEntre(sub: Subscription, desde: LocalDate, hasta: LocalDate): Int =
        cobrosEntre(sub.renewalDate, sub.billingCycle, sub.intervaloMeses, desde, hasta)

    /** Primer día en que puede haber un cobro: hoy, o el inicio del plazo si aún no ha llegado. */
    private fun desde(sub: Subscription, today: LocalDate): LocalDate =
        sub.fechaInicio?.takeIf { it.isAfter(today) } ?: today

    /** Próximo cobro respetando el inicio del plazo (no hay cobros antes de `fechaInicio`). */
    fun proximoCobro(sub: Subscription, today: LocalDate): LocalDate = nextRenewal(sub, desde(sub, today))

    /**
     * true si tiene fecha de fin y ya no le queda ningún cobro hasta ella (D1): cubre el fin
     * pasado y el fin anterior al próximo cobro. Se deriva siempre, nunca se guarda.
     */
    fun isTerminada(sub: Subscription, today: LocalDate): Boolean {
        val fin = sub.fechaFin ?: return false
        return proximoCobro(sub, today).isAfter(fin)
    }

    /** Cobros que quedan en [max(hoy, inicio), fin]; el de hoy cuenta. null sin fecha de fin. */
    fun cuotasRestantes(sub: Subscription, today: LocalDate): Int? =
        sub.fechaFin?.let { cobrosEntre(sub, desde(sub, today), it) }

    /** Cobros de todo el plazo [inicio, fin] (la serie va hacia atrás desde el ancla). null sin las dos fechas. */
    fun cuotasTotales(sub: Subscription): Int? {
        val inicio = sub.fechaInicio ?: return null
        val fin = sub.fechaFin ?: return null
        return cobrosEntre(sub, inicio, fin)
    }

    /**
     * Cuotas que suma la vista anual de un gasto con fecha de fin: mín(restantes, cuotas por año)
     * (decisión P3 del usuario). null sin fecha de fin (la vista anual sigue siendo la tasa).
     */
    fun cuotasProximoAnio(sub: Subscription, today: LocalDate): Int? {
        val restantes = cuotasRestantes(sub, today) ?: return null
        val porAnio = if (sub.billingCycle == BillingCycle.WEEKLY) Periodicidad.SEMANAS_EN_ANIO
            else Periodicidad.MESES_EN_ANIO / Periodicidad.mesesPorCobro(sub.billingCycle, sub.intervaloMeses)
        return minOf(restantes, porAnio)
    }

    /** Progreso de un gasto con fecha de fin, listo para mostrar. null sin fecha de fin. */
    fun plazo(sub: Subscription, today: LocalDate): Plazo? {
        val restantes = cuotasRestantes(sub, today) ?: return null
        return Plazo(
            cuotasRestantes = restantes,
            cuotasTotales = cuotasTotales(sub),
            pendiente = Periodicidad.aCifraVisible(sub.price.multiply(java.math.BigDecimal(restantes))),
            terminado = isTerminada(sub, today)
        )
    }

    /**
     * Cuotas y pendiente de un gasto con fecha de fin. [cuotasTotales] (y con él la barra) solo
     * existe si hay fecha de inicio.
     */
    data class Plazo(
        val cuotasRestantes: Int,
        val cuotasTotales: Int?,
        val pendiente: java.math.BigDecimal,
        val terminado: Boolean
    ) {
        val cuotasPagadas: Int? get() = cuotasTotales?.let { (it - cuotasRestantes).coerceAtLeast(0) }
        /** 0-100, redondeado hacia abajo. */
        val progresoPct: Int? get() = cuotasTotales?.takeIf { it > 0 }?.let { cuotasPagadas!! * 100 / it }
    }

    /**
     * Fecha del cobro número [n] contando desde el ancla. Siempre `ancla + n × meses` en una
     * sola suma (nunca sobre la fecha ya recortada), para que 31-ene → 28/29-feb → 31-mar.
     * Para YEARLY `plusMonths(12 n)` da lo mismo que el antiguo `plusYears(n)` (29-feb → 28-feb).
     */
    private fun plusCycles(anchor: LocalDate, cycle: BillingCycle, meses: Int, n: Long): LocalDate = when (cycle) {
        BillingCycle.WEEKLY -> anchor.plusWeeks(n)
        else -> anchor.plusMonths(n * meses)
    }
}
