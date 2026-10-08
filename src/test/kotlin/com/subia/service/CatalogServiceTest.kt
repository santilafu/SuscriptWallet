package com.subia.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.subia.model.BillingCycle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
        assertFalse(service.getAllItems().first { it.name == "Securitas Direct" }.variablePrice)
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

    @Test
    fun `el JSON de la API expone variablePrice`() {
        val json = ObjectMapper().registerKotlinModule()
            .writeValueAsString(service.getAllItems().first { it.name == "Iberdrola" })
        assertTrue(json.contains("\"variablePrice\":true"), json)
        assertTrue(json.contains("\"domain\":\"iberdrola.es\""), json)
    }
}
