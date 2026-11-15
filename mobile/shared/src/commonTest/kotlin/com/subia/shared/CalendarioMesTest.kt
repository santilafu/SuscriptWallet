package com.subia.shared

import com.subia.shared.model.CobroPrevisto
import com.subia.shared.model.CuadriculaMes
import com.subia.shared.model.DiaCobros
import com.subia.shared.model.PeriodoCobro
import com.subia.shared.model.Subscription
import com.subia.shared.model.calcularProyeccionCobros
import com.subia.shared.model.diaPorDefectoCalendario
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Rejilla del calendario "Este mes" (semanas de lunes a domingo) y día desplegado por defecto. */
class CalendarioMesTest {

    @Test
    fun febreroDe28QueEmpiezaEnLunes_ocupaCuatroFilasSinHuecos() {
        // 1 de febrero de 2021: lunes.
        val c = CuadriculaMes.de(2021, 2)
        assertEquals(0, c.huecosIniciales)
        assertEquals(28, c.diasDelMes)
        assertEquals(4, c.filas)
        assertEquals(0, c.huecosFinales)
    }

    @Test
    fun febreroDe28QueEmpiezaEnDomingo_ocupaCincoFilas() {
        // 1 de febrero de 2026: domingo → seis huecos delante.
        val c = CuadriculaMes.de(2026, 2)
        assertEquals(6, c.huecosIniciales)
        assertEquals(28, c.diasDelMes)
        assertEquals(5, c.filas)
        assertEquals(1, c.huecosFinales)
    }

    @Test
    fun febreroBisiesto_tiene29Dias() {
        // 2028 es bisiesto; el 1 de febrero es martes.
        val c = CuadriculaMes.de(2028, 2)
        assertEquals(1, c.huecosIniciales)
        assertEquals(29, c.diasDelMes)
        assertEquals(5, c.filas)
        assertEquals(5, c.huecosFinales)
        assertEquals(listOf(null, 1, 2, 3, 4, 5, 6), c.semanas().first())
        assertEquals(listOf(28, 29, null, null, null, null, null), c.semanas().last())
    }

    @Test
    fun mesDe31QueEmpiezaEnDomingo_ocupaSeisFilas() {
        // 1 de marzo de 2026: domingo.
        val c = CuadriculaMes.de(2026, 3)
        assertEquals(6, c.huecosIniciales)
        assertEquals(31, c.diasDelMes)
        assertEquals(6, c.filas)
        assertEquals(5, c.huecosFinales)
        val semanas = c.semanas()
        assertEquals(listOf(null, null, null, null, null, null, 1), semanas.first())
        assertEquals(listOf(30, 31, null, null, null, null, null), semanas.last())
    }

    @Test
    fun mesDe30QueEmpiezaEnDomingo_ocupaSeisFilas() {
        // Noviembre de 2026: 30 días y empieza en domingo.
        val c = CuadriculaMes.de(LocalDate(2026, 11, 15))
        assertEquals(6, c.huecosIniciales)
        assertEquals(30, c.diasDelMes)
        assertEquals(6, c.filas)
    }

    @Test
    fun mesDe31QueEmpiezaEnMartes_ocupaCincoFilas() {
        // 1 de diciembre de 2026: martes.
        val c = CuadriculaMes.de(2026, 12)
        assertEquals(1, c.huecosIniciales)
        assertEquals(31, c.diasDelMes)
        assertEquals(5, c.filas)
        assertEquals(3, c.huecosFinales)
    }

    @Test
    fun semanas_tienenSieteCeldasYCadaDiaUnaVezEnOrden() {
        for (mes in 1..12) {
            val c = CuadriculaMes.de(2026, mes)
            val semanas = c.semanas()
            assertEquals(c.filas, semanas.size)
            semanas.forEach { assertEquals(7, it.size) }
            assertEquals((1..c.diasDelMes).toList(), semanas.flatten().filterNotNull())
            assertEquals(c.huecosIniciales, semanas.first().takeWhile { it == null }.size)
            assertEquals(c.huecosFinales, semanas.last().takeLastWhile { it == null }.size)
        }
    }

    private fun cobro(fecha: LocalDate, nombre: String = "Netflix") = CobroPrevisto(
        suscripcionId = 1L, nombre = nombre, importe = 9.99, moneda = "EUR", fecha = fecha,
        categoriaId = null, periodoFacturacion = "MONTHLY"
    )

    private fun dias(vararg conCobro: Int): List<DiaCobros> = (1..30).map { d ->
        val fecha = LocalDate(2026, 11, d)
        DiaCobros(fecha, if (d in conCobro) listOf(cobro(fecha)) else emptyList())
    }

    @Test
    fun diaPorDefecto_esElProximoConCobrosDesdeHoy() {
        val hoy = LocalDate(2026, 11, 15)
        assertEquals(LocalDate(2026, 11, 20), diaPorDefectoCalendario(dias(3, 20, 28), hoy))
    }

    @Test
    fun diaPorDefecto_incluyeHoy() {
        val hoy = LocalDate(2026, 11, 15)
        assertEquals(hoy, diaPorDefectoCalendario(dias(3, 15, 28), hoy))
    }

    @Test
    fun diaPorDefecto_esNullSiYaNoQuedanCobros() {
        val hoy = LocalDate(2026, 11, 15)
        assertNull(diaPorDefectoCalendario(dias(3, 10), hoy))
        assertNull(diaPorDefectoCalendario(dias(), hoy))
    }

    @Test
    fun cobroDelMes_llevaElPeriodoDelGasto() {
        val trimestral = Subscription(
            id = 7L, nombre = "Seguro", precio = 90.0, moneda = "EUR", periodoFacturacion = "MONTHLY",
            fechaRenovacion = "2026-11-20", intervaloMeses = 3
        )
        val hoy = LocalDate(2026, 11, 15)
        val cobro = calcularProyeccionCobros(listOf(trimestral), hoy).diasMes.single { it.tieneCobros }.cobros.single()
        assertEquals(PeriodoCobro.PorMeses(3), cobro.periodo)
    }
}
