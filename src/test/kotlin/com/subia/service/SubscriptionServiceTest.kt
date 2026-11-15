package com.subia.service

import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Subscription
import com.subia.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SubscriptionServiceTest {

    private val repo = mockk<SubscriptionRepository>()
    private val service = SubscriptionService(repo)

    private fun sub(name: String = "Netflix", price: String = "12.99", currency: String = "EUR") = Subscription(
        name = name, price = BigDecimal(price), currency = currency, billingCycle = BillingCycle.MONTHLY,
        renewalDate = LocalDate.parse("2026-11-01"), category = Category(id = 1, name = "Streaming")
    )

    @Test
    fun `normaliza nombre, divisa y precio antes de guardar`() {
        every { repo.save(any()) } answers { firstArg() }

        val saved = service.save(sub(name = "  Netflix  ", price = "12.999", currency = "eur"), userId = 3)

        assertEquals("Netflix", saved.name)
        assertEquals("EUR", saved.currency)
        assertEquals(BigDecimal("13.00"), saved.price)
        assertEquals(3L, saved.userId)
    }

    @Test
    fun `datos fuera de los limites de la tabla dan IllegalArgumentException y no llegan a la BD`() {
        assertThrows(IllegalArgumentException::class.java) { service.save(sub(name = "x".repeat(101)), 1) }
        assertThrows(IllegalArgumentException::class.java) { service.save(sub(name = "   "), 1) }
        assertThrows(IllegalArgumentException::class.java) { service.save(sub(price = "-1"), 1) }
        assertThrows(IllegalArgumentException::class.java) { service.save(sub(price = "100000000"), 1) }
        assertThrows(IllegalArgumentException::class.java) { service.save(sub(currency = "EUROS"), 1) }
        verify(exactly = 0) { repo.save(any()) }
    }

    @Test
    fun `borrar una suscripcion ajena lanza NoSuchElementException`() {
        every { repo.findByIdAndUserId(5, 2) } returns null

        assertThrows(NoSuchElementException::class.java) { service.delete(5, 2) }
        verify(exactly = 0) { repo.delete(any()) }
    }

    // ── Periodicidad: invariante entre billingCycle e intervaloMeses ────────────────────

    private fun periodica(cycle: BillingCycle, intervalo: Int) =
        sub().copy(billingCycle = cycle, intervaloMeses = intervalo)

    @Test
    fun `semanal guarda intervalo 1 aunque llegue otro valido`() {
        val v = SubscriptionService.validated(periodica(BillingCycle.WEEKLY, 3))
        assertEquals(BillingCycle.WEEKLY, v.billingCycle)
        assertEquals(1, v.intervaloMeses)
    }

    @Test
    fun `intervalo 12 deriva a anual`() {
        val v = SubscriptionService.validated(periodica(BillingCycle.MONTHLY, 12))
        assertEquals(BillingCycle.YEARLY, v.billingCycle)
        assertEquals(12, v.intervaloMeses)
    }

    @Test
    fun `anual de un cliente antiguo (intervalo por defecto 1) pasa a 12`() {
        val v = SubscriptionService.validated(sub().copy(billingCycle = BillingCycle.YEARLY))
        assertEquals(BillingCycle.YEARLY, v.billingCycle)
        assertEquals(12, v.intervaloMeses)
    }

    @Test
    fun `intervalos 1, 2, 3 y 6 derivan a mensual`() {
        listOf(1, 2, 3, 6).forEach { intervalo ->
            val desdeMensual = SubscriptionService.validated(periodica(BillingCycle.MONTHLY, intervalo))
            assertEquals(BillingCycle.MONTHLY, desdeMensual.billingCycle)
            assertEquals(intervalo, desdeMensual.intervaloMeses)
        }
        // Un anual con intervalo trimestral: manda el intervalo
        val v = SubscriptionService.validated(periodica(BillingCycle.YEARLY, 3))
        assertEquals(BillingCycle.MONTHLY, v.billingCycle)
        assertEquals(3, v.intervaloMeses)
    }

    @Test
    fun `intervalos fuera de 1, 2, 3, 6 y 12 dan IllegalArgumentException y no llegan a la BD`() {
        listOf(0, -1, 4, 13).forEach { malo ->
            BillingCycle.entries.forEach { cycle ->
                assertThrows(IllegalArgumentException::class.java) { service.save(periodica(cycle, malo), 1) }
            }
        }
        verify(exactly = 0) { repo.save(any()) }
    }
}
