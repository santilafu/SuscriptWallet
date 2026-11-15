package com.subia.shared

import com.subia.shared.model.NuevaSuscripcionRequest
import com.subia.shared.model.Periodicidad
import com.subia.shared.model.Subscription
import com.subia.shared.model.equivalenteMensualInterno
import com.subia.shared.model.gastoMensualEquivalente
import com.subia.shared.model.mesesPorCobro
import com.subia.shared.model.totalAnualInterno
import com.subia.shared.viewmodel.DashboardViewModel
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Periodicidad de pago (`intervaloMeses`) en la app: regla de dinero ([Periodicidad]), lectura
 * robusta del campo y totales del Inicio. Los ejemplos son los mismos que los del backend
 * (`PeriodicidadTest` del servidor) para que las cifras coincidan al céntimo.
 */
class PeriodicidadTest {

    private fun sub(precio: Double, periodo: String = "MONTHLY", intervalo: Int = 1, moneda: String = "EUR", categoriaId: Long? = null) =
        Subscription(
            nombre = "X", precio = precio, moneda = moneda, periodoFacturacion = periodo,
            fechaRenovacion = "2026-10-10", intervaloMeses = intervalo, categoriaId = categoriaId
        )

    // ── Regla de dinero ──────────────────────────────────────────────────────

    @Test
    fun aDiezmilesimas_leeLosDecimalesSinErrorBinario() {
        assertEquals(99_900L, Periodicidad.aDiezmilesimas(9.99))
        assertEquals(3_000L, Periodicidad.aDiezmilesimas(0.1 + 0.2)) // 0.30000000000000004
        assertEquals(100_000_000_000L, Periodicidad.aDiezmilesimas(1.0E7))
        assertEquals(0L, Periodicidad.aDiezmilesimas(0.0))
    }

    @Test
    fun aDiezmilesimas_redondeaHalfUp() {
        assertEquals(10_001L, Periodicidad.aDiezmilesimas(1.00005))
        assertEquals(10_000L, Periodicidad.aDiezmilesimas(1.00004))
        assertEquals(-10_001L, Periodicidad.aDiezmilesimas(-1.00005))
    }

