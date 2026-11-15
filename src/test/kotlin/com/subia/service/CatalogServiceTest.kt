package com.subia.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.subia.model.BillingCycle
import com.subia.model.CatalogItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests del catálogo estático: integridad de los datos (nombres únicos, dominios en minúsculas,
 * claves de categoría conocidas) y de las entradas de recibos recurrentes (luz, gas, agua,
 * telecos, seguros, transporte) con su `variablePrice`.
 */
class CatalogServiceTest {

    private val service = CatalogService()
    private val grouped = service.getAllItemsGrouped()

    @Test
    fun `todas las claves del catalogo estan en CategoryMappingService`() {
        val known = CategoryMappingService.KEY_TO_NAME.keys
        grouped.keys.forEach { key -> assertTrue(key in known, "clave sin categoría sembrada: $key") }
        grouped.forEach { (key, items) -> items.forEach { assertEquals(key, it.categoryKey, it.name) } }
    }

    @Test
    fun `los nombres son unicos y los dominios van en minusculas sin esquema`() {
        val all = service.getAllItems()
        // Un mismo servicio puede vivir en varias categorías ("Prueba gratuita" repite Netflix, Spotify…),
        // así que la unicidad se comprueba por (nombre, categoría).
        val dupes = all.groupBy { it.name to it.categoryKey }.filterValues { it.size > 1 }.keys
        assertTrue(dupes.isEmpty(), "nombres duplicados: $dupes")
        all.mapNotNull { it.domain }.forEach { d ->
            assertEquals(d.lowercase(), d, "dominio con mayúsculas: $d")
            assertFalse(d.startsWith("http") || d.startsWith("www."), "dominio mal formado: $d")
        }
    }

    @Test
    fun `existen las categorias nuevas de recibos recurrentes con un minimo de entradas`() {
        assertTrue(grouped.getValue("hogar").size >= 20)
        assertTrue(grouped.getValue("telecos").size >= 15)
        assertTrue(grouped.getValue("seguros").size >= 20)
        assertTrue(grouped.getValue("transporte").size >= 6)
    }

    @Test
    fun `luz gas agua telecos y seguros van marcados como importe variable`() {
        listOf("Iberdrola", "Endesa", "Naturgy", "Canal de Isabel II", "Movistar", "Digi", "Mapfre", "Sanitas")
            .forEach { name ->
                val item = service.getAllItems().first { it.name == name }
                assertTrue(item.variablePrice, "$name debería ser de importe variable")
                assertFalse(item.domain.isNullOrBlank(), "$name necesita dominio para el logo")
            }
        // Las alarmas tienen cuota fija y las suscripciones clásicas no cambian.
        // (Securitas Direct se llama Verisure desde abril de 2026.)
        assertFalse(service.getAllItems().first { it.name == "Verisure (antes Securitas Direct)" }.variablePrice)
        assertFalse(service.getAllItems().first { it.name == "Netflix Estándar" }.variablePrice)
    }

    @Test
    fun `los seguros generales son anuales y los de salud mensuales`() {
        val seguros = grouped.getValue("seguros")
        assertEquals(BillingCycle.YEARLY, seguros.first { it.name == "Mapfre" }.billingCycle)
        assertEquals(BillingCycle.YEARLY, seguros.first { it.name == "Línea Directa" }.billingCycle)
        assertEquals(BillingCycle.MONTHLY, seguros.first { it.name == "Sanitas" }.billingCycle)
        assertEquals(BillingCycle.MONTHLY, seguros.first { it.name == "Adeslas" }.billingCycle)
    }

    @Test
    fun `los genericos sin dominio existen para comunidad parking y autonomos`() {
        val all = service.getAllItems()
        assertNull(all.first { it.name == "Comunidad de propietarios" }.domain)
        assertNull(all.first { it.name == "Parking / garaje" }.domain)
        assertEquals("finanzas", all.first { it.name == "Cuota de autónomo (RETA)" }.categoryKey)
    }

    @Test
    fun `getItemsForCategory resuelve las categorias nuevas por nombre sembrado`() {
        assertEquals(grouped.getValue("hogar"), service.getItemsForCategory("Hogar y suministros"))
        assertEquals(grouped.getValue("seguros"), service.getItemsForCategory("Seguros"))
        assertEquals(grouped.getValue("telecos"), service.getItemsForCategory("Telecomunicaciones"))
        assertEquals(grouped.getValue("transporte"), service.getItemsForCategory("Transporte"))
        // Nombre desconocido → todo el catálogo ordenado por nombre.
        assertEquals(service.getAllItems(), service.getItemsForCategory("Lo que sea"))
    }

