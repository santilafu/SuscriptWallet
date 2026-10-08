package com.subia.shared

import com.subia.shared.model.ProyeccionCobros
import com.subia.shared.model.Subscription
import com.subia.shared.model.calcularProyeccionCobros
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas del modelo puro de proyección de cobros ([calcularProyeccionCobros]). */
class ProyeccionCobrosTest {

    private val hoy = LocalDate(2026, 10, 7)

    private fun sub(
        id: Long = 1L,
        nombre: String = "Netflix",
        precio: Double = 12.99,
        moneda: String = "EUR",
        periodo: String = "MONTHLY",
        fecha: String = "2026-10-15",
        activa: Boolean = true,
        categoriaId: Long? = 1L
    ) = Subscription(
        id = id, nombre = nombre, precio = precio, moneda = moneda,
        periodoFacturacion = periodo, fechaRenovacion = fecha, categoriaId = categoriaId, activa = activa
    )

    @Test
    fun estructura_siempre_14_dias_y_12_meses_desde_hoy() {
        val p = calcularProyeccionCobros(emptyList(), hoy)
        assertEquals(ProyeccionCobros.DIAS_TIRA, p.dias.size)
        assertEquals(ProyeccionCobros.MESES_HORIZONTE, p.meses.size)
        assertEquals(hoy, p.dias.first().fecha)
        assertEquals(LocalDate(2026, 10, 20), p.dias.last().fecha)
        assertEquals(2026 to 10, p.meses.first().anio to p.meses.first().mes)
        assertEquals(2027 to 9, p.meses.last().anio to p.meses.last().mes)
        assertNull(p.proximoCobro)
        assertTrue(p.totalMesActual.isEmpty())
    }

    @Test
    fun mensual_cae_en_su_dia_de_la_tira_y_en_cada_uno_de_los_12_meses() {
        val p = calcularProyeccionCobros(listOf(sub(fecha = "2026-10-15")), hoy)

        val dia = p.dias[8] // 7 + 8 = 15 de octubre
        assertEquals(LocalDate(2026, 10, 15), dia.fecha)
        assertEquals(1, dia.cobros.size)
        assertEquals(12.99, dia.totalPorMoneda["EUR"]!!, 1e-9)
        assertEquals(13, p.dias.count { !it.tieneCobros })

        p.meses.forEach { mes ->
            assertEquals(1, mes.cobros.size, "Mes ${mes.anio}-${mes.mes} debería tener 1 cobro")
            assertEquals(15, mes.cobros.single().fecha.dayOfMonth)
        }
        assertEquals(12.99, p.totalMesActual["EUR"]!!, 1e-9)
        assertEquals(12.99, p.totalMesSiguiente["EUR"]!!, 1e-9)
        assertEquals("Netflix", p.proximoCobro?.nombre)
    }

    @Test
    fun anual_cae_solo_en_su_mes_y_no_aparece_en_la_tira() {
        val p = calcularProyeccionCobros(
            listOf(sub(id = 2, nombre = "Seguro coche", precio = 300.0, periodo = "YEARLY", fecha = "2026-12-05")),
            hoy
        )
        assertTrue(p.dias.none { it.tieneCobros })
        val diciembre = p.meses[2]
        assertEquals(12, diciembre.mes)
        assertEquals(300.0, diciembre.total("EUR"), 1e-9)
        assertEquals(1, p.meses.count { it.cobros.isNotEmpty() })
        assertTrue(p.totalMesActual.isEmpty())
        assertTrue(p.totalMesSiguiente.isEmpty())
    }

    @Test
    fun anual_con_fecha_pasada_avanza_al_siguiente_aniversario() {
        val p = calcularProyeccionCobros(
            listOf(sub(periodo = "YEARLY", precio = 99.0, fecha = "2024-11-20")),
            hoy
        )
        val noviembre = p.meses[1]
        assertEquals(11, noviembre.mes)
        assertEquals(LocalDate(2026, 11, 20), noviembre.cobros.single().fecha)
        assertEquals(1, p.meses.count { it.cobros.isNotEmpty() })
    }

    @Test
    fun varias_divisas_se_suman_por_separado_con_EUR_primero() {
        val p = calcularProyeccionCobros(
            listOf(
                sub(id = 1, nombre = "Netflix", precio = 10.0, moneda = "EUR", fecha = "2026-10-09"),
                sub(id = 2, nombre = "iCloud", precio = 3.0, moneda = "USD", fecha = "2026-10-09"),
                sub(id = 3, nombre = "Spotify", precio = 5.0, moneda = "EUR", fecha = "2026-10-09")
            ),
            hoy
        )
        val dia = p.dias[2]
        assertEquals(listOf("EUR", "USD"), dia.totalPorMoneda.keys.toList())
        assertEquals(15.0, dia.totalPorMoneda["EUR"]!!, 1e-9)
        assertEquals(3.0, dia.totalPorMoneda["USD"]!!, 1e-9)
        assertEquals(15.0, p.totalMesActual["EUR"]!!, 1e-9)
        assertEquals(3.0, p.totalMesActual["USD"]!!, 1e-9)
        // Dentro del día, el importe mayor va primero (logo más grande arriba en la tira).
        assertEquals(listOf("Netflix", "Spotify", "iCloud"), dia.cobros.map { it.nombre })
    }