    @Test
    fun aDiezmilesimas_rechazaImportesNoFinitosOFueraDeRango() {
        assertFailsWith<IllegalArgumentException> { Periodicidad.aDiezmilesimas(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { Periodicidad.aDiezmilesimas(1.0E20) }
    }

    @Test
    fun cifraVisible_redondeaHalfUp_noAPar() {
        // 0,025 → 0,03 (kotlin.math.round, que redondea a par, daría 0,02).
        assertEquals(0.03, Periodicidad.aCifraVisible(250L))
        assertEquals(0.02, Periodicidad.aCifraVisible(249L))
        assertEquals(10.0, Periodicidad.aCifraVisible(99_999L))
    }

    @Test
    fun dividirHalfUp_laMitadSeAlejaDelCero() {
        assertEquals(3L, Periodicidad.dividirHalfUp(5L, 2L))
        assertEquals(-3L, Periodicidad.dividirHalfUp(-5L, 2L))
        assertEquals(1L, Periodicidad.dividirHalfUp(4L, 3L))
        assertEquals(2L, Periodicidad.dividirHalfUp(5L, 3L))
    }

    @Test
    fun trimestralDe90_son30AlMesY360AlAnio() {
        assertEquals(30.0, Periodicidad.aCifraVisible(Periodicidad.equivalenteMensual(90.0, 3)))
        assertEquals(360.0, Periodicidad.aCifraVisible(Periodicidad.totalAnual(90.0, 3)))
    }

    @Test
    fun trimestralDe10_es3coma3333Interno_y3coma33Visible() {
        val interno = Periodicidad.equivalenteMensual(10.0, 3)
        assertEquals(33_333L, interno)
        assertEquals(3.33, Periodicidad.aCifraVisible(interno))
    }

    @Test
    fun tresTrimestralesDe10_sumanDiezYNo9coma99() {
        val suma = List(3) { Periodicidad.equivalenteMensual(10.0, 3) }.sum()
        assertEquals(99_999L, suma)
        assertEquals(10.0, Periodicidad.aCifraVisible(suma))
    }

    @Test
    fun anualDe120_son10AlMes() {
        assertEquals(10.0, Periodicidad.aCifraVisible(Periodicidad.equivalenteMensual(120.0, 12)))
        assertEquals(120.0, Periodicidad.aCifraVisible(Periodicidad.totalAnual(120.0, 12)))
    }

    @Test
    fun intervaloNoPermitido_falla() {
        assertFailsWith<IllegalArgumentException> { Periodicidad.equivalenteMensual(10.0, 5) }
        assertFailsWith<IllegalArgumentException> { Periodicidad.totalAnual(10.0, 0) }
    }

    // ── Interpretación robusta del intervalo ─────────────────────────────────

    @Test
    fun mesesPorCobro_mensualConIntervalo_usaElIntervalo() {
        assertEquals(1, sub(10.0).mesesPorCobro())
        assertEquals(2, sub(10.0, intervalo = 2).mesesPorCobro())
        assertEquals(3, sub(10.0, intervalo = 3).mesesPorCobro())
        assertEquals(6, sub(10.0, intervalo = 6).mesesPorCobro())
    }

    @Test
    fun mesesPorCobro_anualSinCampo_esDoce() {
        assertEquals(12, sub(120.0, "YEARLY").mesesPorCobro())
        assertEquals(12, sub(120.0, "YEARLY", intervalo = 12).mesesPorCobro())
        assertEquals(12, sub(120.0, " annual ").mesesPorCobro())
    }

    @Test
    fun mesesPorCobro_semanalYDiarioNoVanPorMeses() {
        assertNull(sub(5.0, "WEEKLY").mesesPorCobro())
        assertNull(sub(5.0, "BIWEEKLY").mesesPorCobro())
        assertNull(sub(5.0, "DAILY").mesesPorCobro())
        assertEquals(3, sub(5.0, "QUARTERLY").mesesPorCobro())
    }

    @Test
    fun mesesPorCobro_intervaloCorrupto_usaElNaturalDelCiclo() {
        assertEquals(1, sub(10.0, intervalo = 5).mesesPorCobro())
        assertEquals(1, sub(10.0, intervalo = 0).mesesPorCobro())
        assertEquals(12, sub(10.0, "YEARLY", intervalo = -1).mesesPorCobro())
    }

    @Test
    fun gastoMensualEquivalente_conIntervalo() {
        assertEquals(30.0, sub(90.0, intervalo = 3).gastoMensualEquivalente())
        assertEquals(3.3333, sub(10.0, intervalo = 3).gastoMensualEquivalente())
        assertEquals(10.0, sub(60.0, intervalo = 6).gastoMensualEquivalente())
        assertEquals(10.0, sub(120.0, "YEARLY").gastoMensualEquivalente())
    }

    @Test
    fun semanal_mantieneSuCalculo_52Entre12() {
        // Diferencia previa conocida: el backend usa 4,33 semanas/mes; la app, 52/12.
        assertEquals(216_667L, sub(5.0, "WEEKLY").equivalenteMensualInterno())
        assertEquals(2_600_000L, sub(5.0, "WEEKLY").totalAnualInterno()) // 5 × 52 = 260
    }

    /**
     * Filas "Gasto mensual medio" y "Gasto anual" del detalle: se muestran con TODAS las
     * periodicidades, también la mensual (Alquiler de 395 €) y la anual.
     */
    @Test
    fun detalle_mensualMedioYAnual_conCualquierPeriodicidad() {
        fun filas(s: Subscription) =
            Periodicidad.aCifraVisible(s.equivalenteMensualInterno()) to Periodicidad.aCifraVisible(s.totalAnualInterno())
        assertEquals(395.0 to 4740.0, filas(sub(395.0)))                        // mensual
        assertEquals(30.0 to 360.0, filas(sub(60.0, intervalo = 2)))             // cada 2 meses
        assertEquals(30.0 to 360.0, filas(sub(90.0, intervalo = 3)))             // cada 3 meses
        assertEquals(10.0 to 120.0, filas(sub(60.0, intervalo = 6)))             // cada 6 meses
        assertEquals(10.0 to 120.0, filas(sub(120.0, "YEARLY", intervalo = 12))) // anual
        assertEquals(21.67 to 260.0, filas(sub(5.0, "WEEKLY")))                  // semanal, 52/12
    }

    // ── Serialización ────────────────────────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun respuestaSinElCampo_esIntervalo1() {
        val s = json.decodeFromString<Subscription>(
            """{"id":1,"name":"N","price":9.99,"billingCycle":"MONTHLY","renewalDate":"2026-11-01"}"""
        )
        assertEquals(1, s.intervaloMeses)
        assertNull(s.equivalenteMensual)
        assertEquals(1, s.mesesPorCobro())
    }

    @Test
    fun respuestaAnualSinElCampo_seTrataComoDoce() {
        val s = json.decodeFromString<Subscription>(
            """{"id":1,"name":"N","price":120,"billingCycle":"YEARLY","renewalDate":"2026-11-01"}"""
        )
        assertEquals(12, s.mesesPorCobro())
        assertEquals(10.0, s.gastoMensualEquivalente())
    }

    @Test
    fun respuestaConElCampo_loLee() {
        val s = json.decodeFromString<Subscription>(
            """{"id":1,"name":"N","price":10,"billingCycle":"MONTHLY","renewalDate":"2026-11-01","intervaloMeses":3,"equivalenteMensual":3.33}"""
        )
        assertEquals(3, s.intervaloMeses)
        assertEquals(3.33, s.equivalenteMensual)
    }

    @Test
    fun respuestaConIntervaloNull_usaElDefault() {
        val s = json.decodeFromString<Subscription>(
            """{"id":1,"name":"N","price":10,"billingCycle":"MONTHLY","renewalDate":"2026-11-01","intervaloMeses":null}"""
        )
        assertEquals(1, s.intervaloMeses)
    }

    @Test
    fun request_siempreLlevaElIntervalo_inclusoSiEs1() {
        // Sin él, volver de trimestral a mensual no llegaría al backend (conservaría el 3).
        fun cuerpo(intervalo: Int) = json.encodeToString(
            NuevaSuscripcionRequest.serializer(),
            NuevaSuscripcionRequest(
                nombre = "N", precio = 10.0, periodoFacturacion = "MONTHLY",
                fechaRenovacion = "2026-11-01", intervaloMeses = intervalo
            )
        )
        assertTrue("\"intervaloMeses\":1" in cuerpo(1), cuerpo(1))
        assertTrue("\"intervaloMeses\":3" in cuerpo(3), cuerpo(3))
    }

    // ── Totales del Inicio (DashboardViewModel) ──────────────────────────────

    @Test
    fun dashboard_totalesConTrimestralYSemestral() {
        val subs = listOf(
            sub(90.0, intervalo = 3),      // 30 / mes, 360 / año
            sub(60.0, intervalo = 6),      // 10 / mes, 120 / año
            sub(10.0),                     // 10 / mes, 120 / año
            sub(120.0, "YEARLY")           // 10 / mes, 120 / año (sin campo → 12)
        )
        assertEquals(mapOf("EUR" to 60.0), DashboardViewModel.calcularTotalesPorMoneda(subs))
        assertEquals(mapOf("EUR" to 720.0), DashboardViewModel.calcularTotalesAnualesPorMoneda(subs))
    }

    @Test
    fun dashboard_tresTrimestralesDe10_sumanDiezAlMesY120AlAnio() {
        val subs = List(3) { sub(10.0, intervalo = 3) }
        assertEquals(10.0, DashboardViewModel.calcularTotalesPorMoneda(subs)["EUR"])
        // Anual por gasto = 10 × 12 / 3 = 40 (no 3,33 × 12 = 39,96).
        assertEquals(120.0, DashboardViewModel.calcularTotalesAnualesPorMoneda(subs)["EUR"])
    }

    @Test
    fun dashboard_mensualYAnual_igualQueAntes() {
        val subs = listOf(sub(9.99), sub(119.88, "YEARLY"), sub(15.0, moneda = "USD"))
        assertEquals(mapOf("EUR" to 19.98, "USD" to 15.0), DashboardViewModel.calcularTotalesPorMoneda(subs))
        assertEquals(mapOf("EUR" to 239.76, "USD" to 180.0), DashboardViewModel.calcularTotalesAnualesPorMoneda(subs))
    }

    @Test
    fun dashboard_semanal_igualQueAntes() {
        val subs = listOf(sub(5.0, "WEEKLY"))
        assertEquals(21.67, DashboardViewModel.calcularTotalesPorMoneda(subs)["EUR"])
        assertEquals(260.0, DashboardViewModel.calcularTotalesAnualesPorMoneda(subs)["EUR"])
    }

    @Test
    fun dashboard_categoriasYTopConIntervalo() {
        val subs = listOf(sub(90.0, intervalo = 3, categoriaId = 1), sub(10.0, categoriaId = 2))
        val categorias = DashboardViewModel.calcularGastosPorCategoria(subs, mapOf(1L to "Seguros", 2L to "Música"))
        assertEquals(30.0, categorias["Seguros (EUR)"])
        assertEquals(10.0, categorias["Música (EUR)"])

        val detalle = DashboardViewModel.calcularGastosPorCategoriaDetalle(subs)
        assertEquals(listOf(30.0, 10.0), detalle.map { it.gastoMensual })
        assertEquals(listOf(75, 25), detalle.map { it.porcentaje })

        val top = DashboardViewModel.calcularTopSuscripciones(listOf(sub(10.0, intervalo = 3), sub(5.0)))
        assertEquals(listOf(5.0, 3.33), top.map { it.gastoMensual })
    }
}