    // ── Revisión de precios de noviembre de 2026 ────────────────────────────

    @Test
    fun `todos los precios son positivos y estan en euros`() {
        // Los servicios que cobran en dólares se guardan ya convertidos a EUR (ver KDoc de CatalogService).
        service.getAllItems().forEach { item ->
            assertTrue(item.price > BigDecimal.ZERO, "precio no positivo: ${item.name}")
            assertEquals("EUR", item.currency, "moneda distinta de EUR: ${item.name}")
            item.priceAnnual?.let { assertTrue(it > BigDecimal.ZERO, "anual no positivo: ${item.name}") }
        }
    }

    @Test
    fun `el precio anual de un plan mensual sale mas barato que doce mensualidades`() {
        service.getAllItems()
            .filter { it.billingCycle == BillingCycle.MONTHLY && it.priceAnnual != null }
            .forEach { item ->
                val twelve = item.price.multiply(BigDecimal(12))
                assertTrue(item.priceAnnual!! < twelve, "${item.name}: anual ${item.priceAnnual} ≥ 12 × ${item.price}")
            }
    }

    @Test
    fun `las filas de prueba son copias exactas de su fila original`() {
        val prueba = grouped.getValue("prueba")
        assertEquals(CatalogService.TRIALS.size, prueba.size)
        CatalogService.TRIALS.forEach { (originKey, name, days) ->
            val original = grouped.getValue(originKey).first { it.name == name }
            val trial = prueba.first { it.name == name }
            assertEquals(original.copy(categoryKey = "prueba", trialDays = days), trial, name)
        }
    }

    @Test
    fun `iconUrl si existe es https`() {
        service.getAllItems().mapNotNull { it.iconUrl }.forEach { url ->
            assertTrue(url.startsWith("https://"), "iconUrl sin https: $url")
        }
    }

    @Test
    fun `ningun servicio usa dominios de la lista negra`() {
        // youcam.com es una web para adultos (el editor YouCam es de perfectcorp.com) y
        // splice.com es una empresa de samples de música (el editor de vídeo es spliceapp.com).
        val blacklist = listOf("youcam.com", "splice.com")
        service.getAllItems().forEach { item ->
            assertFalse(item.domain in blacklist, "dominio en lista negra: ${item.name} → ${item.domain}")
            blacklist.forEach { bad ->
                val host = item.cancelUrl?.substringAfter("://")?.substringBefore('/')?.removePrefix("www.")
                assertFalse(host == bad, "cancelUrl en lista negra: ${item.name} → ${item.cancelUrl}")
            }
        }
    }

    @Test
    fun `los servicios principales tienen su precio verificado de noviembre de 2026`() {
        val streaming = grouped.getValue("streaming")
        fun price(name: String, list: List<CatalogItem> = streaming) =
            list.first { it.name == name }.price
        assertEquals(BigDecimal("8.99"), price("Netflix con anuncios"))
        assertEquals(BigDecimal("15.99"), price("YouTube Premium"))
        assertEquals(BigDecimal("10.99"), price("Disney+"))
        assertEquals(BigDecimal("11.99"), price("Spotify Premium", grouped.getValue("musica")))
    }

    @Test
    fun `los nombres antiguos de planes renombrados conservan logo y enlace de baja`() {
        val domains = service.getDomainMap()
        assertEquals("hbomax.com", domains["Max"])
        assertEquals("tv.apple.com", domains["Apple TV+"])
        assertEquals("verisure.es", domains["Securitas Direct"])
        assertEquals(service.getCancelUrlMap()["Cursor Premium"], service.getCancelUrlMap()["Cursor Pro"])
        // Todos los nombres nuevos existen de verdad en el catálogo.
        val names = service.getAllItems().map { it.name }.toSet()
        CatalogService.RENAMED_SERVICES.values.forEach { assertTrue(it in names, "renombrado inexistente: $it") }
    }

    @Test
    fun `el mapa de iconos solo contiene servicios con iconUrl`() {
        val icons = service.getIconUrlMap()
        assertTrue(icons.isNotEmpty())
        assertEquals("https://www.google.com/s2/favicons?domain=midjourney.com&sz=128", icons["Midjourney Standard"])
        assertNull(icons["Netflix Estándar"])
    }

    @Test
    fun `el JSON de la API expone variablePrice`() {
        val json = ObjectMapper().registerKotlinModule()
            .writeValueAsString(service.getAllItems().first { it.name == "Iberdrola" })
        assertTrue(json.contains("\"variablePrice\":true"), json)
        assertTrue(json.contains("\"domain\":\"iberdrola.es\""), json)
    }
}
