package com.subia.service

import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Subscription
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Préstamos y financiaciones (sprint 1): plazo de un gasto con fecha de inicio y de fin.
 * Tabla de casos que debe coincidir con la de shared (`PlazoGasto`), ver docs/specs/2026-11-24-prestamos-fase0.md.
 * "Hoy" en todos los casos: 2026-10-10.
 */
class PlazoGastoTest {

    private fun d(s: String): LocalDate = LocalDate.parse(s)
    private val hoy = d("2026-10-10")
    private val cat = Category(id = 1, name = "Finanzas")

    private fun sub(
        ancla: String, inicio: String? = null, fin: String? = null, precio: String = "100.00",
        cycle: BillingCycle = BillingCycle.MONTHLY, intervalo: Int = 1
    ) = Subscription(
        name = "Préstamo", price = BigDecimal(precio), billingCycle = cycle, intervaloMeses = intervalo,
        renewalDate = d(ancla), category = cat, userId = 1,
        fechaInicio = inicio?.let(::d), fechaFin = fin?.let(::d)
    )

    @Test
    fun `cuotas restantes con intervalos de 1, 2, 3, 6 y 12 meses`() {
        val casos = mapOf(1 to 15, 2 to 7, 3 to 5, 6 to 2, 12 to 1)
        casos.forEach { (intervalo, esperado) ->
            val cycle = if (intervalo == 12) BillingCycle.YEARLY else BillingCycle.MONTHLY
            val s = sub("2026-01-15", fin = "2027-12-31", cycle = cycle, intervalo = intervalo)
            assertEquals(esperado, RenewalDates.cuotasRestantes(s, hoy), "intervalo $intervalo")
        }
    }

    @Test
    fun `semanal cuenta cada 7 dias`() {
        // 2026-10-13, 20, 27, 11-03, 10, 17, 24 → 7
        val s = sub("2026-10-13", fin = "2026-11-25", cycle = BillingCycle.WEEKLY)
        assertEquals(7, RenewalDates.cuotasRestantes(s, hoy))
    }

    @Test
    fun `dia 31 cae el ultimo dia de los meses cortos`() {
        // 31-oct, 30-nov, 31-dic, 31-ene, 28-feb
        val s = sub("2026-01-31", fin = "2027-03-01")
        assertEquals(5, RenewalDates.cuotasRestantes(s, hoy))
    }

    @Test
    fun `el cobro de hoy cuenta como pendiente`() {
        val s = sub("2026-10-10", fin = "2026-11-10")
        assertEquals(2, RenewalDates.cuotasRestantes(s, hoy))
        assertFalse(RenewalDates.isTerminada(s, hoy))
    }

    @Test
    fun `renewalDate guardada en el futuro - se cuenta desde ella, no desde hoy`() {
        // Hoy 10-oct, próxima renovación guardada 15-dic, fin 15-mar: dic, ene, feb, mar → 4 (como la app)
        val s = sub("2026-12-15", fin = "2027-03-15")
        assertEquals(4, RenewalDates.cuotasRestantes(s, hoy))
        assertEquals(d("2026-12-15"), RenewalDates.proximoCobro(s, hoy))
        assertFalse(RenewalDates.isTerminada(s, hoy))
        assertEquals(BigDecimal("400.00"), RenewalDates.plazo(s, hoy)!!.pendiente)
        assertEquals(4, RenewalDates.cuotasProximoAnio(s, hoy))
        // Con inicio pasado: pagadas solo las anteriores a hoy (jul, ago, sep = 3); oct y nov no
        // llegan (la renovación guardada es dic) → barra 3/7, no 5/9.
        val conInicio = RenewalDates.plazo(sub("2026-12-15", inicio = "2026-07-15", fin = "2027-03-15"), hoy)!!
        assertEquals(4, conInicio.cuotasRestantes)
        assertEquals(7, conInicio.cuotasTotales)
        assertEquals(3, conInicio.cuotasPagadas)
        // Fin antes de la renovación guardada: terminado
        assertTrue(RenewalDates.isTerminada(sub("2026-12-15", fin = "2026-11-15"), hoy))
    }

    @Test
    fun `pagadas son los cobros anteriores a hoy - renovacion guardada el mes que viene`() {
        // Hoy 24-nov, inicio 15-mar, renovación guardada 15-dic, fin 15-feb: mar..nov = 9 pagadas, 3 restantes.
        val plazo = RenewalDates.plazo(sub("2026-12-15", inicio = "2026-03-15", fin = "2027-02-15"), d("2026-11-24"))!!
        assertEquals(9, plazo.cuotasPagadas)
        assertEquals(3, plazo.cuotasRestantes)
        assertEquals(12, plazo.cuotasTotales)
    }

    @Test
    fun `terminado con inicio - las pagadas se cortan en el fin`() {
        val plazo = RenewalDates.plazo(sub("2025-01-15", inicio = "2025-01-15", fin = "2025-12-15"), hoy)!!
        assertEquals(0, plazo.cuotasRestantes)
        assertEquals(12, plazo.cuotasPagadas)
        assertEquals(12, plazo.cuotasTotales)
        assertEquals(100, plazo.progresoPct)
    }

    @Test
    fun `sin fecha de fin no hay plazo ni terminado`() {
        val s = sub("2026-01-15", inicio = "2026-01-15")
        assertNull(RenewalDates.cuotasRestantes(s, hoy))
        assertNull(RenewalDates.plazo(s, hoy))
        assertFalse(RenewalDates.isTerminada(s, hoy))
    }

