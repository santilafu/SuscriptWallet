package com.subia.shared

import com.subia.shared.model.PeriodoCobro
import com.subia.shared.model.Subscription
import com.subia.shared.model.VistaCategorias
import com.subia.shared.model.calcularProyeccionCobros
import com.subia.shared.model.gastosPorCategoria
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * "Gasto por categoría" del Inicio con sus dos vistas ([gastosPorCategoria]):
 * - [VistaCategorias.ESTE_MES]: lo que se cobra DE VERDAD en el mes natural de hoy (del 1 al
 *   último, también los días ya pasados), igual que el calendario y "Este mes pagas".
 * - [VistaCategorias.AL_ANIO]: total al año (importe × 12 / intervalo; semanal × 52).
 *
 * Hoy es el 15 de noviembre de 2026 (mes de 30 días, el día 1 es domingo).
 */
class GastosPorCategoriaTest {

    private val hoy = LocalDate.parse("2026-11-15")

    private fun sub(
        ancla: String,
        precio: Double,
        intervalo: Int = 1,
        periodo: String = "MONTHLY",
        categoriaId: Long? = 1L,
        id: Long = 1L,
        moneda: String = "EUR",
        activa: Boolean = true
    ) = Subscription(
        id = id, nombre = "S$id", precio = precio, moneda = moneda, periodoFacturacion = periodo,
        fechaRenovacion = ancla, fechaAncla = ancla, intervaloMeses = intervalo, activa = activa,
        categoriaId = categoriaId
    )

    private fun mes(subs: List<Subscription>, nombres: Map<Long, String> = emptyMap()) =
        gastosPorCategoria(subs, hoy, VistaCategorias.ESTE_MES, nombres)

    private fun anio(subs: List<Subscription>, nombres: Map<Long, String> = emptyMap()) =
        gastosPorCategoria(subs, hoy, VistaCategorias.AL_ANIO, nombres)

    // ── Este mes ─────────────────────────────────────────────────────────────

    @Test
    fun esteMes_anualQueCaeEsteMes_cuentaEntero() {
        val anual = sub("2025-11-20", precio = 120.0, intervalo = 12, periodo = "YEARLY")
        val categoria = mes(listOf(anual)).single()
        assertEquals(120.0, categoria.total)
        assertEquals(100, categoria.porcentaje)
        assertEquals(1, categoria.gastos.single().numCobros)
        assertEquals(PeriodoCobro.PorMeses(12), categoria.gastos.single().periodo)
    }

    @Test
    fun esteMes_anualQueNoCaeEsteMes_noAparece() {
        val anual = sub("2026-03-10", precio = 120.0, intervalo = 12, periodo = "YEARLY")
        assertTrue(mes(listOf(anual)).isEmpty())
    }

    @Test
    fun esteMes_cadaDosMeses_soloSiTocaEsteMes() {
        val toca = sub("2026-09-05", precio = 60.0, intervalo = 2, categoriaId = 1, id = 1)
        val noToca = sub("2026-10-05", precio = 60.0, intervalo = 2, categoriaId = 2, id = 2)
        val resultado = mes(listOf(toca, noToca))
        assertEquals(listOf(1L), resultado.map { it.categoriaId })
        assertEquals(60.0, resultado.single().total) // el cobro entero, no la media de 30 €
    }

    @Test
    fun esteMes_semanalConCuatroCobros() {
        // Martes: 3, 10, 17 y 24 de noviembre (el 3 y el 10 ya han pasado y también cuentan).
        val semanal = sub("2026-10-06", precio = 5.0, periodo = "WEEKLY")
        val gasto = mes(listOf(semanal)).single().gastos.single()
        assertEquals(4, gasto.numCobros)
        assertEquals(20.0, gasto.importe)
        assertEquals(5.0, gasto.precio)
        assertEquals(PeriodoCobro.Semanal, gasto.periodo)
    }

    @Test
    fun esteMes_semanalConCincoCobros() {
        // Lunes: 2, 9, 16, 23 y 30 de noviembre.
        val semanal = sub("2026-11-02", precio = 5.0, periodo = "WEEKLY")
        val categoria = mes(listOf(semanal)).single()
        assertEquals(5, categoria.gastos.single().numCobros)
        assertEquals(25.0, categoria.total)
    }

    @Test
    fun esteMes_categoriaSinCobrosDesaparece_yLosPorcentajesSonSobreLoDelMes() {
        val subs = listOf(
            sub("2026-11-01", precio = 30.0, categoriaId = 1, id = 1),               // mensual, día ya pasado
            sub("2026-11-20", precio = 10.0, categoriaId = 2, id = 2),               // mensual
            sub("2026-02-01", precio = 500.0, intervalo = 12, periodo = "YEARLY", categoriaId = 3, id = 3) // no cae
        )
        val resultado = mes(subs, mapOf(1L to "Hogar", 2L to "Música", 3L to "Seguros"))
        assertEquals(listOf("Hogar", "Música"), resultado.map { it.nombre })
        assertEquals(listOf(30.0, 10.0), resultado.map { it.total })
        assertEquals(listOf(75, 25), resultado.map { it.porcentaje })
    }

