package com.subia.shared

import com.subia.shared.model.CatalogItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de los helpers introducidos en `catalogo-modelo` v2.13.0:
 * [CatalogItem.hasBothCycles], [CatalogItem.monthlyEquivalent] y
 * [CatalogItem.annualSavingsPercent].
 *
 * No se prueba directamente `SuscripcionFormViewModel.prerellenarDesdeCatalogo`
 * porque el VM depende de tres repositorios concretos con `ApiClient` y no es
 * viable instanciarlo desde commonTest sin DI. La selección de precio por ciclo
 * (core del pre-rellenado) queda cubierta aquí vía `monthlyEquivalent()` y las
 * aserciones de presencia de cada precio.
 */
class CatalogItemHelpersTest {

    private fun baseItem() = CatalogItem(
        id = 1,
        nombre = "Test",
        moneda = "EUR",
        periodoFacturacion = "MONTHLY",
        categoriaKey = "productividad"
    )

    // ── hasBothCycles ─────────────────────────────────────────────────────

    @Test
    fun hasBothCycles_true_when_both_prices_present() {
        val item = baseItem().copy(precioMensual = 9.99, precioAnual = 99.99)
        assertTrue(item.hasBothCycles())
    }

    @Test
    fun hasBothCycles_false_when_monthly_only() {
        val item = baseItem().copy(precioMensual = 9.99, precioAnual = null)
        assertFalse(item.hasBothCycles())
    }

    @Test
    fun hasBothCycles_false_when_annual_only() {
        val item = baseItem().copy(precioMensual = null, precioAnual = 84.69)
        assertFalse(item.hasBothCycles())
    }

    @Test
    fun hasBothCycles_false_when_none() {
        val item = baseItem()
        assertFalse(item.hasBothCycles())
    }

    // ── monthlyEquivalent ─────────────────────────────────────────────────

    @Test
    fun monthlyEquivalent_prefers_annual_divided_by_12_when_present() {
        val item = baseItem().copy(precioMensual = 9.99, precioAnual = 99.99)
        assertEquals(99.99 / 12.0, item.monthlyEquivalent())
    }

    @Test
    fun monthlyEquivalent_falls_back_to_monthly_when_no_annual() {
        val item = baseItem().copy(precioMensual = 9.99, precioAnual = null)
        assertEquals(9.99, item.monthlyEquivalent())
    }

    @Test
    fun monthlyEquivalent_null_when_no_prices() {
        val item = baseItem()
        assertNull(item.monthlyEquivalent())
    }

    // ── annualSavingsPercent ──────────────────────────────────────────────

    @Test
    fun annualSavingsPercent_null_when_any_price_missing() {
        assertNull(baseItem().copy(precioMensual = 9.99).annualSavingsPercent())
        assertNull(baseItem().copy(precioAnual = 99.99).annualSavingsPercent())
        assertNull(baseItem().annualSavingsPercent())
    }

    @Test
    fun annualSavingsPercent_null_when_monthly_is_zero_or_negative() {
        assertNull(baseItem().copy(precioMensual = 0.0, precioAnual = 10.0).annualSavingsPercent())
        assertNull(baseItem().copy(precioMensual = -1.0, precioAnual = 10.0).annualSavingsPercent())
    }

    @Test
    fun annualSavingsPercent_positive_for_typical_dual_pricing() {
        // 9.99 * 12 = 119.88, ahorro = (119.88 - 99.99) / 119.88 = 16.59% → 16
        val item = baseItem().copy(precioMensual = 9.99, precioAnual = 99.99)
        assertEquals(16, item.annualSavingsPercent())
    }

    @Test
    fun annualSavingsPercent_zero_when_annual_worse_than_monthly_times_12() {
        // Anual peor que 12× mensual → coerceAtLeast(0) devuelve 0
        val item = baseItem().copy(precioMensual = 5.0, precioAnual = 70.0)
        assertEquals(0, item.annualSavingsPercent())
    }

    @Test
    fun annualSavingsPercent_exactly_12x_monthly_is_zero() {
        val item = baseItem().copy(precioMensual = 10.0, precioAnual = 120.0)
        assertEquals(0, item.annualSavingsPercent())
    }
}