    @Test
    fun `fin antes del proximo cobro - 0 cuotas y terminado`() {
        val s = sub("2026-01-15", fin = "2026-10-12") // próximo cobro 15-oct > fin
        assertEquals(0, RenewalDates.cuotasRestantes(s, hoy))
        assertTrue(RenewalDates.isTerminada(s, hoy))
        assertEquals(BigDecimal("0.00"), RenewalDates.plazo(s, hoy)!!.pendiente)
    }

    @Test
    fun `fin pasado - terminado`() {
        assertTrue(RenewalDates.isTerminada(sub("2025-01-15", fin = "2025-12-15"), hoy))
    }

    @Test
    fun `inicio futuro - no hay cobros antes del inicio`() {
        val s = sub("2026-10-15", inicio = "2027-01-15", fin = "2027-03-15")
        assertEquals(3, RenewalDates.cuotasRestantes(s, hoy))
        assertEquals(3, RenewalDates.cuotasTotales(s, hoy))
        assertFalse(RenewalDates.isTerminada(s, hoy))
        assertEquals(d("2027-01-15"), RenewalDates.proximoCobro(s, hoy))
    }

    @Test
    fun `inicio futuro sin cobros dentro del plazo - terminado`() {
        // Cobros los días 24; el plazo [1-dic, 15-dic] no contiene ninguno.
        val s = sub("2026-10-24", inicio = "2026-12-01", fin = "2026-12-15")
        assertEquals(0, RenewalDates.cuotasRestantes(s, hoy))
        assertTrue(RenewalDates.isTerminada(s, hoy))
    }

    @Test
    fun `totales cuentan hacia atras desde un ancla reciente y la barra sale de pagadas entre totales`() {
        val s = sub("2026-10-15", inicio = "2026-01-15", fin = "2026-12-15", precio = "80.50")
        val plazo = RenewalDates.plazo(s, hoy)!!
        assertEquals(3, plazo.cuotasRestantes)
        assertEquals(12, plazo.cuotasTotales)
        assertEquals(9, plazo.cuotasPagadas)
        assertEquals(75, plazo.progresoPct)
        assertEquals(BigDecimal("241.50"), plazo.pendiente)
    }

    @Test
    fun `sin inicio no hay totales ni barra`() {
        val plazo = RenewalDates.plazo(sub("2026-01-15", fin = "2026-12-15"), hoy)!!
        assertEquals(3, plazo.cuotasRestantes)
        assertNull(plazo.cuotasTotales)
        assertNull(plazo.cuotasPagadas)
        assertNull(plazo.progresoPct)
    }

    @Test
    fun `pendiente con importe de 4 decimales se redondea al final`() {
        // 3 × 33,3333 = 99,9999 → 100,00
        val plazo = RenewalDates.plazo(sub("2026-01-15", fin = "2026-12-15", precio = "33.3333"), hoy)!!
        assertEquals(BigDecimal("100.00"), plazo.pendiente)
    }

    @Test
    fun `cuotas en el proximo anio - minimo entre restantes y cuotas por anio`() {
        assertEquals(3, RenewalDates.cuotasProximoAnio(sub("2026-01-15", fin = "2026-12-15"), hoy))
        assertEquals(12, RenewalDates.cuotasProximoAnio(sub("2026-01-15", fin = "2030-12-15"), hoy))
        assertEquals(4, RenewalDates.cuotasProximoAnio(sub("2026-01-15", fin = "2030-12-15", intervalo = 3), hoy))
        assertEquals(52, RenewalDates.cuotasProximoAnio(sub("2026-10-13", fin = "2030-12-15", cycle = BillingCycle.WEEKLY), hoy))
        // Inicio dentro de 8 meses y fin dentro de 3 años: en los próximos 12 meses (hasta 9-oct-2027)
        // solo caen jun, jul, ago y sep → 4, no 12.
        assertEquals(4, RenewalDates.cuotasProximoAnio(sub("2026-10-15", inicio = "2027-06-15", fin = "2029-10-15"), hoy))
    }

    @Test
    fun `validated rechaza inicio posterior al fin y acepta fin pasado`() {
        val mala = sub("2026-01-15", inicio = "2026-12-01", fin = "2026-11-01")
        val ex = assertThrows(IllegalArgumentException::class.java) { SubscriptionService.validated(mala) }
        assertTrue(ex.message!!.contains("inicio"))
        SubscriptionService.validated(sub("2024-01-15", inicio = "2024-01-15", fin = "2025-01-15"))
        SubscriptionService.validated(sub("2026-01-15", inicio = "2026-01-15", fin = "2026-01-15"))
    }

    @Test
    fun `validated acota las fechas del plazo entre 2000-01-01 y 2100-12-31`() {
        listOf(sub("2026-01-15", fin = "9999-12-31"), sub("2026-01-15", inicio = "1999-12-31"),
            sub("2026-01-15", inicio = "2026-01-15", fin = "2101-01-01")).forEach { mala ->
            assertThrows(IllegalArgumentException::class.java) { SubscriptionService.validated(mala) }
        }
        SubscriptionService.validated(sub("2026-01-15", inicio = "2000-01-01", fin = "2100-12-31"))
    }
}
