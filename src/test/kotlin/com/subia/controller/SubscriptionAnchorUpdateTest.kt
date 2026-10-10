package com.subia.controller

import com.subia.controller.api.ApiSubscriptionController
import com.subia.dto.api.SubscriptionRequestDto
import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Subscription
import com.subia.model.User
import com.subia.model.UserRole
import com.subia.repository.UserRepository
import com.subia.service.CategoryService
import com.subia.service.SubscriptionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * R3-01: editar sin tocar la fecha (que llega proyectada) no debe mover el ancla del día 31 /
 * 29-feb, ni en la API (app) ni en el formulario web. Una fecha distinta sí se guarda.
 */
class SubscriptionAnchorUpdateTest {

    private val madrid = ZoneId.of("Europe/Madrid")
    // 2026-11-10 12:00 en Madrid
    private val clock = Clock.fixed(Instant.parse("2026-11-10T11:00:00Z"), madrid)
    private val category = Category(id = 3, name = "Streaming")
    private val user = User(id = 1L, email = "u@example.com", passwordHash = "x", emailVerified = true, role = UserRole.USER)

    private val subscriptionService = mockk<SubscriptionService>()
    private val categoryService = mockk<CategoryService>()
    private val userRepository = mockk<UserRepository>()
    private val saved = slot<Subscription>()

    private fun stored(anchor: String, cycle: BillingCycle = BillingCycle.MONTHLY) = Subscription(
        id = 7, name = "Netflix", price = BigDecimal("12.99"), billingCycle = cycle,
        renewalDate = LocalDate.parse(anchor), category = category, userId = 1L
    )

    private fun setUp(stored: Subscription) {
        every { userRepository.findByEmail("u@example.com") } returns user
        every { subscriptionService.findById(7, 1L) } returns stored
        every { categoryService.findById(3) } returns category
        every { subscriptionService.save(capture(saved)) } answers { saved.captured }
    }

    private fun apiPut(received: String, cycle: BillingCycle = BillingCycle.MONTHLY): LocalDate {
        val controller = ApiSubscriptionController(subscriptionService, categoryService, userRepository, clock)
        val jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u@example.com").build()
        val res = controller.update(jwt, 7, SubscriptionRequestDto(
            name = "Netflix", price = BigDecimal("12.99"), billingCycle = cycle,
            renewalDate = LocalDate.parse(received), categoryId = 3
        ))
        // La respuesta sigue proyectando y expone el ancla.
        assertEquals(saved.captured.renewalDate, res.data!!.renewalAnchor)
        return saved.captured.renewalDate
    }

    private fun webPost(received: String, cycle: BillingCycle = BillingCycle.MONTHLY): LocalDate {
        val controller = SubscriptionController(subscriptionService, categoryService, mockk(), userRepository, clock)
        val details = org.springframework.security.core.userdetails.User("u@example.com", "x", emptyList())
        controller.update(
            details, 7, name = "Netflix", description = "", price = BigDecimal("12.99"), currency = "EUR",
            billingCycle = cycle, renewalDate = LocalDate.parse(received), categoryId = 3, active = true,
            notes = "", isTrial = false, trialEndsAt = null
        )
        return saved.captured.renewalDate
    }

    @Test
    fun `API - dia 31 proyectado al 30 de noviembre conserva el 31`() {
        setUp(stored("2026-01-31"))
        assertEquals(LocalDate.parse("2026-01-31"), apiPut("2026-11-30"))
    }

    @Test
    fun `API - anual del 29 de febrero conserva el ancla`() {
        setUp(stored("2024-02-29", BillingCycle.YEARLY))
        assertEquals(LocalDate.parse("2024-02-29"), apiPut("2027-02-28", BillingCycle.YEARLY))
    }

    @Test
    fun `API - fecha cambiada por el usuario se guarda`() {
        setUp(stored("2026-01-31"))
        assertEquals(LocalDate.parse("2026-11-20"), apiPut("2026-11-20"))
    }

    @Test
    fun `API - la respuesta proyecta renewalDate y expone renewalAnchor`() {
        setUp(stored("2026-01-31"))
        val controller = ApiSubscriptionController(subscriptionService, categoryService, userRepository, clock)
        val jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u@example.com").build()
        val dto = controller.getById(jwt, 7).data!!
        assertEquals(LocalDate.parse("2026-11-30"), dto.renewalDate)
        assertEquals(LocalDate.parse("2026-01-31"), dto.renewalAnchor)
    }

    @Test
    fun `web - dia 31 proyectado al 30 de noviembre conserva el 31`() {
        setUp(stored("2026-01-31"))
        assertEquals(LocalDate.parse("2026-01-31"), webPost("2026-11-30"))
    }

    @Test
    fun `web - fecha cambiada por el usuario se guarda`() {
        setUp(stored("2026-01-31"))
        assertEquals(LocalDate.parse("2026-12-05"), webPost("2026-12-05"))
    }
}
