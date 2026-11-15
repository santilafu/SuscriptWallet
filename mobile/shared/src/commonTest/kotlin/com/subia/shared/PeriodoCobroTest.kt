package com.subia.shared

import com.subia.shared.model.PeriodoCobro
import com.subia.shared.model.Subscription
import com.subia.shared.model.periodoCobro
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Interpretación del periodo de cobro que usa la UI para sus textos ("al mes", "cada 3 meses",
 * "al año", "a la semana"). Debe seguir las mismas reglas que los cálculos de dinero.
 */
class PeriodoCobroTest {

    private fun sub(periodo: String, intervalo: Int) = Subscription(
        nombre = "X", precio = 10.0, moneda = "EUR", periodoFacturacion = periodo,
        fechaRenovacion = "2026-10-10", intervaloMeses = intervalo
    )

    @Test
    fun mensualYSusIntervalos_sonPorMeses() {
        assertEquals(PeriodoCobro.PorMeses(1), PeriodoCobro.de("MONTHLY", 1))
        assertEquals(PeriodoCobro.PorMeses(2), PeriodoCobro.de("MONTHLY", 2))
        assertEquals(PeriodoCobro.PorMeses(3), PeriodoCobro.de("MONTHLY", 3))
        assertEquals(PeriodoCobro.PorMeses(6), PeriodoCobro.de("MONTHLY", 6))
        assertEquals(PeriodoCobro.PorMeses(12), PeriodoCobro.de("YEARLY", 12))
    }

    @Test
    fun anualAntiguoConIntervalo1_esCada12Meses() {
        // Caché o backend de antes del campo: el 1 por defecto no significa "mensual".
        assertEquals(PeriodoCobro.PorMeses(12), PeriodoCobro.de("YEARLY", 1))
        assertEquals(PeriodoCobro.PorMeses(12), PeriodoCobro.de("annual", 1))
    }

    @Test
    fun trimestralAntiguo_esCada3Meses() {
        assertEquals(PeriodoCobro.PorMeses(3), PeriodoCobro.de("QUARTERLY", 1))
    }

    @Test
    fun intervaloCorrupto_caeAlNaturalDelCiclo() {
        assertEquals(PeriodoCobro.PorMeses(1), PeriodoCobro.de("MONTHLY", 5))
        assertEquals(PeriodoCobro.PorMeses(1), PeriodoCobro.de("MONTHLY", 0))
        assertEquals(PeriodoCobro.PorMeses(12), PeriodoCobro.de("YEARLY", 7))
    }

    @Test
    fun ciclosPorSemanasODias_ignoranElIntervalo() {
        assertEquals(PeriodoCobro.Semanal, PeriodoCobro.de("WEEKLY", 1))
        assertEquals(PeriodoCobro.Semanal, PeriodoCobro.de(" weekly ", 3))
        assertEquals(PeriodoCobro.Quincenal, PeriodoCobro.de("BIWEEKLY", 1))
        assertEquals(PeriodoCobro.Diario, PeriodoCobro.de("DAILY", 1))
    }

    @Test
    fun esMensual_soloParaUnCobroAlMes() {
        assertTrue(PeriodoCobro.PorMeses(1).esMensual)
        assertFalse(PeriodoCobro.PorMeses(3).esMensual)
        assertFalse(PeriodoCobro.PorMeses(12).esMensual)
        assertFalse(PeriodoCobro.Semanal.esMensual)
    }

    @Test
    fun extensionDeSubscription_usaCicloEIntervalo() {
        assertEquals(PeriodoCobro.PorMeses(3), sub("MONTHLY", 3).periodoCobro())
        assertEquals(PeriodoCobro.PorMeses(12), sub("YEARLY", 1).periodoCobro())
        assertEquals(PeriodoCobro.Semanal, sub("WEEKLY", 1).periodoCobro())
    }
}
