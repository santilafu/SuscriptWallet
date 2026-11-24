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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Préstamos (sprint 2): `fechaInicio`/`fechaFin` en la API. Ausente (null) conserva lo guardado
 * (apps ≤ 2.18.2 hacen PUT sin los campos), `""` borra y una fecha ISO se guarda. El 400 por HTTP
 * se prueba en `integration/PlazoIntegrationTest`.
 */
class SubscriptionPlazoControllerTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-10T11:00:00Z"), ZoneId.of("Europe/Madrid"))
    private val category = Category(id = 3, name = "Finanzas")
    private val user = User(id = 1L, email = "u@example.com", passwordHash = "x", emailVerified = true, role = UserRole.USER)

    private val subscriptionService = mockk<SubscriptionService>()
    private val categoryService = mockk<CategoryService>()
    private val userRepository = mockk<UserRepository>()
    private val saved = slot<Subscription>()

    private val api = ApiSubscriptionController(subscriptionService, categoryService, userRepository, clock)
    private val jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u@example.com").build()

    private val stored = Subscription(
        id = 7, name = "Vitaldent", price = BigDecimal("80.00"), billingCycle = BillingCycle.MONTHLY,
        renewalDate = LocalDate.parse("2026-11-20"), category = category, userId = 1L,
        fechaInicio = LocalDate.parse("2026-01-20"), fechaFin = LocalDate.parse("2027-06-20")
    )

    init {
        every { userRepository.findByEmail("u@example.com") } returns user
        every { categoryService.findById(3) } returns category
        every { subscriptionService.findById(7, 1L) } returns stored
        every { subscriptionService.save(capture(saved)) } answers { SubscriptionService.validated(saved.captured) }
        every { subscriptionService.save(capture(saved), 1L) } answers {
            SubscriptionService.validated(saved.captured.copy(id = 8, userId = 1L))
        }
    }

    private fun request(inicio: String?, fin: String?) = SubscriptionRequestDto(
        name = "Vitaldent", price = BigDecimal("80.00"), billingCycle = BillingCycle.MONTHLY,
        renewalDate = LocalDate.parse("2026-11-20"), categoryId = 3, intervaloMeses = 1,
        fechaInicio = inicio, fechaFin = fin
    )

    @Test
    fun `PUT sin los campos conserva las fechas guardadas`() {
        val dto = api.update(jwt, 7, request(null, null)).data!!
        assertEquals(LocalDate.parse("2026-01-20"), saved.captured.fechaInicio)
        assertEquals(LocalDate.parse("2027-06-20"), saved.captured.fechaFin)
        assertEquals("2026-01-20", dto.fechaInicio)
        assertEquals("2027-06-20", dto.fechaFin)
    }

    @Test
    fun `PUT con cadena vacia borra la fecha`() {
        val dto = api.update(jwt, 7, request("", " ")).data!!
        assertNull(saved.captured.fechaInicio)
        assertNull(saved.captured.fechaFin)
        assertNull(dto.fechaInicio)
        assertNull(dto.fechaFin)
    }

    @Test
    fun `PUT con fecha ISO la guarda y puede tocar solo una`() {
        api.update(jwt, 7, request(null, "2026-12-20"))
        assertEquals(LocalDate.parse("2026-01-20"), saved.captured.fechaInicio)
        assertEquals(LocalDate.parse("2026-12-20"), saved.captured.fechaFin)
    }

    @Test
    fun `POST sin los campos no pone fechas y con ellas las guarda`() {
        api.create(jwt, request(null, null))
        assertNull(saved.captured.fechaInicio)
        assertNull(saved.captured.fechaFin)

        val dto = api.create(jwt, request("2026-11-20", "2027-02-20")).data!!
        assertEquals(LocalDate.parse("2026-11-20"), saved.captured.fechaInicio)
        assertEquals("2027-02-20", dto.fechaFin)
    }

    @Test
    fun `formato incorrecto da IllegalArgumentException (400 VALIDATION_ERROR)`() {
        val ex = assertThrows(IllegalArgumentException::class.java) { api.update(jwt, 7, request("20/01/2026", null)) }
        assertTrue(ex.message!!.contains("fechaInicio"))
        assertThrows(IllegalArgumentException::class.java) { api.create(jwt, request(null, "2026-13-01")) }
        assertFalse(saved.isCaptured)
    }

    @Test
    fun `inicio posterior al fin da IllegalArgumentException (400 VALIDATION_ERROR)`() {
        assertThrows(IllegalArgumentException::class.java) { api.create(jwt, request("2027-01-01", "2026-12-01")) }
        // También si la fecha que llega choca con la guardada que se conserva
        assertThrows(IllegalArgumentException::class.java) { api.update(jwt, 7, request("2027-07-01", null)) }
    }
}
