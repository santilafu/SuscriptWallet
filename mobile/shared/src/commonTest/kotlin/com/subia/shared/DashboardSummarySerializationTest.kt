package com.subia.shared

import com.subia.shared.model.DashboardSummary
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pruebas de robustez de serialización para [DashboardSummary].
 *
 * Replica la configuración Json EXACTA de producción (ApiClient / DashboardViewModel):
 * `ignoreUnknownKeys + isLenient + coerceInputValues`. El objetivo es garantizar que una
 * respuesta del backend con campos ausentes o `null` NO lance `SerializationException`
 * (que en la app se traduce en pantalla de error / dashboard vacío en lugar de datos).
 *
 * Cubre el fix que añadió defaults a todos los campos de [DashboardSummary].
 */
class DashboardSummarySerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun empty_object_parses_to_all_defaults() {
        val item = json.decodeFromString<DashboardSummary>("{}")

        assertEquals(0.0, item.gastoMensual)
        assertEquals(0.0, item.gastoAnual)
        assertEquals(0, item.totalSuscripciones)
        assertTrue(item.renovacionesProximas.isEmpty())
    }

    @Test
    fun null_fields_are_coerced_to_defaults() {
        // El backend podría enviar null explícito en cualquier campo. Con coerceInputValues
        // y defaults, deben coaccionarse al valor por defecto en lugar de crashear.
        val withNulls = """
            {
              "gastoMensual": null,
              "gastoAnual": null,
              "totalSuscripciones": null,
              "renovacionesProximas": null
            }
        """.trimIndent()

        val item = json.decodeFromString<DashboardSummary>(withNulls)

        assertEquals(0.0, item.gastoMensual)
        assertEquals(0.0, item.gastoAnual)
        assertEquals(0, item.totalSuscripciones)
        assertTrue(item.renovacionesProximas.isEmpty())
    }

    @Test
    fun full_payload_parses_all_fields() {
        val full = """
            {
              "gastoMensual": 49.95,
              "gastoAnual": 599.40,
              "totalSuscripciones": 7,
              "renovacionesProximas": [
                {
                  "id": 3,
                  "nombre": "Netflix",
                  "precio": 12.99,
                  "fechaRenovacion": "2026-06-15",
                  "diasRestantes": 5
                }
              ]
            }
        """.trimIndent()

        val item = json.decodeFromString<DashboardSummary>(full)

        assertEquals(49.95, item.gastoMensual)
        assertEquals(599.40, item.gastoAnual)
        assertEquals(7, item.totalSuscripciones)
        assertEquals(1, item.renovacionesProximas.size)
        assertEquals("Netflix", item.renovacionesProximas.first().nombre)
        assertEquals(5, item.renovacionesProximas.first().diasRestantes)
    }

    @Test
    fun unknown_keys_are_ignored() {
        // El backend puede añadir campos nuevos sin romper clientes antiguos.
        val withExtra = """
            {
              "gastoMensual": 10.0,
              "gastoAnual": 120.0,
              "totalSuscripciones": 2,
              "renovacionesProximas": [],
              "campoFuturoDesconocido": "ignorar"
            }
        """.trimIndent()

        val item = json.decodeFromString<DashboardSummary>(withExtra)

        assertEquals(10.0, item.gastoMensual)
        assertEquals(2, item.totalSuscripciones)
    }
}
