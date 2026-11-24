package com.subia.controller

import com.subia.controller.api.ApiSubscriptionController
import com.subia.dto.api.SubscriptionDto
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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Sprint 2 de la periodicidad: la API y la web ya no pierden `intervaloMeses` al guardar, y las
 * apps antiguas (que no envían el campo) no convierten un trimestral en mensual al editar.
 *
 * Los mocks de `save` aplican [SubscriptionService.validated], igual que el servicio real, para
 * comprobar el resultado ya normalizado. El 400 por HTTP y el render de las plantillas se prueban
 * en `integration/PeriodicidadIntegrationTest` (necesita el PostgreSQL de docker).
 */
class SubscriptionPeriodicidadControllerTest {

    private val madrid = ZoneId.of("Europe/Madrid")
    // 2026-11-10 12:00 en Madrid
    private val clock = Clock.fixed(Instant.parse("2026-11-10T11:00:00Z"), madrid)
    private val category = Category(id = 3, name = "Seguros")
    private val user = User(id = 1L, email = "u@example.com", passwordHash = "x", emailVerified = true, role = UserRole.USER)

    private val subscriptionService = mockk<SubscriptionService>()
    private val categoryService = mockk<CategoryService>()
    private val userRepository = mockk<UserRepository>()
    private val saved = slot<Subscription>()

    private val api = ApiSubscriptionController(subscriptionService, categoryService, userRepository, clock)
    private val web = SubscriptionController(subscriptionService, categoryService, mockk(), userRepository, clock)
    private val jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u@example.com").build()
    private val details = org.springframework.security.core.userdetails.User("u@example.com", "x", emptyList())

    private fun stored(cycle: BillingCycle, intervalo: Int) = Subscription(
        id = 7, name = "Seguro hogar", price = BigDecimal("90.00"), billingCycle = cycle, intervaloMeses = intervalo,
        renewalDate = LocalDate.parse("2026-01-20"), category = category, userId = 1L
    )

    private fun setUp(stored: Subscription? = null) {
        every { userRepository.findByEmail("u@example.com") } returns user
        every { categoryService.findById(3) } returns category
        if (stored != null) every { subscriptionService.findById(7, 1L) } returns stored
        every { subscriptionService.save(capture(saved)) } answers { SubscriptionService.validated(saved.captured) }
        every { subscriptionService.save(capture(saved), 1L) } answers {
            SubscriptionService.validated(saved.captured.copy(id = 7, userId = 1L))
        }
    }

    private fun request(cycle: BillingCycle, intervalo: Int?, renewal: String = "2026-12-01") = SubscriptionRequestDto(
        name = "Seguro hogar", price = BigDecimal("90.00"), billingCycle = cycle,
        renewalDate = LocalDate.parse(renewal), categoryId = 3, intervaloMeses = intervalo
    )

    private fun apiPost(cycle: BillingCycle, intervalo: Int?): SubscriptionDto =
        api.create(jwt, request(cycle, intervalo)).data!!

    private fun apiPut(cycle: BillingCycle, intervalo: Int?): SubscriptionDto =
        api.update(jwt, 7, request(cycle, intervalo)).data!!

    // ── API: alta ───────────────────────────────────────────────────────────────────────

    @Test
    fun `API POST sin intervalo - MONTHLY guarda 1 y YEARLY guarda 12`() {
        setUp()
        val mensual = apiPost(BillingCycle.MONTHLY, null)
        assertEquals(1, saved.captured.intervaloMeses)
        assertEquals(1, mensual.intervaloMeses)
        assertEquals("MONTHLY", mensual.billingCycle)

        val anual = apiPost(BillingCycle.YEARLY, null)
        assertEquals(12, saved.captured.intervaloMeses)
        assertEquals(12, anual.intervaloMeses)
        assertEquals("YEARLY", anual.billingCycle)
    }

