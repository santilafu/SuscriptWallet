package com.subia.service

import com.subia.model.BillingCycle
import java.time.LocalDate

/**
 * Fecha de próxima renovación para una suscripción importada desde Gmail (M-07).
 *
 * Parte de la fecha del último correo de cobro (`lastSeen`, formato yyyy-MM-dd) o de hoy si
 * falta o no se puede parsear, y avanza ciclos completos según [cycle] hasta que la fecha no
 * sea anterior a hoy: así no se crean suscripciones ya "vencidas" que disparen alertas falsas.
 */
fun nextRenewalDate(lastSeen: String?, cycle: BillingCycle, today: LocalDate = LocalDate.now()): LocalDate {
    val base = lastSeen?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    var next = base.plusCycle(cycle)
    while (next.isBefore(today)) next = next.plusCycle(cycle)
    return next
}

private fun LocalDate.plusCycle(cycle: BillingCycle): LocalDate = when (cycle) {
    BillingCycle.MONTHLY -> plusMonths(1)
    BillingCycle.YEARLY -> plusYears(1)
    BillingCycle.WEEKLY -> plusWeeks(1)
}
