package com.subia.shared

import com.subia.shared.model.PeriodoCobro
import com.subia.shared.model.Subscription
import com.subia.shared.model.calcularProyeccionCobros
import com.subia.shared.model.cobrosDelMes
import com.subia.shared.model.totalDeDias
import com.subia.shared.viewmodel.DashboardViewModel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Vista "Este mes" de la tira de cobros ([cobrosDelMes]): mes natural completo, del 1 al último,
 * con los cobros ya pasados, y su suma ([totalDeDias]) idéntica a "Este mes pagas"
 * (`ProyeccionCobros.totalMesCompleto`). Al final, el desglose por gasto de las categorías.
 */
class TiraMesTest {

    private fun sub(
        ancla: String,
        intervalo: Int = 1,
        periodo: String = "MONTHLY",
        precio: Double = 10.0,
        moneda: String = "EUR",
        id: Long = 1L,
        activa: Boolean = true,
        categoriaId: Long? = null
    ) = Subscription(
        id = id, nombre = "S$id", precio = precio, moneda = moneda, periodoFacturacion = periodo,
        fechaRenovacion = ancla, fechaAncla = ancla, intervaloMeses = intervalo, activa = activa,
        categoriaId = categoriaId
    )

    private fun d(iso: String) = LocalDate.parse(iso)

    /** Fechas con algún cobro y cuántos cobros tiene cada una. */
    private fun cobrosPorFecha(subs: List<Subscription>, hoy: LocalDate): Map<LocalDate, Int> =
        cobrosDelMes(subs, hoy).filter { it.tieneCobros }.associate { it.fecha to it.cobros.size }

    // ── Estructura ───────────────────────────────────────────────────────────

    @Test
    fun cubreElMesNaturalCompleto_delUnoAlUltimo() {
        assertEquals(30, cobrosDelMes(emptyList(), d("2026-11-15")).size)
        assertEquals(d("2026-11-01"), cobrosDelMes(emptyList(), d("2026-11-15")).first().fecha)
        assertEquals(d("2026-11-30"), cobrosDelMes(emptyList(), d("2026-11-15")).last().fecha)
        assertEquals(31, cobrosDelMes(emptyList(), d("2026-12-31")).size)
        assertEquals(28, cobrosDelMes(emptyList(), d("2026-02-01")).size)
        assertEquals(29, cobrosDelMes(emptyList(), d("2028-02-10")).size)
    }

    @Test
    fun incluyeLosCobrosDeDiasYaPasados() {
        // Hoy es 20: el cobro del 5 ya pasó pero sigue en la vista del mes (atenuado en la UI).
        val dias = cobrosDelMes(listOf(sub("2026-10-05")), d("2026-11-20"))
        assertEquals(1, dias.single { it.fecha == d("2026-11-05") }.cobros.size)
        // La tira de 14 días, en cambio, empieza hoy y no lo ve.
        val p = calcularProyeccionCobros(listOf(sub("2026-10-05")), d("2026-11-20"))
        assertTrue(p.dias.none { it.tieneCobros })
        assertEquals(p.diasMes, dias)
    }

    @Test
    fun ignoraLasInactivas() {
        assertTrue(cobrosPorFecha(listOf(sub("2026-11-05", activa = false)), d("2026-11-01")).isEmpty())
    }

    // ── Periodicidades ───────────────────────────────────────────────────────

    @Test
    fun mensualBimestralTrimestralSemestralYAnual_soloCaenEnSusMeses() {
        val hoy = d("2026-11-10")
        // Mensual: todos los meses.
        assertEquals(mapOf(d("2026-11-03") to 1), cobrosPorFecha(listOf(sub("2026-01-03")), hoy))
        // Bimestral desde septiembre: nov sí, dic no.
        assertEquals(mapOf(d("2026-11-12") to 1), cobrosPorFecha(listOf(sub("2026-09-12", intervalo = 2)), hoy))
        assertTrue(cobrosPorFecha(listOf(sub("2026-09-12", intervalo = 2)), d("2026-12-01")).isEmpty())
        // Trimestral desde agosto: nov sí, oct no.
        assertEquals(mapOf(d("2026-11-20") to 1), cobrosPorFecha(listOf(sub("2026-08-20", intervalo = 3)), hoy))
        assertTrue(cobrosPorFecha(listOf(sub("2026-08-20", intervalo = 3)), d("2026-10-01")).isEmpty())
        // Semestral desde mayo: nov sí.
        assertEquals(mapOf(d("2026-11-01") to 1), cobrosPorFecha(listOf(sub("2026-05-01", intervalo = 6)), hoy))
        // Anual (con campo y backend antiguo sin campo): solo en su mes.
        assertEquals(mapOf(d("2026-11-30") to 1), cobrosPorFecha(listOf(sub("2025-11-30", intervalo = 12, periodo = "YEARLY")), hoy))
        assertEquals(mapOf(d("2026-11-30") to 1), cobrosPorFecha(listOf(sub("2025-11-30", periodo = "YEARLY")), hoy))
        assertTrue(cobrosPorFecha(listOf(sub("2025-11-30", periodo = "YEARLY")), d("2026-12-15")).isEmpty())
    }

