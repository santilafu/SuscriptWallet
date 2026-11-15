package com.subia.service

import com.subia.model.BillingCycle
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PeriodicidadTest {

    private fun bd(s: String) = BigDecimal(s)

    /** Compara por valor (compareTo) para que 30.0000 y 30.00 cuenten como iguales. */
    private fun assertMismoValor(esperado: String, real: BigDecimal) =
        assertEquals(0, bd(esperado).compareTo(real), "esperado $esperado, real $real")

    @Test
    fun `equivalente mensual para cada intervalo permitido`() {
        assertMismoValor("120.00", Periodicidad.equivalenteMensual(bd("120.00"), 1))
        assertMismoValor("60.00", Periodicidad.equivalenteMensual(bd("120.00"), 2))
        assertMismoValor("40.00", Periodicidad.equivalenteMensual(bd("120.00"), 3))
        assertMismoValor("20.00", Periodicidad.equivalenteMensual(bd("120.00"), 6))
        assertMismoValor("10.00", Periodicidad.equivalenteMensual(bd("120.00"), 12))
    }

    @Test
    fun `total anual para cada intervalo permitido`() {
        assertMismoValor("1440.00", Periodicidad.totalAnual(bd("120.00"), 1))
        assertMismoValor("720.00", Periodicidad.totalAnual(bd("120.00"), 2))
        assertMismoValor("480.00", Periodicidad.totalAnual(bd("120.00"), 3))
        assertMismoValor("240.00", Periodicidad.totalAnual(bd("120.00"), 6))
        assertMismoValor("120.00", Periodicidad.totalAnual(bd("120.00"), 12))
    }

    @Test
    fun `90 euros trimestral son 30 al mes y 360 al anio`() {
        assertEquals(bd("30.00"), Periodicidad.aCifraVisible(Periodicidad.equivalenteMensual(bd("90.00"), 3)))
        assertEquals(bd("360.00"), Periodicidad.aCifraVisible(Periodicidad.totalAnual(bd("90.00"), 3)))
    }

    @Test
    fun `10 euros cada 3 meses son 3,3333 internos y 3,33 visibles`() {
        val interno = Periodicidad.equivalenteMensual(bd("10.00"), 3)
        assertEquals(bd("3.3333"), interno)
        assertEquals(bd("3.33"), Periodicidad.aCifraVisible(interno))
        // Total anual exacto: se multiplica antes de dividir, no 3,3333 × 12 = 39,9996
        assertEquals(bd("40.00"), Periodicidad.aCifraVisible(Periodicidad.totalAnual(bd("10.00"), 3)))
    }

    @Test
    fun `el redondeo interno y el visible son HALF_UP`() {
        // 0,05 / 6 = 0,008333… → 0,0083 ; 1,00 / 6 = 0,16666… → 0,1667
        assertEquals(bd("0.0083"), Periodicidad.equivalenteMensual(bd("0.05"), 6))
        assertEquals(bd("0.1667"), Periodicidad.equivalenteMensual(bd("1.00"), 6))
        assertEquals(bd("2.35"), Periodicidad.aCifraVisible(bd("2.345")))
    }

    @Test
    fun `un anual con 12 da exactamente lo mismo que el antiguo calculo entre 12`() {
        val precio = bd("99.99")
        assertEquals(precio.divide(bd("12"), 4, java.math.RoundingMode.HALF_UP), Periodicidad.equivalenteMensual(precio, 12))
        assertMismoValor("99.99", Periodicidad.totalAnual(precio, 12))
    }

    @Test
    fun `intervalos no permitidos se rechazan`() {
        listOf(0, -1, 4, 5, 13, 24).forEach { malo ->
            assertFalse(Periodicidad.esValido(malo))
            assertThrows(IllegalArgumentException::class.java) { Periodicidad.normalizar(BillingCycle.MONTHLY, malo) }
            assertThrows(IllegalArgumentException::class.java) { Periodicidad.equivalenteMensual(bd("10"), malo) }
            assertThrows(IllegalArgumentException::class.java) { Periodicidad.totalAnual(bd("10"), malo) }
        }
        listOf(1, 2, 3, 6, 12).forEach { assertTrue(Periodicidad.esValido(it)) }
    }

    @Test
    fun `normalizar aplica la invariante entre ciclo e intervalo`() {
        assertEquals(BillingCycle.WEEKLY to 1, Periodicidad.normalizar(BillingCycle.WEEKLY, 1))
        assertEquals(BillingCycle.WEEKLY to 1, Periodicidad.normalizar(BillingCycle.WEEKLY, 6))
        assertEquals(BillingCycle.YEARLY to 12, Periodicidad.normalizar(BillingCycle.YEARLY, 1))
        assertEquals(BillingCycle.YEARLY to 12, Periodicidad.normalizar(BillingCycle.MONTHLY, 12))
        assertEquals(BillingCycle.MONTHLY to 3, Periodicidad.normalizar(BillingCycle.YEARLY, 3))
        assertEquals(BillingCycle.MONTHLY to 1, Periodicidad.normalizar(BillingCycle.MONTHLY, 1))
        assertEquals(BillingCycle.MONTHLY to 6, Periodicidad.normalizar(BillingCycle.MONTHLY, 6))
    }

    @Test
    fun `meses por cobro no admite semanal`() {
        assertEquals(12, Periodicidad.mesesPorCobro(BillingCycle.YEARLY, 1))
        assertEquals(2, Periodicidad.mesesPorCobro(BillingCycle.MONTHLY, 2))
        assertThrows(IllegalArgumentException::class.java) { Periodicidad.mesesPorCobro(BillingCycle.WEEKLY, 1) }
    }
}
