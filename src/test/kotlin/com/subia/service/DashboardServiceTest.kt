package com.subia.service

import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Subscription
import com.subia.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DashboardServiceTest {

    private val cat = Category(id = 1, name = "Streaming")
    private val repo = mockk<SubscriptionRepository>()

    /** 2026-10-10 00:30 en Madrid = 2026-10-09 22:30 UTC (caso N-19). */
    private val clock = Clock.fixed(Instant.parse("2026-10-09T22:30:00Z"), ZoneId.of("Europe/Madrid"))
    private val service = DashboardService(repo, clock)

    private fun sub(
        id: Long, renewal: String, price: String = "10.00", currency: String = "EUR",
        cycle: BillingCycle = BillingCycle.MONTHLY, trial: Boolean = false, trialEnds: String? = null
    ) = Subscription(
        id = id, name = "S$id", price = BigDecimal(price), currency = currency, billingCycle = cycle,
        renewalDate = LocalDate.parse(renewal), category = cat, isTrial = trial,
        trialEndsAt = trialEnds?.let { LocalDate.parse(it) }, userId = 7
    )

    @Test
    fun `suscripcion antigua aparece en proximas renovaciones con la fecha proyectada`() {
        every { repo.findByUserIdAndActiveTrue(7) } returns listOf(
            sub(1, "2026-05-15"),   // proyectada → 2026-10-15 (5 días)
            sub(2, "2026-05-30"),   // proyectada → 2026-10-30 (20 días)
            sub(3, "2026-01-20", cycle = BillingCycle.YEARLY) // → 2027-01-20, fuera de 30 días
        )

        val dash = service.getDashboard(7)

        assertEquals(listOf(1L, 2L), dash.upcomingRenewals.map { it.id })
        assertEquals(LocalDate.parse("2026-10-15"), dash.upcomingRenewals[0].renewalDate)
        assertEquals(listOf(1L), dash.alertRenewals.map { it.id })
    }

    @Test
    fun `hoy se calcula en la zona de Madrid y no en UTC`() {
        every { repo.findByUserIdAndActiveTrue(7) } returns listOf(sub(1, "2026-10-10"))

        val stats = service.getDashboardStats(7)

        // En UTC aún sería 9 de octubre y saldría 1 día restante
        assertEquals(0, stats.renovacionesProximas.single().diasRestantes)
    }

    @Test
    fun `stats moviles no mezclan divisas ni cuentan pruebas vigentes`() {
        every { repo.findByUserIdAndActiveTrue(7) } returns listOf(
            sub(1, "2026-10-20", price = "20.00", currency = "EUR"),
            sub(2, "2026-10-20", price = "5.00", currency = "USD"),
            sub(3, "2026-10-20", price = "99.00", currency = "EUR", trial = true, trialEnds = "2026-10-20")
        )

        val stats = service.getDashboardStats(7)

        assertEquals(20.0, stats.gastoMensual, 0.001)
        assertEquals(240.0, stats.gastoAnual, 0.001)
        assertEquals(3, stats.totalSuscripciones)
    }

    @Test
    fun `una prueba ya vencida cuenta como de pago en los totales (N-10)`() {
        every { repo.findByUserIdAndActiveTrue(7) } returns listOf(
            sub(1, "2026-10-20", price = "8.00", trial = true, trialEnds = "2026-09-01")
        )

        val dash = service.getDashboard(7)

        assertEquals(BigDecimal("8.00"), dash.totalMonthly)
    }
}
