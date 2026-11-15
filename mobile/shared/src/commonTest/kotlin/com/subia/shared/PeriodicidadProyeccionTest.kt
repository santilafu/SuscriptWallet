package com.subia.shared

import com.subia.shared.model.Subscription
import com.subia.shared.model.TipoAviso
import com.subia.shared.model.avisosPendientes
import com.subia.shared.model.calcularProyeccionCobros
import com.subia.shared.model.ocurrencias
import com.subia.shared.model.proximaRenovacion
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Fechas de cobro con `intervaloMeses` (siempre desde el ancla), el total "Este mes pagas"
 * (mes natural entero) y los avisos de un gasto trimestral.
 */
class PeriodicidadProyeccionTest {

    private fun sub(
        ancla: String,
        intervalo: Int = 1,
        periodo: String = "MONTHLY",
        precio: Double = 10.0,
        moneda: String = "EUR",
        id: Long = 1L
    ) = Subscription(
        id = id, nombre = "S$id", precio = precio, moneda = moneda, periodoFacturacion = periodo,
        fechaRenovacion = ancla, fechaAncla = ancla, intervaloMeses = intervalo
    )

    private fun d(iso: String) = LocalDate.parse(iso)

    // ── Próxima fecha ────────────────────────────────────────────────────────

    @Test
    fun dia31MasUnMes_es28DeFebrero_o29EnBisiesto() {
        assertEquals(d("2026-02-28"), proximaRenovacion(sub("2026-01-31"), d("2026-02-01")))
        assertEquals(d("2028-02-29"), proximaRenovacion(sub("2028-01-31"), d("2028-02-01")))
    }

    @Test
    fun serieDesdeEl31_vuelveAl31EnMarzo() {
        assertEquals(
            listOf(d("2026-01-31"), d("2026-02-28"), d("2026-03-31"), d("2026-04-30")),
            ocurrencias(sub("2026-01-31"), d("2026-01-31"), d("2026-04-30"))
        )
    }

    @Test
    fun anualDesde29DeFebrero_28EnAniosNormales_y29EnBisiestos() {
        val anualSinCampo = sub("2024-02-29", periodo = "YEARLY") // backend antiguo: intervalo 1 → 12
        assertEquals(d("2025-02-28"), proximaRenovacion(anualSinCampo, d("2024-03-01")))
        assertEquals(
            listOf(d("2025-02-28"), d("2026-02-28"), d("2027-02-28"), d("2028-02-29")),
            ocurrencias(anualSinCampo, d("2024-03-01"), d("2028-12-31"))
        )
        val anualConCampo = sub("2024-02-29", intervalo = 12, periodo = "YEARLY")
        assertEquals(d("2028-02-29"), proximaRenovacion(anualConCampo, d("2027-03-01")))
    }

    @Test
    fun trimestral_cruzaElCambioDeAnio() {
        assertEquals(d("2027-02-15"), proximaRenovacion(sub("2026-11-15", intervalo = 3), d("2026-11-16")))
    }

    @Test
    fun trimestral_cobraCadaTresMeses() {
        assertEquals(
            listOf(d("2026-01-10"), d("2026-04-10"), d("2026-07-10"), d("2026-10-10")),
            ocurrencias(sub("2026-01-10", intervalo = 3), d("2026-01-01"), d("2026-12-31"))
        )
    }

    @Test
    fun semestral_cobraCadaSeisMeses() {
        assertEquals(
            listOf(d("2026-03-05"), d("2026-09-05"), d("2027-03-05")),
            ocurrencias(sub("2026-03-05", intervalo = 6), d("2026-01-01"), d("2027-03-31"))
        )
    }

    @Test
    fun bimestral_cobraCadaDosMeses() {
        assertEquals(d("2026-12-20"), proximaRenovacion(sub("2026-08-20", intervalo = 2), d("2026-11-01")))
    }

    // ── "Este mes pagas" (mes natural entero) ────────────────────────────────

