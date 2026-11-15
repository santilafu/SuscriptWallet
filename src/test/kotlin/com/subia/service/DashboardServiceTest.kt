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
        cycle: BillingCycle = BillingCycle.MONTHLY, trial: Boolean = false, trialEnds: String? = null,
        intervalo: Int = 1
    ) = Subscription(
        id = id, name = "S$id", price = BigDecimal(price), currency = currency, billingCycle = cycle,
        renewalDate = LocalDate.parse(renewal), category = cat, isTrial = trial,
        trialEndsAt = trialEnds?.let { LocalDate.parse(it) }, userId = 7, intervaloMeses = intervalo
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

    // ── Periodicidad (intervaloMeses) ──────────────────────────────────────────────────

    @Test
    fun `totales con gastos trimestrales y semestrales`() {
        every { repo.findByUserIdAndActiveTrue(7) } returns listOf(
            sub(1, "2026-10-20", price = "90.00", intervalo = 3),   // 30/mes, 360/año
            sub(2, "2026-10-20", price = "60.00", intervalo = 6),   // 10/mes, 120/año
            sub(3, "2026-10-20", price = "10.00"),                  // 10/mes, 120/año
            sub(4, "2026-10-20", price = "120.00", cycle = BillingCycle.YEARLY, intervalo = 12) // 10/mes
        )

        val dash = service.getDashboard(7)
        val stats = service.getDashboardStats(7)

        assertEquals(BigDecimal("60.00"), dash.totalMonthly)
        assertEquals(BigDecimal("720.00"), dash.totalYearly)
        assertEquals(BigDecimal("60.00"), dash.spendByCategory[cat])
        assertEquals(60.0, stats.gastoMensual, 0.001)
        assertEquals(720.0, stats.gastoAnual, 0.001)
        // El trimestral es el que más pesa al mes
        assertEquals(1L, dash.topSubscriptions.first().id)
    }

    @Test
    fun `el redondeo a centimos se hace solo en el total`() {
        every { repo.findByUserIdAndActiveTrue(7) } returns listOf(
            sub(1, "2026-10-20", price = "10.00", intervalo = 3),   // 3,3333/mes
            sub(2, "2026-10-20", price = "10.00", intervalo = 3),
            sub(3, "2026-10-20", price = "10.00", intervalo = 3)
        )

        val dash = service.getDashboard(7)

        // 3 × 3,3333 = 9,9999 → 10,00. Redondeando cada gasto antes (3 × 3,33) saldría 9,99.
        assertEquals(BigDecimal("10.00"), dash.totalMonthly)
        // Anual: 3 × (10 × 12 / 3) = 120 exacto
        assertEquals(BigDecimal("120.00"), dash.totalYearly)
    }

    /**
     * Cálculo de antes de la periodicidad, como referencia. Única diferencia con el original: el
     * semanal ya usa 52 / 12 en vez de 4,33 (factor unificado con la app).
     */
    private fun antiguoMensual(s: Subscription): BigDecimal = when (s.billingCycle) {
        BillingCycle.MONTHLY -> s.price
        BillingCycle.YEARLY -> s.price.divide(BigDecimal("12"), 4, java.math.RoundingMode.HALF_UP)
        BillingCycle.WEEKLY -> s.price.multiply(BigDecimal("52")).divide(BigDecimal("12"), 4, java.math.RoundingMode.HALF_UP)
    }

    private fun antiguoAnual(s: Subscription): BigDecimal = when (s.billingCycle) {
        BillingCycle.MONTHLY -> s.price.multiply(BigDecimal("12"))
        BillingCycle.YEARLY -> s.price
        BillingCycle.WEEKLY -> s.price.multiply(BigDecimal("52"))
    }

    @Test
    fun `mensuales, anuales y semanales dan los mismos totales que antes`() {
        val subs = listOf(
            sub(1, "2026-10-20", price = "12.99"),
            sub(2, "2026-10-20", price = "99.99", cycle = BillingCycle.YEARLY, intervalo = 12),
            // Entidad anual con el intervalo por defecto (como la construye hoy un controlador)
            sub(3, "2026-10-20", price = "35.50", cycle = BillingCycle.YEARLY),
            sub(4, "2026-10-20", price = "5.00", cycle = BillingCycle.WEEKLY),
            sub(5, "2026-10-20", price = "0.07", cycle = BillingCycle.YEARLY, intervalo = 12)
        )
        every { repo.findByUserIdAndActiveTrue(7) } returns subs

        val dash = service.getDashboard(7)

        val antesMensual = subs.sumOf { antiguoMensual(it) }.setScale(2, java.math.RoundingMode.HALF_UP)
        val antesAnual = subs.sumOf { antiguoAnual(it) }.setScale(2, java.math.RoundingMode.HALF_UP)
        assertEquals(antesMensual, dash.totalMonthly)
        assertEquals(antesAnual, dash.totalYearly)
        // Cifras a mano: 12,99 + 8,3325 + 2,9583 + 21,6667 (5 × 52 / 12) + 0,0058 = 45,9533 → 45,95
        assertEquals(BigDecimal("45.95"), dash.totalMonthly)
        // 155,88 + 99,99 + 35,50 + 260 + 0,07 = 551,44
        assertEquals(BigDecimal("551.44"), dash.totalYearly)
    }

    @Test
    fun `un semanal usa exactamente 52 entre 12 semanas por mes y 52 al anio`() {
        // 18,84 € a la semana: 18,84 × 52 / 12 = 81,64 exacto; con el antiguo 4,33 salían 81,5772 → 81,58.
        // 18,85 € a la semana: 81,6833 → 81,68 (4,33 daba 81,6205 → 81,62).
        every { repo.findByUserIdAndActiveTrue(7) } returns listOf(
            sub(1, "2026-10-20", price = "18.85", cycle = BillingCycle.WEEKLY)
        )

        val dash = service.getDashboard(7)

        assertEquals(BigDecimal("81.68"), dash.totalMonthly)
        // Anual = importe × 52, coherente con mensual × 12 antes de redondear (81,6833 × 12 = 980,20)
        assertEquals(BigDecimal("980.20"), dash.totalYearly)
        assertEquals(BigDecimal("81.68"), dash.topMonthly.single())
        assertEquals(0, BigDecimal("81.6833").compareTo(DashboardService.equivalenteMensual(dash.topSubscriptions.single())))
    }
}
