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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
 * Periodicidad (sprint 2) de punta a punta contra PostgreSQL real: JSON de la API (400 con el
 * formato de error de siempre, campos nuevos de la respuesta, lo que queda guardado en la BD) y
 * render de las plantillas Thymeleaf (formulario, lista y dashboard) con la periodicidad nueva.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("com.subia.integration.TestDatabase#available")
class PeriodicidadIntegrationTest {

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

    /** IP distinta por petición para no tropezar con el rate limit por IP. */
    private fun ip() = "10.70.${ipCounter.get() / 250}.${ipCounter.getAndIncrement() % 250 + 1}"

    private fun newUser(): User = userRepository.save(
        User(email = "periodo-${UUID.randomUUID().toString().take(8)}@example.com", passwordHash = "x", emailVerified = true)
    )

    private fun newCategory(): Category =
        categoryRepository.save(Category(name = "Seguros-${UUID.randomUUID().toString().take(6)}", color = "#0ea5e9", icon = "🛡"))

    private fun stored(user: User, category: Category, cycle: BillingCycle, intervalo: Int, price: String = "90.00") =
        subscriptionRepository.save(
            Subscription(
                name = "Seguro hogar", price = BigDecimal(price), billingCycle = cycle, intervaloMeses = intervalo,
                renewalDate = LocalDate.now().plusDays(20), category = category, userId = user.id
            )
        )

    private fun body(categoryId: Long, cycle: String, intervalo: Int?): String {
        val campos = mutableMapOf<String, Any>(
            "name" to "Seguro hogar", "price" to 90.00, "currency" to "EUR", "billingCycle" to cycle,
            "renewalDate" to LocalDate.now().plusDays(20).toString(), "categoryId" to categoryId
        )
        if (intervalo != null) campos["intervaloMeses"] = intervalo
        return objectMapper.writeValueAsString(campos)
    }

    private fun apiPost(user: User, categoryId: Long, cycle: String, intervalo: Int?) =
        mockMvc.post("/api/subscriptions") {
            with(jwt().jwt { it.subject(user.email) })
            header("X-Forwarded-For", ip())
            contentType = MediaType.APPLICATION_JSON
            content = body(categoryId, cycle, intervalo)
        }

    private fun apiPut(user: User, id: Long, categoryId: Long, cycle: String, intervalo: Int?) =
        mockMvc.put("/api/subscriptions/$id") {
            with(jwt().jwt { it.subject(user.email) })
            header("X-Forwarded-For", ip())
            contentType = MediaType.APPLICATION_JSON
            content = body(categoryId, cycle, intervalo)
        }

    private fun reload(id: Long): Subscription = subscriptionRepository.findById(id).orElseThrow()

