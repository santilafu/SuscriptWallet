package com.subia.shared

import com.subia.shared.model.Subscription
import com.subia.shared.model.gastoMensualEquivalente
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [gastoMensualEquivalente] es el único criterio de "gasto al mes" del Inicio, la lista y el
 * resumen anual. La lista sumaba una semanal como si fuera mensual (5 € en vez de 21,67 €).
 */
class GastoMensualTest {

    private fun sub(precio: Double, periodo: String) = Subscription(
        nombre = "X", precio = precio, periodoFacturacion = periodo, fechaRenovacion = "2026-10-10"
    )

    @Test
    fun mensual_es_el_mismo_importe() {
        assertEquals(9.99, sub(9.99, "MONTHLY").gastoMensualEquivalente(), 0.0001)
    }

    @Test
    fun anual_se_divide_entre_doce() {
        assertEquals(10.0, sub(120.0, "YEARLY").gastoMensualEquivalente(), 0.0001)
    }

    @Test
    fun semanal_se_multiplica_por_52_y_divide_entre_12() {
        assertEquals(5.0 * 52 / 12, sub(5.0, "WEEKLY").gastoMensualEquivalente(), 0.0001)
    }

    @Test
    fun ciclo_en_minusculas_o_con_espacios_se_reconoce() {
        assertEquals(10.0, sub(120.0, " yearly ").gastoMensualEquivalente(), 0.0001)
    }

    @Test
    fun ciclo_desconocido_se_trata_como_mensual() {
        assertEquals(7.0, sub(7.0, "RARO").gastoMensualEquivalente(), 0.0001)
    }
}
