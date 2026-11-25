package com.subia.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Subscription
import com.subia.model.User
import com.subia.repository.CategoryRepository
import com.subia.repository.SubscriptionRepository
import com.subia.repository.UserRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Préstamos (sprint 2) de punta a punta contra PostgreSQL real: JSON de la API (campos, 400 con
 * VALIDATION_ERROR, PUT de una app antigua sin los campos) y web (formulario, lista y CSV).
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("com.subia.integration.TestDatabase#available")
class PlazoIntegrationTest {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) = TestDatabase.register(registry)

        private val ipCounter = AtomicInteger(1)
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var categoryRepository: CategoryRepository
    @Autowired lateinit var subscriptionRepository: SubscriptionRepository
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun ip() = "10.71.${ipCounter.get() / 250}.${ipCounter.getAndIncrement() % 250 + 1}"
    private val hoy = LocalDate.now()
    private val es = Cookie("lang", "es")

    private fun newUser(): User = userRepository.save(
        User(email = "plazo-${UUID.randomUUID().toString().take(8)}@example.com", passwordHash = "x", emailVerified = true)
    )

    private fun newCategory(): Category =
        categoryRepository.save(Category(name = "Fin-${UUID.randomUUID().toString().take(6)}", color = "#0ea5e9", icon = "🏦"))

    private fun stored(user: User, category: Category, inicio: LocalDate?, fin: LocalDate?, name: String = "Vitaldent") =
        subscriptionRepository.save(
            Subscription(
                name = name, price = BigDecimal("80.00"), billingCycle = BillingCycle.MONTHLY,
                renewalDate = hoy.plusDays(5), category = category, userId = user.id,
                fechaInicio = inicio, fechaFin = fin
            )
        )

    private fun body(categoryId: Long, extra: Map<String, Any?>): String = objectMapper.writeValueAsString(
        mapOf(
            "name" to "Vitaldent", "price" to 80.00, "currency" to "EUR", "billingCycle" to "MONTHLY",
            "renewalDate" to hoy.plusDays(5).toString(), "categoryId" to categoryId
        ) + extra
    )

    private fun reload(id: Long): Subscription = subscriptionRepository.findById(id).orElseThrow()

    // ── API ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `API POST con fechas las devuelve en ISO y las guarda`() {
        val owner = newUser(); val cat = newCategory()
        val inicio = hoy.minusMonths(2).toString(); val fin = hoy.plusMonths(10).toString()
        val res = mockMvc.post("/api/subscriptions") {
            with(jwt().jwt { it.subject(owner.email) }); header("X-Forwarded-For", ip())
            contentType = MediaType.APPLICATION_JSON
            content = body(cat.id, mapOf("fechaInicio" to inicio, "fechaFin" to fin))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.fechaInicio") { value(inicio) }
            jsonPath("$.data.fechaFin") { value(fin) }
        }.andReturn()
        val id = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()
        assertEquals(LocalDate.parse(fin), reload(id).fechaFin)
    }

    @Test
    fun `API PUT de una app antigua sin los campos conserva el plazo y con vacio lo borra`() {
        val owner = newUser(); val cat = newCategory()
        val sub = stored(owner, cat, hoy.minusMonths(2), hoy.plusMonths(10))

        mockMvc.put("/api/subscriptions/${sub.id}") {
            with(jwt().jwt { it.subject(owner.email) }); header("X-Forwarded-For", ip())
            contentType = MediaType.APPLICATION_JSON
            content = body(cat.id, emptyMap())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.fechaFin") { value(hoy.plusMonths(10).toString()) }
        }
        assertEquals(hoy.minusMonths(2), reload(sub.id).fechaInicio)

        mockMvc.put("/api/subscriptions/${sub.id}") {
            with(jwt().jwt { it.subject(owner.email) }); header("X-Forwarded-For", ip())
            contentType = MediaType.APPLICATION_JSON
            content = body(cat.id, mapOf("fechaInicio" to "", "fechaFin" to ""))
        }.andExpect { status { isOk() } }
        assertNull(reload(sub.id).fechaInicio)
        assertNull(reload(sub.id).fechaFin)
    }

    @Test
    fun `API inicio posterior al fin o formato malo da 400 VALIDATION_ERROR y no guarda`() {
        val owner = newUser(); val cat = newCategory()
        listOf(
            mapOf("fechaInicio" to "2027-02-01", "fechaFin" to "2027-01-01"),
            mapOf("fechaFin" to "01/01/2027"),
            mapOf("fechaFin" to "9999-12-31")
        ).forEach { malo ->
            mockMvc.post("/api/subscriptions") {
                with(jwt().jwt { it.subject(owner.email) }); header("X-Forwarded-For", ip())
                contentType = MediaType.APPLICATION_JSON
                content = body(cat.id, malo)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("VALIDATION_ERROR") }
            }
        }
        assertEquals(0, subscriptionRepository.findByUserId(owner.id).size)

        // El límite superior sí se acepta
        mockMvc.post("/api/subscriptions") {
            with(jwt().jwt { it.subject(owner.email) }); header("X-Forwarded-For", ip())
            contentType = MediaType.APPLICATION_JSON
            content = body(cat.id, mapOf("fechaFin" to "2100-12-31"))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.fechaFin") { value("2100-12-31") }
        }
    }

    @Test
    fun `API dashboard no cuenta un prestamo terminado`() {
        val owner = newUser(); val cat = newCategory()
        stored(owner, cat, null, null, name = "Netflix")
        stored(owner, cat, null, hoy.minusDays(1))
        mockMvc.get("/api/dashboard/stats") {
            with(jwt().jwt { it.subject(owner.email) }); header("X-Forwarded-For", ip())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.gastoMensual") { value(80.0) }
            jsonPath("$.data.totalSuscripciones") { value(1) }
        }
    }

    // ── Web ─────────────────────────────────────────────────────────────────────────────

    private fun webPost(owner: User, url: String, cat: Category, inicio: String, fin: String) =
        mockMvc.post(url) {
            with(user(owner.email)); with(csrf())
            param("name", "Vitaldent"); param("price", "80.00"); param("currency", "EUR")
            param("periodicidad", "1")
            param("renewalDate", hoy.plusDays(5).toString())
            param("categoryId", cat.id.toString()); param("active", "true")
            param("fechaInicio", inicio); param("fechaFin", fin)
        }

    @Test
    fun `web alta con fechas, edicion con campos vacios las borra e inicio posterior da 400`() {
        val owner = newUser(); val cat = newCategory()
        webPost(owner, "/subscriptions", cat, hoy.toString(), hoy.plusMonths(6).toString())
            .andExpect { status { is3xxRedirection() } }
        val saved = subscriptionRepository.findByUserId(owner.id).single()
        assertEquals(hoy.plusMonths(6), saved.fechaFin)

        webPost(owner, "/subscriptions/${saved.id}", cat, "", "").andExpect { status { is3xxRedirection() } }
        assertNull(reload(saved.id).fechaInicio)
        assertNull(reload(saved.id).fechaFin)

        webPost(owner, "/subscriptions/${saved.id}", cat, "2027-02-01", "2027-01-01")
            .andExpect { status { isBadRequest() } }
        assertNull(reload(saved.id).fechaFin)
    }

    @Test
    fun `web lista muestra Terminado y Quedan N cuotas`() {
        val owner = newUser(); val cat = newCategory()
        stored(owner, cat, null, hoy.minusDays(1), name = "Coche")
        // Cobros hoy+5, +1 mes y +2 meses
        stored(owner, cat, null, hoy.plusDays(5).plusMonths(2), name = "Vitaldent")
        val html = mockMvc.get("/subscriptions") {
            with(user(owner.email)); cookie(es)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertTrue(html.contains("Terminado"), "falta la etiqueta Terminado")
        assertTrue(html.contains("Terminó el"), "falta la fecha de fin del terminado")
        assertTrue(html.contains("Quedan 3 cuotas"), "falta Quedan 3 cuotas")
        // Los terminados van al final, como en la app (Coche se guardó antes que Vitaldent).
        assertTrue(
            html.indexOf(">Vitaldent</h3>") in 0 until html.indexOf(">Coche</h3>"),
            "el terminado debe ir después de los vigentes"
        )
    }

    @Test
    fun `web formulario de edicion muestra las fechas, cuotas, pendiente y barra`() {
        val owner = newUser(); val cat = newCategory()
        // Serie mensual de hoy+5: desde hace 9 meses hasta dentro de 2 → 12 totales, 3 restantes
        val sub = stored(owner, cat, hoy.plusDays(5).minusMonths(9), hoy.plusDays(5).plusMonths(2))
        val html = mockMvc.get("/subscriptions/${sub.id}/edit") {
            with(user(owner.email)); cookie(es)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertTrue(html.contains("name=\"fechaInicio\""), "falta el campo fechaInicio")
        assertTrue(html.contains("value=\"${sub.fechaFin}\""), "el fin no sale relleno")
        assertTrue(html.contains("Quedan 3 cuotas"))
        assertTrue(html.contains("EUR 240,00") || html.contains("EUR 240.00"), "falta el pendiente")
        assertTrue(html.contains("9 de 12 cuotas pagadas"))
        assertTrue(html.contains("width:75%"), "falta la barra al 75 %")

        val alta = mockMvc.get("/subscriptions/new") {
            with(user(owner.email)); cookie(es)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertTrue(alta.contains("name=\"fechaFin\""))
        assertFalse(alta.contains("id=\"plazoDetalle\""))
    }

    @Test
    fun `web formulario lleva la calculadora del total sin name y vacia al editar`() {
        val owner = newUser(); val cat = newCategory()
        val sub = stored(owner, cat, LocalDate.parse("2026-03-15"), LocalDate.parse("2027-02-15"))
        for (url in listOf("/subscriptions/new", "/subscriptions/${sub.id}/edit")) {
            val html = mockMvc.get(url) { with(user(owner.email)); cookie(es) }
                .andExpect { status { isOk() } }.andReturn().response.contentAsString
            val input = Regex("<input[^>]*id=\"fieldTotalPrestamo\"[^>]*>").find(html)?.value
            assertNotNull(input, "falta el campo del total en $url")
            // Sin name: el navegador no lo envía, así que nunca llega al servidor ni se guarda.
            assertFalse(input!!.contains("name="), "el total no debe enviarse: $input")
            assertFalse(input.contains("value="), "el total debe empezar vacío: $input")
            assertTrue(html.contains("Importe total del préstamo (opcional)"))
            assertTrue(html.contains("{0} cuotas de {1}"), "faltan los textos del resultado para el JS")
        }
    }

    @Test
    fun `web CSV lleva fecha de inicio y de fin al final`() {
        val owner = newUser(); val cat = newCategory()
        stored(owner, cat, LocalDate.parse("2026-01-20"), LocalDate.parse("2027-06-20"))
        val csv = mockMvc.get("/subscriptions/export") { with(user(owner.email)) }
            .andExpect { status { isOk() } }.andReturn().response.contentAsString
        val lineas = csv.trim().lines()
        assertTrue(lineas[0].endsWith(",Notas,Fecha inicio,Fecha fin"), lineas[0])
        assertTrue(lineas[1].endsWith(",2026-01-20,2027-06-20"), lineas[1])
    }
}