    @Test
    fun esteMesPagas_incluyeUnCobroYaPasadoDelMes() {
        // Hoy es día 20 y el cobro fue el día 2: "lo que queda" no lo cuenta, el mes entero sí.
        val p = calcularProyeccionCobros(listOf(sub("2026-01-02", precio = 12.99)), d("2026-11-20"))
        assertEquals(mapOf("EUR" to 12.99), p.totalMesCompleto)
        assertTrue(p.totalMesActual.isEmpty(), "totalMesActual sigue contando solo desde hoy")
    }

    @Test
    fun esteMesPagas_excluyeTrimestralQueNoCae_eIncluyeSemestralQueSi() {
        val subs = listOf(
            sub("2026-01-02", precio = 12.99, id = 1),                // mensual: sí
            sub("2026-01-10", intervalo = 3, precio = 90.0, id = 2),  // ene, abr, jul, oct: no
            sub("2026-05-25", intervalo = 6, precio = 60.0, id = 3)   // may, nov: sí
        )
        val p = calcularProyeccionCobros(subs, d("2026-11-20"))
        assertEquals(mapOf("EUR" to 72.99), p.totalMesCompleto)
    }

    @Test
    fun esteMesPagas_enDiciembre_yElMesSiguienteEsEnero() {
        val subs = listOf(
            sub("2025-12-03", intervalo = 3, precio = 30.0, id = 1),  // dic 2026 (ya pasado): sí
            sub("2026-10-15", intervalo = 3, precio = 45.0, id = 2)   // ene 2027: no
        )
        val p = calcularProyeccionCobros(subs, d("2026-12-20"))
        assertEquals(mapOf("EUR" to 30.0), p.totalMesCompleto)
        assertEquals(mapOf("EUR" to 45.0), p.totalMesSiguiente)
        assertEquals(2027 to 1, p.meses[1].anio to p.meses[1].mes)
    }

    @Test
    fun esteMesPagas_sumaSinErrorDeComaFlotante_yPorDivisa() {
        val subs = listOf(
            sub("2026-11-01", precio = 0.1, id = 1),
            sub("2026-11-02", precio = 0.1, id = 2),
            sub("2026-11-03", precio = 0.1, id = 3),
            sub("2026-11-04", precio = 5.0, moneda = "USD", id = 4)
        )
        val p = calcularProyeccionCobros(subs, d("2026-11-20"))
        assertEquals(mapOf("EUR" to 0.3, "USD" to 5.0), p.totalMesCompleto)
        assertEquals(listOf("EUR", "USD"), p.totalMesCompleto.keys.toList())
    }

    @Test
    fun esteMesPagas_ignoraLasInactivas() {
        val inactiva = sub("2026-11-02").copy(activa = false)
        assertTrue(calcularProyeccionCobros(listOf(inactiva), d("2026-11-20")).totalMesCompleto.isEmpty())
    }

    // ── Avisos ───────────────────────────────────────────────────────────────

    @Test
    fun trimestral_avisaSoloCadaTresMeses() {
        val trimestral = listOf(sub("2026-01-15", intervalo = 3))
        assertEquals(d("2026-10-15"), avisosPendientes(trimestral, d("2026-10-12"), 3, emptySet()).single().fecha)
        assertTrue(avisosPendientes(trimestral, d("2026-11-12"), 3, emptySet()).isEmpty())
        assertTrue(avisosPendientes(trimestral, d("2026-12-12"), 3, emptySet()).isEmpty())
        val enero = avisosPendientes(trimestral, d("2027-01-12"), 3, emptySet()).single()
        assertEquals(TipoAviso.COBRO, enero.tipo)
        assertEquals(d("2027-01-15"), enero.fecha)
    }

    @Test
    fun anualSinCampo_noAvisaCadaMes() {
        val anual = listOf(sub("2026-03-15", periodo = "YEARLY"))
        assertTrue(avisosPendientes(anual, d("2026-10-12"), 3, emptySet()).isEmpty())
        assertEquals(d("2027-03-15"), avisosPendientes(anual, d("2027-03-13"), 3, emptySet()).single().fecha)
    }
}