    // ── API ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `API POST sin campo deduce el intervalo del ciclo`() {
        val owner = newUser(); val cat = newCategory()
        apiPost(owner, cat.id, "MONTHLY", null).andExpect {
            status { isCreated() }
            jsonPath("$.data.billingCycle") { value("MONTHLY") }
            jsonPath("$.data.intervaloMeses") { value(1) }
        }
        apiPost(owner, cat.id, "YEARLY", null).andExpect {
            status { isCreated() }
            jsonPath("$.data.billingCycle") { value("YEARLY") }
            jsonPath("$.data.intervaloMeses") { value(12) }
            jsonPath("$.data.equivalenteMensual") { value(7.5) }
        }
    }

    @Test
    fun `API POST con 3 guarda MONTHLY cada 3 meses y responde 30 euros al mes`() {
        val owner = newUser(); val cat = newCategory()
        val res = apiPost(owner, cat.id, "MONTHLY", 3).andExpect {
            status { isCreated() }
            jsonPath("$.data.billingCycle") { value("MONTHLY") }
            jsonPath("$.data.intervaloMeses") { value(3) }
        }.andReturn()
        // El número sale con sus 2 decimales en el JSON (30.00, no 30 ni 30.0000)
        assertTrue(res.response.contentAsString.contains("\"equivalenteMensual\":30.00"), res.response.contentAsString)
        val id = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()
        assertEquals(BillingCycle.MONTHLY to 3, reload(id).let { it.billingCycle to it.intervaloMeses })
    }

    @Test
    fun `API POST con intervalo 0 o 4 da 400 VALIDATION_ERROR y no guarda nada`() {
        val owner = newUser(); val cat = newCategory()
        listOf(0, 4).forEach { malo ->
            apiPost(owner, cat.id, "MONTHLY", malo).andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("VALIDATION_ERROR") }
                jsonPath("$.data") { doesNotExist() }
            }
        }
        assertEquals(0, subscriptionRepository.findByUserId(owner.id).size)
    }

    @Test
    fun `API PUT sin campo con MONTHLY sobre un trimestral sigue siendo trimestral`() {
        val owner = newUser(); val cat = newCategory()
        val sub = stored(owner, cat, BillingCycle.MONTHLY, 3)
        apiPut(owner, sub.id, cat.id, "MONTHLY", null).andExpect {
            status { isOk() }
            jsonPath("$.data.intervaloMeses") { value(3) }
        }
        assertEquals(3, reload(sub.id).intervaloMeses)
    }

    @Test
    fun `API PUT sin campo con YEARLY sobre un trimestral pasa a anual`() {
        val owner = newUser(); val cat = newCategory()
        val sub = stored(owner, cat, BillingCycle.MONTHLY, 3)
        apiPut(owner, sub.id, cat.id, "YEARLY", null).andExpect {
            status { isOk() }
            jsonPath("$.data.billingCycle") { value("YEARLY") }
            jsonPath("$.data.intervaloMeses") { value(12) }
        }
        assertEquals(BillingCycle.YEARLY to 12, reload(sub.id).let { it.billingCycle to it.intervaloMeses })
    }

    @Test
    fun `API PUT con 6 guarda semestral`() {
        val owner = newUser(); val cat = newCategory()
        val sub = stored(owner, cat, BillingCycle.MONTHLY, 3)
        apiPut(owner, sub.id, cat.id, "MONTHLY", 6).andExpect {
            status { isOk() }
            jsonPath("$.data.intervaloMeses") { value(6) }
            jsonPath("$.data.equivalenteMensual") { value(15.0) }
        }
        assertEquals(6, reload(sub.id).intervaloMeses)
    }

    @Test
    fun `API PUT con intervalo 4 da 400 y no cambia lo guardado`() {
        val owner = newUser(); val cat = newCategory()
        val sub = stored(owner, cat, BillingCycle.MONTHLY, 3)
        apiPut(owner, sub.id, cat.id, "MONTHLY", 4).andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("VALIDATION_ERROR") }
        }
        assertEquals(3, reload(sub.id).intervaloMeses)
    }

    // ── Web ─────────────────────────────────────────────────────────────────────────────

    private val es = Cookie("lang", "es")

    @Test
    fun `web alta con la periodicidad nueva guarda MONTHLY cada 2 meses`() {
        val owner = newUser(); val cat = newCategory()
        mockMvc.post("/subscriptions") {
            with(user(owner.email)); with(csrf())
            param("name", "Luz"); param("price", "60.00"); param("currency", "EUR")
            param("periodicidad", "2")
            param("renewalDate", LocalDate.now().plusDays(10).toString())
            param("categoryId", cat.id.toString()); param("active", "true")
        }.andExpect { status { is3xxRedirection() } }
        val saved = subscriptionRepository.findByUserId(owner.id).single()
        assertEquals(BillingCycle.MONTHLY to 2, saved.billingCycle to saved.intervaloMeses)
    }

    @Test
    fun `web alta con periodicidad no valida da 400`() {
        val owner = newUser(); val cat = newCategory()
        mockMvc.post("/subscriptions") {
            with(user(owner.email)); with(csrf())
            param("name", "Luz"); param("price", "60.00")
            param("periodicidad", "4")
            param("renewalDate", LocalDate.now().plusDays(10).toString())
            param("categoryId", cat.id.toString())
        }.andExpect { status { isBadRequest() } }
        assertEquals(0, subscriptionRepository.findByUserId(owner.id).size)
    }

    @Test
    fun `web edicion conserva el intervalo y el formulario lo preselecciona`() {
        val owner = newUser(); val cat = newCategory()
        val sub = stored(owner, cat, BillingCycle.MONTHLY, 3)

        val html = mockMvc.get("/subscriptions/${sub.id}/edit") {
            with(user(owner.email)); cookie(es)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertTrue(html.contains("name=\"periodicidad\""), "falta el desplegable de periodicidad")
        val desplegable = html.substringAfter("name=\"periodicidad\"").substringBefore("</select>")
        assertTrue(Regex("""<option value="3"\s+selected="selected">Trimestral</option>""").containsMatchIn(desplegable),
            "la opción trimestral no sale seleccionada: $desplegable")
        assertEquals(1, Regex("selected=\"selected\"").findAll(desplegable).count(), "solo una opción seleccionada")

        // El formulario reenvía el valor seleccionado: sigue siendo trimestral
        mockMvc.post("/subscriptions/${sub.id}") {
            with(user(owner.email)); with(csrf())
            param("name", "Seguro hogar"); param("price", "90.00"); param("currency", "EUR")
            param("periodicidad", "3")
            param("renewalDate", LocalDate.now().plusDays(20).toString())
            param("categoryId", cat.id.toString()); param("active", "true")
        }.andExpect { status { is3xxRedirection() } }
        assertEquals(BillingCycle.MONTHLY to 3, reload(sub.id).let { it.billingCycle to it.intervaloMeses })

        // Y si se cambia a semestral, se guarda
        mockMvc.post("/subscriptions/${sub.id}") {
            with(user(owner.email)); with(csrf())
            param("name", "Seguro hogar"); param("price", "90.00"); param("currency", "EUR")
            param("periodicidad", "6")
            param("renewalDate", LocalDate.now().plusDays(20).toString())
            param("categoryId", cat.id.toString()); param("active", "true")
        }.andExpect { status { is3xxRedirection() } }
        assertEquals(6, reload(sub.id).intervaloMeses)
    }

    @Test
    fun `web formulario de alta renderiza las seis opciones sin ninguna seleccionada`() {
        val owner = newUser()
        val html = mockMvc.get("/subscriptions/new") {
            with(user(owner.email)); cookie(es)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        listOf("WEEKLY", "1", "2", "3", "6", "12").forEach { valor ->
            assertTrue(html.contains("<option value=\"$valor\""), "falta la opción $valor")
        }
        assertTrue(!html.contains("selected=\"selected\">Trimestral"), "no debería haber periodicidad preseleccionada")
    }

    @Test
    fun `web lista muestra la periodicidad real`() {
        val owner = newUser(); val cat = newCategory()
        stored(owner, cat, BillingCycle.MONTHLY, 3)
        stored(owner, cat, BillingCycle.MONTHLY, 6)
        stored(owner, cat, BillingCycle.YEARLY, 12)
        val html = mockMvc.get("/subscriptions") {
            with(user(owner.email)); cookie(es)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertTrue(html.contains("/trimestre"), "falta /trimestre")
        assertTrue(html.contains("/semestre"), "falta /semestre")
        assertTrue(html.contains("/año"), "falta /año")

        val htmlEn = mockMvc.get("/subscriptions") {
            with(user(owner.email)); cookie(Cookie("lang", "en"))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertTrue(htmlEn.contains("/3 mo"), "falta /3 mo en inglés")
    }

    @Test
    fun `web dashboard pinta el equivalente mensual calculado en el servidor`() {
        val owner = newUser(); val cat = newCategory()
        stored(owner, cat, BillingCycle.MONTHLY, 3)   // 90 € trimestral → 30.00 €/mes
        val html = mockMvc.get("/dashboard") {
            with(user(owner.email)); cookie(es)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertTrue(html.contains("amounts.push(Number(30.00));"), "el gráfico no usa el equivalente del servidor")
        assertTrue(!html.contains("function toMonthly"), "sigue el cálculo duplicado en JavaScript")
    }
}