    @Test
    fun `API POST con 3 guarda MONTHLY cada 3 meses y devuelve el equivalente mensual`() {
        setUp()
        val dto = apiPost(BillingCycle.MONTHLY, 3)
        assertEquals(BillingCycle.MONTHLY, saved.captured.billingCycle)
        assertEquals(3, saved.captured.intervaloMeses)
        assertEquals("MONTHLY", dto.billingCycle)
        assertEquals(3, dto.intervaloMeses)
        assertEquals(BigDecimal("30.00"), dto.equivalenteMensual)
    }

    @Test
    fun `API POST con 12 deriva YEARLY aunque el ciclo enviado sea MONTHLY`() {
        setUp()
        val dto = apiPost(BillingCycle.MONTHLY, 12)
        assertEquals("YEARLY", dto.billingCycle)
        assertEquals(12, dto.intervaloMeses)
        assertEquals(BigDecimal("7.50"), dto.equivalenteMensual)
    }

    @Test
    fun `API POST con intervalo 0 o 4 lanza IllegalArgumentException (400 VALIDATION_ERROR)`() {
        setUp()
        assertThrows(IllegalArgumentException::class.java) { apiPost(BillingCycle.MONTHLY, 0) }
        assertThrows(IllegalArgumentException::class.java) { apiPost(BillingCycle.MONTHLY, 4) }
        assertEquals(false, saved.isCaptured)
    }

    // ── API: edición ────────────────────────────────────────────────────────────────────

    @Test
    fun `API PUT sin intervalo con MONTHLY sobre un trimestral conserva el 3`() {
        setUp(stored(BillingCycle.MONTHLY, 3))
        val dto = apiPut(BillingCycle.MONTHLY, null)
        assertEquals(3, saved.captured.intervaloMeses)
        assertEquals(3, dto.intervaloMeses)
        assertEquals(BigDecimal("30.00"), dto.equivalenteMensual)
    }

    @Test
    fun `API PUT sin intervalo con YEARLY sobre un trimestral pasa a 12`() {
        setUp(stored(BillingCycle.MONTHLY, 3))
        val dto = apiPut(BillingCycle.YEARLY, null)
        assertEquals(BillingCycle.YEARLY, saved.captured.billingCycle)
        assertEquals(12, saved.captured.intervaloMeses)
        assertEquals(12, dto.intervaloMeses)
    }

    @Test
    fun `API PUT con 6 guarda 6`() {
        setUp(stored(BillingCycle.MONTHLY, 3))
        val dto = apiPut(BillingCycle.MONTHLY, 6)
        assertEquals(6, saved.captured.intervaloMeses)
        assertEquals(6, dto.intervaloMeses)
        assertEquals(BigDecimal("15.00"), dto.equivalenteMensual)
    }

    @Test
    fun `API PUT que cambia el intervalo toma la fecha recibida como nueva ancla`() {
        // Trimestral del 20-ene visto el 10-nov → proyectada 20-ene-2027. Pasarlo a semestral sin
        // tocar la fecha es otra serie de cobros: el ancla pasa a ser la fecha recibida.
        setUp(stored(BillingCycle.MONTHLY, 3))
        api.update(jwt, 7, request(BillingCycle.MONTHLY, 6, renewal = "2027-01-20"))
        assertEquals(LocalDate.parse("2027-01-20"), saved.captured.renewalDate)
    }

    @Test
    fun `API PUT sin cambios de periodicidad conserva el ancla del trimestral`() {
        setUp(stored(BillingCycle.MONTHLY, 3))
        api.update(jwt, 7, request(BillingCycle.MONTHLY, null, renewal = "2027-01-20"))
        assertEquals(LocalDate.parse("2026-01-20"), saved.captured.renewalDate)
    }

    @Test
    fun `API GET devuelve equivalente mensual del semanal con el factor del dashboard`() {
        setUp(stored(BillingCycle.WEEKLY, 1).copy(price = BigDecimal("10.00")))
        val dto = api.getById(jwt, 7).data!!
        assertEquals("WEEKLY", dto.billingCycle)
        assertEquals(1, dto.intervaloMeses)
        // 10 × 52 / 12 = 43,3333 → 43,33 (con el antiguo 4,33 daba 43,30)
        assertEquals(BigDecimal("43.33"), dto.equivalenteMensual)
    }

