package com.subia.shared

import com.subia.shared.model.NuevaSuscripcionRequest
import com.subia.shared.model.Subscription
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de serialización para [Subscription] y [NuevaSuscripcionRequest].
 *
 * Replica la configuración Json de producción (ApiClient / SuscripcionesViewModel):
 * `ignoreUnknownKeys + isLenient + coerceInputValues`. Verifica el mapeo de
 * `@SerialName` (name/price/billingCycle/renewalDate/categoryId/...), el uso de
 * defaults para campos opcionales y el fallo controlado ante datos obligatorios
 * inválidos — toda esta superficie ha provocado crashes históricos en la app.
 */
class SubscriptionSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun minimal_backend_payload_parses_with_defaults() {
        // Solo los campos obligatorios que el backend siempre envía.
        val minimal = """
            {
              "name": "Spotify",
              "price": 10.99,
              "billingCycle": "MONTHLY",
              "renewalDate": "2026-07-01"
            }
        """.trimIndent()

        val sub = json.decodeFromString<Subscription>(minimal)

        assertEquals("Spotify", sub.nombre)
        assertEquals(10.99, sub.precio)
        assertEquals("MONTHLY", sub.periodoFacturacion)
        assertEquals("2026-07-01", sub.fechaRenovacion)
        // Defaults
        assertEquals(0L, sub.id)
        assertEquals("", sub.descripcion)
        assertEquals("EUR", sub.moneda)
        assertNull(sub.categoriaId)
        assertTrue(sub.activa)
        assertEquals("", sub.notas)
        assertFalse(sub.esPrueba)
        assertNull(sub.fechaFinPrueba)
    }

    @Test
    fun full_payload_maps_all_serial_names() {
        val full = """
            {
              "id": 99,
              "name": "Disney+",
              "descripcion": "Streaming familiar",
              "price": 8.99,
              "moneda": "USD",
              "billingCycle": "YEARLY",
              "renewalDate": "2026-12-31",
              "categoryId": 4,
              "active": false,
              "notes": "Compartida",
              "isTrial": true,
              "trialEndsAt": "2026-08-01"
            }
        """.trimIndent()

        val sub = json.decodeFromString<Subscription>(full)

        assertEquals(99L, sub.id)
        assertEquals("Disney+", sub.nombre)
        assertEquals("USD", sub.moneda)
        assertEquals("YEARLY", sub.periodoFacturacion)
        assertEquals(4L, sub.categoriaId)
        assertFalse(sub.activa)
        assertEquals("Compartida", sub.notas)
        assertTrue(sub.esPrueba)
        assertEquals("2026-08-01", sub.fechaFinPrueba)
    }

    @Test
    fun null_categoryId_is_allowed() {
        // categoryId es nullable: "Sin categoría" llega como null explícito.
        val json5 = """
            {
              "name": "iCloud",
              "price": 2.99,
              "billingCycle": "MONTHLY",
              "renewalDate": "2026-07-10",
              "categoryId": null
            }
        """.trimIndent()

        val sub = json.decodeFromString<Subscription>(json5)
        assertNull(sub.categoriaId)
    }

    @Test
    fun null_in_optional_string_with_default_is_coerced() {
        // `notes` es no-nullable con default ""; un null explícito del backend
        // debe coaccionarse a "" (coerceInputValues) en vez de crashear.
        val withNull = """
            {
              "name": "HBO Max",
              "price": 9.99,
              "billingCycle": "MONTHLY",
              "renewalDate": "2026-07-20",
              "notes": null
            }
        """.trimIndent()

        val sub = json.decodeFromString<Subscription>(withNull)
        assertEquals("", sub.notas)
    }

    @Test
    fun list_of_subscriptions_parses() {
        val list = """
            [
              {"name":"A","price":1.0,"billingCycle":"MONTHLY","renewalDate":"2026-07-01"},
              {"name":"B","price":2.0,"billingCycle":"YEARLY","renewalDate":"2026-08-01"}
            ]
        """.trimIndent()

        val subs = json.decodeFromString<List<Subscription>>(list)

        assertEquals(2, subs.size)
        assertEquals("A", subs[0].nombre)
        assertEquals("YEARLY", subs[1].periodoFacturacion)
    }

    @Test
    fun missing_required_field_throws() {
        // Sin `price` (obligatorio, sin default) debe fallar de forma controlada.
        val broken = """
            {
              "name": "X",
              "billingCycle": "MONTHLY",
              "renewalDate": "2026-07-01"
            }
        """.trimIndent()

        assertFailsWith<Exception> {
            json.decodeFromString<Subscription>(broken)
        }
    }

    @Test
    fun nueva_suscripcion_request_serializes_with_serial_names() {
        val req = NuevaSuscripcionRequest(
            nombre = "Audible",
            precio = 9.99,
            periodoFacturacion = "MONTHLY",
            fechaRenovacion = "2026-09-01",
            categoriaId = 2
        )

        val encoded = json.encodeToString(NuevaSuscripcionRequest.serializer(), req)

        // Debe usar las claves del backend, no los nombres Kotlin.
        assertTrue(encoded.contains("\"name\""))
        assertTrue(encoded.contains("\"billingCycle\""))
        assertTrue(encoded.contains("\"renewalDate\""))
        assertTrue(encoded.contains("\"categoryId\""))
        assertFalse(encoded.contains("\"nombre\""))
        assertFalse(encoded.contains("\"periodoFacturacion\""))
    }
}