    @Test
    fun mensual_con_fecha_pasada_se_avanza_al_siguiente_ciclo_sin_cobros_anteriores_a_hoy() {
        val p = calcularProyeccionCobros(listOf(sub(fecha = "2024-01-20")), hoy)
        val primero = p.cobrosTira.first()
        assertEquals(LocalDate(2026, 10, 20), primero.fecha)
        assertTrue(p.meses.all { it.cobros.size == 1 })
        assertTrue(p.meses.flatMap { it.cobros }.all { it.fecha >= hoy })
    }

    @Test
    fun fecha_de_hoy_cuenta_como_cobro_de_hoy() {
        val p = calcularProyeccionCobros(listOf(sub(fecha = "2026-10-07")), hoy)
        assertEquals(1, p.dias[0].cobros.size)
        assertEquals(hoy, p.proximoCobro?.fecha)
    }

    @Test
    fun ancla_dia_31_cae_el_30_en_meses_cortos_y_vuelve_al_31_en_los_largos() {
        val hoyMarzo = LocalDate(2026, 3, 15)
        val p = calcularProyeccionCobros(listOf(sub(fecha = "2026-01-31", precio = 60.0)), hoyMarzo)
        val fechas = p.meses.map { it.cobros.single().fecha }
        assertEquals(LocalDate(2026, 3, 31), fechas[0])
        assertEquals(LocalDate(2026, 4, 30), fechas[1])
        assertEquals(LocalDate(2026, 5, 31), fechas[2])
        assertEquals(LocalDate(2026, 6, 30), fechas[3])
        assertEquals(LocalDate(2027, 2, 28), fechas[11])
        assertEquals(12, fechas.size)
    }

    @Test
    fun semanal_aparece_dos_veces_en_la_tira_de_14_dias() {
        val p = calcularProyeccionCobros(listOf(sub(periodo = "WEEKLY", precio = 2.0, fecha = "2026-10-07")), hoy)
        assertEquals(listOf(0, 7), p.dias.withIndex().filter { it.value.tieneCobros }.map { it.index })
        // Octubre completo: 7, 14, 21, 28 → 4 cobros
        assertEquals(4, p.meses[0].cobros.size)
        assertEquals(8.0, p.totalMesActual["EUR"]!!, 1e-9)
    }

    @Test
    fun inactivas_y_fechas_invalidas_se_ignoran() {
        val p = calcularProyeccionCobros(
            listOf(
                sub(id = 1, activa = false, fecha = "2026-10-08"),
                sub(id = 2, fecha = "no-es-fecha"),
                sub(id = 3, fecha = "")
            ),
            hoy
        )
        assertTrue(p.cobrosTira.isEmpty())
        assertTrue(p.meses.all { it.cobros.isEmpty() })
    }

    @Test
    fun agrupacion_por_categoria_del_mes_en_curso_ordenada_de_mayor_a_menor() {
        val p = calcularProyeccionCobros(
            listOf(
                sub(id = 1, nombre = "Netflix", precio = 12.99, categoriaId = 1L, fecha = "2026-10-10"),
                sub(id = 2, nombre = "HBO", precio = 9.99, categoriaId = 1L, fecha = "2026-10-12"),
                sub(id = 3, nombre = "Luz", precio = 60.0, categoriaId = 2L, fecha = "2026-10-25"),
                sub(id = 4, nombre = "Seguro", precio = 300.0, periodo = "YEARLY", categoriaId = 3L, fecha = "2026-12-01"),
                sub(id = 5, nombre = "Sin cat", precio = 1.0, categoriaId = null, fecha = "2026-10-30")
            ),
            hoy
        )
        val cats = p.porCategoriaMesActual
        assertEquals(listOf(2L, 1L, null), cats.map { it.categoriaId })
        assertEquals(60.0, cats[0].total, 1e-9)
        assertEquals(22.98, cats[1].total, 1e-9)
        assertEquals(2, cats[1].numCobros)
        // El seguro anual de diciembre no entra en el mes en curso.
        assertTrue(cats.none { it.categoriaId == 3L })
        assertEquals(83.98, p.totalMesActual["EUR"]!!, 1e-9)
        assertEquals(300.0 + 12.99 + 9.99 + 60.0 + 1.0, p.meses[2].total("EUR"), 1e-9)
    }
}
