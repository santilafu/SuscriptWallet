package com.subia.service

import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Subscription
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
    fun `semanal fija el factor 52 entre 12 y no 4,33`() {
        // 10 € a la semana: 10 × 52 / 12 = 43,3333 (con 4,33 serían 43,30)
        assertEquals(bd("43.3333"), Periodicidad.equivalenteMensualSemanal(bd("10.00")))
        assertEquals(bd("43.33"), Periodicidad.aCifraVisible(Periodicidad.equivalenteMensualSemanal(bd("10.00"))))
        // 3 € a la semana: 13,00 exacto (52 × 3 / 12); con 4,33 serían 12,99
        assertEquals(bd("13.00"), Periodicidad.aCifraVisible(Periodicidad.equivalenteMensualSemanal(bd("3.00"))))
        // Se multiplica antes de dividir: 1 € → 4,3333 (escala 4, HALF_UP), no 4,33 × 1
        assertEquals(bd("4.3333"), Periodicidad.equivalenteMensualSemanal(bd("1.00")))
        // 0,02 € → 0,0867 (0,08666… redondeado HALF_UP a 4 decimales)
        assertEquals(bd("0.0867"), Periodicidad.equivalenteMensualSemanal(bd("0.02")))
    }

    @Test
    fun `semanal anual es importe por 52 y coincide con el mensual por 12`() {
        assertEquals(bd("520.0000"), Periodicidad.totalAnualSemanal(bd("10.00")))
        assertEquals(bd("980.20"), Periodicidad.aCifraVisible(Periodicidad.totalAnualSemanal(bd("18.85"))))
        // 3 € → 13,00 al mes × 12 = 156,00 = 3 × 52
        assertEquals(
            Periodicidad.aCifraVisible(Periodicidad.equivalenteMensualSemanal(bd("3.00"))).multiply(bd("12")),
            Periodicidad.aCifraVisible(Periodicidad.totalAnualSemanal(bd("3.00")))
        )
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

    // ── resolver: lo que envía el cliente (sprint 2) ─────────────────────────────────────

    private fun guardado(ciclo: BillingCycle, intervalo: Int) = Subscription(
        id = 1, name = "Seguro", price = bd("90.00"), billingCycle = ciclo, intervaloMeses = intervalo,
        renewalDate = java.time.LocalDate.parse("2026-01-20"), category = Category(id = 1, name = "Seguros")
    )

    @Test
    fun `alta sin intervalo lo deduce del ciclo`() {
        assertEquals(BillingCycle.MONTHLY to 1, Periodicidad.resolver(BillingCycle.MONTHLY, null))
        assertEquals(BillingCycle.YEARLY to 12, Periodicidad.resolver(BillingCycle.YEARLY, null))
        assertEquals(BillingCycle.WEEKLY to 1, Periodicidad.resolver(BillingCycle.WEEKLY, null))
    }

    @Test
    fun `con intervalo manda el intervalo y el ciclo se deriva`() {
        assertEquals(BillingCycle.MONTHLY to 3, Periodicidad.resolver(BillingCycle.MONTHLY, 3))
        assertEquals(BillingCycle.YEARLY to 12, Periodicidad.resolver(BillingCycle.MONTHLY, 12))
        // El cliente SÍ envió 1: es mensual aunque diga YEARLY
        assertEquals(BillingCycle.MONTHLY to 1, Periodicidad.resolver(BillingCycle.YEARLY, 1))
        assertEquals(BillingCycle.WEEKLY to 1, Periodicidad.resolver(BillingCycle.WEEKLY, 1))
    }

    @Test
    fun `intervalo no permitido lanza IllegalArgumentException`() {
        listOf(0, -1, 4, 13).forEach { malo ->
            assertThrows(IllegalArgumentException::class.java) { Periodicidad.resolver(BillingCycle.MONTHLY, malo) }
        }
        assertThrows(IllegalArgumentException::class.java) { Periodicidad.resolver(BillingCycle.WEEKLY, 0) }
    }

    @Test
    fun `edicion sin intervalo y mismo ciclo conserva el guardado`() {
        // App antigua que edita un trimestral: recibe y reenvía MONTHLY
        assertEquals(BillingCycle.MONTHLY to 3,
            Periodicidad.resolver(BillingCycle.MONTHLY, null, guardado(BillingCycle.MONTHLY, 3)))
    }

    @Test
    fun `edicion sin intervalo y ciclo cambiado lo deduce del nuevo ciclo`() {
        assertEquals(BillingCycle.YEARLY to 12,
            Periodicidad.resolver(BillingCycle.YEARLY, null, guardado(BillingCycle.MONTHLY, 3)))
        assertEquals(BillingCycle.MONTHLY to 1,
            Periodicidad.resolver(BillingCycle.MONTHLY, null, guardado(BillingCycle.YEARLY, 12)))
    }

    @Test
    fun `edicion con intervalo usa el recibido aunque el ciclo coincida`() {
        assertEquals(BillingCycle.MONTHLY to 6,
            Periodicidad.resolver(BillingCycle.MONTHLY, 6, guardado(BillingCycle.MONTHLY, 3)))
    }
}