    // ── Web ─────────────────────────────────────────────────────────────────────────────

    private fun webCreate(periodicidad: String?, billingCycle: BillingCycle? = null) {
        web.create(
            details, name = "Seguro hogar", description = "", price = BigDecimal("90.00"), currency = "EUR",
            periodicidad = periodicidad, billingCycle = billingCycle,
            renewalDate = LocalDate.parse("2026-12-01"), categoryId = 3, active = true,
            notes = "", isTrial = false, trialEndsAt = null, fechaInicio = null, fechaFin = null
        )
    }

    private fun webUpdate(periodicidad: String?, billingCycle: BillingCycle? = null, renewal: String = "2026-12-01") {
        web.update(
            details, 7, name = "Seguro hogar", description = "", price = BigDecimal("90.00"), currency = "EUR",
            periodicidad = periodicidad, billingCycle = billingCycle,
            renewalDate = LocalDate.parse(renewal), categoryId = 3, active = true,
            notes = "", isTrial = false, trialEndsAt = null, fechaInicio = null, fechaFin = null
        )
    }

    @Test
    fun `web alta con cada opcion del desplegable`() {
        setUp()
        val esperado = mapOf(
            "WEEKLY" to (BillingCycle.WEEKLY to 1),
            "1" to (BillingCycle.MONTHLY to 1),
            "2" to (BillingCycle.MONTHLY to 2),
            "3" to (BillingCycle.MONTHLY to 3),
            "6" to (BillingCycle.MONTHLY to 6),
            "12" to (BillingCycle.YEARLY to 12)
        )
        esperado.forEach { (opcion, par) ->
            webCreate(opcion)
            assertEquals(par, saved.captured.billingCycle to saved.captured.intervaloMeses, "opción $opcion")
        }
    }

    @Test
    fun `web alta con periodicidad no valida lanza IllegalArgumentException (400)`() {
        setUp()
        listOf("4", "0", "abc").forEach { malo ->
            assertThrows(IllegalArgumentException::class.java) { webCreate(malo) }
        }
        // Sin periodicidad ni billingCycle tampoco se puede guardar
        assertThrows(IllegalArgumentException::class.java) { webCreate(null) }
    }

    @Test
    fun `web edicion con la periodicidad nueva`() {
        setUp(stored(BillingCycle.MONTHLY, 3))
        webUpdate("6")
        assertEquals(BillingCycle.MONTHLY to 6, saved.captured.billingCycle to saved.captured.intervaloMeses)
    }

    @Test
    fun `web edicion que reenvia la misma periodicidad conserva el intervalo`() {
        setUp(stored(BillingCycle.MONTHLY, 3))
        webUpdate("3")
        assertEquals(3, saved.captured.intervaloMeses)
    }

    @Test
    fun `web formulario antiguo (solo billingCycle) conserva el intervalo guardado`() {
        setUp(stored(BillingCycle.MONTHLY, 3))
        webUpdate(periodicidad = null, billingCycle = BillingCycle.MONTHLY)
        assertEquals(3, saved.captured.intervaloMeses)
    }

    @Test
    fun `web formulario de edicion preselecciona la periodicidad guardada`() {
        val model = org.springframework.ui.ExtendedModelMap()
        setUp(stored(BillingCycle.MONTHLY, 3))
        every { categoryService.findAll() } returns listOf(category)
        web.editForm(details, 7, model)
        assertEquals("3", model["periodicidadActual"])

        setUp(stored(BillingCycle.YEARLY, 12))
        web.editForm(details, 7, model)
        assertEquals("12", model["periodicidadActual"])

        setUp(stored(BillingCycle.WEEKLY, 1))
        web.editForm(details, 7, model)
        assertEquals("WEEKLY", model["periodicidadActual"])
    }
}