    @Test
    fun esteMes_sumaDeCategoriasCuadraConEsteMesPagas() {
        val subs = listOf(
            sub("2026-11-02", precio = 4.99, periodo = "WEEKLY", categoriaId = 1, id = 1),
            sub("2026-09-05", precio = 60.0, intervalo = 2, categoriaId = 2, id = 2),
            sub("2025-11-20", precio = 120.0, intervalo = 12, periodo = "YEARLY", categoriaId = null, id = 3),
            sub("2026-11-10", precio = 9.99, categoriaId = 1, id = 4)
        )
        val total = mes(subs).sumOf { it.total }
        assertEquals(calcularProyeccionCobros(subs, hoy).totalMesCompleto["EUR"]!!, total, 0.001)
    }

    @Test
    fun esteMes_ignoraLasInactivas() {
        val inactiva = sub("2026-11-20", precio = 10.0, activa = false)
        assertTrue(mes(listOf(inactiva)).isEmpty())
    }

    // ── Al año ───────────────────────────────────────────────────────────────

    @Test
    fun alAnio_totalConMezclaDePeriodicidades() {
        val subs = listOf(
            sub("2026-11-01", precio = 395.0, categoriaId = 1, id = 1),                                  // 4.740
            sub("2026-09-05", precio = 60.0, intervalo = 2, categoriaId = 1, id = 2),                    // 360
            sub("2026-03-10", precio = 120.0, intervalo = 12, periodo = "YEARLY", categoriaId = 2, id = 3), // 120
            sub("2026-11-02", precio = 5.0, periodo = "WEEKLY", categoriaId = 2, id = 4),                // 5 × 52 = 260
            sub("2026-11-02", precio = 90.0, intervalo = 3, categoriaId = null, id = 5)                  // 360
        )
        val resultado = anio(subs, mapOf(1L to "Hogar", 2L to "Ocio"))
        assertEquals(listOf(1L, 2L, null), resultado.map { it.categoriaId })
        assertEquals(listOf(5100.0, 380.0, 360.0), resultado.map { it.total })
        assertEquals(listOf(87, 7, 6), resultado.map { it.porcentaje })
        val hogar = resultado.first()
        assertEquals(listOf(1L, 2L), hogar.gastos.map { it.suscripcionId })
        assertEquals(listOf(4740.0, 360.0), hogar.gastos.map { it.importe })
        assertEquals(listOf(12, 6), hogar.gastos.map { it.numCobros })
        assertEquals(listOf(260.0, 120.0), resultado[1].gastos.map { it.importe })
        assertEquals(listOf(52, 1), resultado[1].gastos.map { it.numCobros })
        assertEquals(2, hogar.numSuscripciones)
    }

    @Test
    fun alAnio_anualQueNoCaeEsteMes_siCuenta() {
        val anual = sub("2026-03-10", precio = 120.0, intervalo = 12, periodo = "YEARLY")
        assertEquals(listOf(120.0), anio(listOf(anual)).map { it.total })
    }

    @Test
    fun alAnio_sumaEnDiezmilesimas_sinPerderCentimos() {
        // Tres trimestrales de 10,00 €: 40,00 € cada uno al año, 120,00 € en total.
        val subs = List(3) { sub("2026-11-02", precio = 10.0, intervalo = 3, id = it.toLong()) }
        assertEquals(120.0, anio(subs).single().total)
    }

    // ── Comunes ──────────────────────────────────────────────────────────────

    @Test
    fun todoVacio_noDaCategorias() {
        assertTrue(mes(emptyList()).isEmpty())
        assertTrue(anio(emptyList()).isEmpty())
    }

    @Test
    fun variasDivisas_porcentajeDentroDeCadaDivisa_yEurPrimero() {
        val subs = listOf(
            sub("2026-11-20", precio = 10.0, moneda = "USD", categoriaId = 1, id = 1),
            sub("2026-11-20", precio = 5.0, moneda = "EUR", categoriaId = 1, id = 2)
        )
        val resultado = mes(subs)
        assertEquals(listOf("EUR", "USD"), resultado.map { it.moneda })
        assertEquals(listOf(100, 100), resultado.map { it.porcentaje })
    }

    @Test
    fun sinCategoria_vaConNombreVacio_yConCategoriaDesconocida_usaElGenerico() {
        val subs = listOf(
            sub("2026-11-20", precio = 10.0, categoriaId = null, id = 1),
            sub("2026-11-20", precio = 20.0, categoriaId = 9, id = 2)
        )
        val resultado = anio(subs)
        assertEquals(listOf("Categoría 9", ""), resultado.map { it.nombre })
    }
}