    @Test
    fun semanal_cuatroOCincoCobrosSegunElMes() {
        // Ancla jueves 5 de noviembre de 2026: 5, 12, 19 y 26.
        assertEquals(
            listOf(d("2026-11-05"), d("2026-11-12"), d("2026-11-19"), d("2026-11-26")),
            cobrosPorFecha(listOf(sub("2026-11-05", periodo = "WEEKLY")), d("2026-11-15")).keys.sorted()
        )
        // Diciembre de 2026 tiene cinco jueves.
        assertEquals(5, cobrosPorFecha(listOf(sub("2026-11-05", periodo = "WEEKLY")), d("2026-12-15")).size)
    }

    @Test
    fun dia31_caeElUltimoDiaEnLosMesesCortos() {
        assertEquals(mapOf(d("2026-11-30") to 1), cobrosPorFecha(listOf(sub("2026-01-31")), d("2026-11-02")))
        assertEquals(mapOf(d("2026-02-28") to 1), cobrosPorFecha(listOf(sub("2026-01-31")), d("2026-02-02")))
    }

    // ── La suma cuadra con "Este mes pagas" ──────────────────────────────────

    @Test
    fun sumaDeLaVistaMensual_esIgualQueTotalMesCompleto() {
        val subs = listOf(
            sub("2026-01-03", precio = 12.99, id = 1),
            sub("2026-09-12", intervalo = 2, precio = 0.1, id = 2),
            sub("2026-08-20", intervalo = 3, precio = 90.0, id = 3),
            sub("2026-05-01", intervalo = 6, precio = 0.2, id = 4),
            sub("2025-11-30", intervalo = 12, periodo = "YEARLY", precio = 450.0, id = 5),
            sub("2026-11-05", periodo = "WEEKLY", precio = 3.33, id = 6),
            sub("2026-10-31", precio = 9.99, moneda = "USD", id = 7),
            sub("2026-10-07", precio = 5.0, activa = false, id = 8)
        )
        // Varios "hoy" a lo largo del año (meses cortos, largos y con cinco semanas).
        val hoys = listOf("2026-11-01", "2026-11-15", "2026-11-30", "2026-12-31", "2027-02-14", "2027-05-20")
        for (iso in hoys) {
            val p = calcularProyeccionCobros(subs, d(iso))
            assertEquals(p.totalMesCompleto, totalDeDias(p.diasMes), "Descuadre con hoy = $iso")
        }
    }

    @Test
    fun totalDeDias_redondeaAlFinal_comoElRestoDeTotales() {
        // 0,1 + 0,2 en Double da 0,30000000000000004: con diezmilésimas sale 0,30 exacto.
        val p = calcularProyeccionCobros(
            listOf(sub("2026-11-02", precio = 0.1, id = 1), sub("2026-11-03", precio = 0.2, id = 2)),
            d("2026-11-15")
        )
        assertEquals(mapOf("EUR" to 0.3), totalDeDias(p.diasMes))
        assertEquals(mapOf("EUR" to 0.3), p.totalMesCompleto)
    }

    @Test
    fun totalDeDias_ordenaLasDivisasComoElDashboard() {
        val p = calcularProyeccionCobros(
            listOf(sub("2026-11-02", moneda = "GBP", id = 1), sub("2026-11-03", moneda = "USD", id = 2), sub("2026-11-04", id = 3)),
            d("2026-11-15")
        )
        assertEquals(listOf("EUR", "USD", "GBP"), totalDeDias(p.diasMes).keys.toList())
    }

    // ── Desglose por gasto de "Gasto por categoría" ─────────────────────────

    @Test
    fun categoria_traeSusGastosConEquivalenteMensualYPeriodo_deMayorAMenor() {
        val subs = listOf(
            sub("2026-11-01", precio = 9.99, id = 1, categoriaId = 7),
            sub("2026-11-01", intervalo = 3, precio = 90.0, id = 2, categoriaId = 7),
            sub("2026-11-01", periodo = "WEEKLY", precio = 5.0, id = 3, categoriaId = 7),
            sub("2026-11-01", precio = 4.0, id = 4, categoriaId = null)
        )
        val detalle = DashboardViewModel.calcularGastosPorCategoriaDetalle(subs, mapOf(7L to "Hogar"))
        val hogar = detalle.single { it.categoriaId == 7L }
        assertEquals(listOf(2L, 3L, 1L), hogar.gastos.map { it.suscripcionId })
        assertEquals(30.0, hogar.gastos[0].equivalenteMensual)
        assertEquals(PeriodoCobro.PorMeses(3), hogar.gastos[0].periodo)
        assertEquals(90.0, hogar.gastos[0].precio)
        assertEquals(21.67, hogar.gastos[1].equivalenteMensual) // 5 × 52 / 12 = 21,666…
        assertEquals(PeriodoCobro.Semanal, hogar.gastos[1].periodo)
        assertEquals(PeriodoCobro.PorMeses(1), hogar.gastos[2].periodo)
        // La suma de los gastos coincide (al céntimo) con la cifra de la categoría.
        assertEquals(hogar.gastoMensual, hogar.gastos.sumOf { it.equivalenteMensual }, 0.011)
        assertEquals(listOf(4L), detalle.single { it.categoriaId == null }.gastos.map { it.suscripcionId })
    }
}
