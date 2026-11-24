package com.subia.integration

import com.subia.model.User
import com.subia.repository.UserRepository
import com.subia.service.CatalogService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

/**
 * Catálogo web con items sin precio (préstamos, D6) y contador sin las copias de "prueba" (D8),
 * contra PostgreSQL real porque la categoría "Préstamos" la siembra la V18.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("com.subia.integration.TestDatabase#available")
class CatalogoPrestamosWebTest {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) = TestDatabase.register(registry)
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var catalogService: CatalogService

    private val es = Cookie("lang", "es")

    private fun newUser(): User = userRepository.save(
        User(email = "cat-${UUID.randomUUID().toString().take(8)}@example.com", passwordHash = "x", emailVerified = true)
    )

    private fun html(url: String, owner: User, catalogo: String? = null): String =
        mockMvc.get(url) { with(user(owner.email)); cookie(es); catalogo?.let { param("catalogo", it) } }
            .andExpect { status { isOk() } }.andReturn().response.contentAsString

    @Test
    fun `catalogo muestra los prestamos sin precio, su chip y un Anadir que lleva al formulario`() {
        val page = html("/catalog-browser", newUser())
        assertTrue(page.contains("data-cat=\"prestamos\""), "falta el chip de préstamos")
        assertTrue(page.contains("Importe variable"), "falta el texto de importe variable")
        assertTrue(page.contains("href=\"/subscriptions/new?catalogo=Hipoteca\""), "falta el Añadir de Hipoteca")
        // Ningún préstamo genera el POST directo (price es obligatorio en add-from-catalog).
        assertFalse(page.contains("name=\"name\" value=\"Hipoteca\""), "Hipoteca no debe hacer POST directo")
        // Los préstamos no tienen botón "Cancelar" (llevaba a la web del banco); el resto sí.
        assertFalse(page.contains("href=\"https://www.ing.es/\""), "ING no debe tener botón Cancelar")
        assertTrue(page.contains("card-action cancel"), "los servicios normales conservan Cancelar")
        assertTrue(page.indexOf("data-name=\"ibercaja\"") < page.indexOf("data-name=\"ing\""), "orden sin mayúsculas")
        // El contador cuenta servicios distintos, sin las copias de "prueba".
        val distintos = catalogService.getAllItems().count { it.categoryKey != "prueba" }
        assertTrue(page.contains("$distintos servicios disponibles"), "contador distinto de $distintos")
    }

    @Test
    fun `formulario nuevo con catalogo prerrellena nombre y categoria y deja el importe vacio`() {
        val page = html("/subscriptions/new", newUser(), catalogo = "Vitaldent (financiación)")
        assertTrue(page.contains("value=\"Vitaldent (financiación)\""), "el nombre no sale relleno")
        // El primer desplegable de categorías es el del selector de catálogo: se mira el del formulario.
        val opcion = Regex("<option[^>]*>[^<]*Préstamos")
            .find(page.substringAfter("id=\"fieldCategoryId\""))?.value.orEmpty()
        assertTrue(opcion.contains("selected"), "la categoría Préstamos no sale seleccionada: $opcion")
        val precio = Regex("<input[^>]*id=\"fieldPrice\"[^>]*>").find(page)?.value.orEmpty()
        assertFalse(Regex("value=\"[0-9]").containsMatchIn(precio), "el importe no debe venir relleno: $precio")

        // Un nombre desconocido se ignora: formulario vacío.
        val vacio = html("/subscriptions/new", newUser(), catalogo = "NoExiste")
        assertFalse(vacio.contains("value=\"NoExiste\""))
    }
}
