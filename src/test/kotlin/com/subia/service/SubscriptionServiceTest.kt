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
}
