package com.subia.service

import com.subia.model.BillingCycle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GmailImportDatesTest {

    private val today = LocalDate.of(2026, 10, 7)

    @Test
    fun `mensual - parte del ultimo correo y avanza un mes`() {
        assertEquals(LocalDate.of(2026, 10, 20), nextRenewalDate("2026-09-20", BillingCycle.MONTHLY, today))
    }

    @Test
    fun `anual - parte del ultimo correo y avanza un año`() {
        assertEquals(LocalDate.of(2027, 3, 15), nextRenewalDate("2026-03-15", BillingCycle.YEARLY, today))
    }

    @Test
    fun `semanal - parte del ultimo correo y avanza una semana`() {
        assertEquals(LocalDate.of(2026, 10, 12), nextRenewalDate("2026-10-05", BillingCycle.WEEKLY, today))
    }

    @Test
    fun `si la fecha calculada ya paso avanza ciclos hasta no ser anterior a hoy`() {
        // Último correo hace 5 meses: 2026-05-01 → +1 mes = 2026-06-01 (pasado) ... → 2026-11-01
        assertEquals(LocalDate.of(2026, 11, 1), nextRenewalDate("2026-05-01", BillingCycle.MONTHLY, today))
    }

    @Test
    fun `sin lastSeen o con formato invalido parte de hoy`() {
        assertEquals(today.plusMonths(1), nextRenewalDate(null, BillingCycle.MONTHLY, today))
        assertEquals(today.plusMonths(1), nextRenewalDate("", BillingCycle.MONTHLY, today))
        assertEquals(today.plusYears(1), nextRenewalDate("ayer", BillingCycle.YEARLY, today))
    }
}
