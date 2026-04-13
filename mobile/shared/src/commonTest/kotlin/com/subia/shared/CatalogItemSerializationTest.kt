package com.subia.shared

import com.subia.shared.model.CatalogItem
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pruebas de serialización para [CatalogItem] en el change `catalogo-modelo` (v2.13.0).
 *
 * Cubre los criterios de aceptación AC1 (legacy JSON sin campos nuevos) y AC2
 * (JSON completo v2.13.0 con `priceAnnual` e `iconUrl`), además del caso
 * anual-only y un blob claramente inválido.
 */
class CatalogItemSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = false }

    @Test
    fun legacy_json_parses_with_null_new_fields_and_id_0() {
        val legacy = """
            {
              "name": "Netflix",
              "price": 12.99,
              "currency": "EUR",
              "billingCycle": "MONTHLY",
              "description": "Streaming de cine y series",
              "categoryKey": "streaming",
              "domain": "netflix.com"
            }
        """.trimIndent()

        val item = json.decodeFromString<CatalogItem>(legacy)

        assertEquals(0L, item.id)
        assertEquals("Netflix", item.nombre)
        assertEquals(12.99, item.precioMensual)
        assertNull(item.precioAnual)
        assertNull(item.iconUrl)
        assertEquals("EUR", item.moneda)
        assertEquals("MONTHLY", item.periodoFacturacion)
        assertEquals("streaming", item.categoriaKey)
    }

    @Test
    fun full_v2_13_json_parses_all_new_fields() {
        val full = """
            {
              "id": 42,
              "name": "Microsoft 365",
              "price": 9.99,
              "priceAnnual": 99.99,
              "currency": "EUR",
              "billingCycle": "MONTHLY",
              "description": "Ofimática y 1 TB de OneDrive",
              "categoryKey": "productividad",
              "trialDays": 30,
              "cancelUrl": "https://account.microsoft.com/services",
              "domain": "microsoft.com",
              "iconUrl": "https://suscriptwallet.com/icons/microsoft.webp"
            }
        """.trimIndent()

        val item = json.decodeFromString<CatalogItem>(full)

        assertEquals(42L, item.id)
        assertEquals("Microsoft 365", item.nombre)
        assertEquals(9.99, item.precioMensual)
        assertEquals(99.99, item.precioAnual)
        assertEquals("https://suscriptwallet.com/icons/microsoft.webp", item.iconUrl)
        assertEquals(30, item.diasPrueba)
        assertEquals("microsoft.com", item.domain)
        assertNotNull(item.cancelUrl)
    }

    @Test
    fun annual_only_json_parses_with_null_monthly() {
        val annual = """
            {
              "name": "Nintendo Switch Online Family",
              "priceAnnual": 34.99,
              "currency": "EUR",
              "billingCycle": "YEARLY",
              "categoryKey": "gaming",
              "domain": "nintendo.com"
            }
        """.trimIndent()

        val item = json.decodeFromString<CatalogItem>(annual)

        assertNull(item.precioMensual)
        assertEquals(34.99, item.precioAnual)
        assertEquals("YEARLY", item.periodoFacturacion)
    }

    @Test
    fun malformed_json_throws_serialization_exception() {
        val broken = """{"name": "X", "price": "not-a-number"}"""

        assertFailsWith<Exception> {
            json.decodeFromString<CatalogItem>(broken)
        }
    }
}
