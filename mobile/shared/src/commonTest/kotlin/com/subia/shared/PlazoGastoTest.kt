package com.subia.shared

import com.subia.shared.model.NuevaSuscripcionRequest
import com.subia.shared.model.Subscription
import com.subia.shared.model.VistaCategorias
import com.subia.shared.model.avisosPendientes
import com.subia.shared.model.CuotaPrestamo
import com.subia.shared.model.calcularCuotaPrestamo
import com.subia.shared.model.calcularProyeccionCobros
import com.subia.shared.model.cobrosDelMes
import com.subia.shared.model.cuentaEnTotales
import com.subia.shared.model.gastosPorCategoria
import com.subia.shared.model.plazoGasto
import com.subia.shared.model.proximaRenovacion
import com.subia.shared.model.proximaRenovacionPrestamo
import com.subia.shared.model.terminado
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Préstamos y financiaciones: plazo de un gasto con fecha de inicio y de fin ([plazoGasto],
 * [terminado], recorte de la serie de cobros). Misma tabla de casos que `PlazoGastoTest` del
 * backend (docs/specs/2026-11-24-prestamos-fase0.md). "Hoy" en todos los casos: 2026-10-10.
 */
class PlazoGastoTest {

    private fun d(s: String) = LocalDate.parse(s)
    private val hoy = d("2026-10-10")

    private fun sub(
        ancla: String,
        inicio: String? = null,
        fin: String? = null,
        precio: Double = 100.0,
        periodo: String = "MONTHLY",
        intervalo: Int = 1,
        id: Long = 1L,
        activa: Boolean = true
    ) = Subscription(
        id = id, nombre = "Préstamo $id", precio = precio, periodoFacturacion = periodo,
        fechaRenovacion = ancla, fechaAncla = ancla, intervaloMeses = intervalo, activa = activa,
        categoriaId = 1L, fechaInicio = inicio, fechaFin = fin
    )

    private fun restantes(s: Subscription) = plazoGasto(s, hoy)?.cuotasRestantes

    // ── Tabla compartida con el backend ──────────────────────────────────────

    @Test
    fun cuotasRestantes_conIntervalosDe1_2_3_6y12Meses() {
        val casos = mapOf(1 to 15, 2 to 7, 3 to 5, 6 to 2, 12 to 1)
        casos.forEach { (intervalo, esperado) ->
            val periodo = if (intervalo == 12) "YEARLY" else "MONTHLY"
            val s = sub("2026-01-15", fin = "2027-12-31", periodo = periodo, intervalo = intervalo)
            assertEquals(esperado, restantes(s), "intervalo $intervalo")
        }
    }

    @Test
    fun semanal_cuentaCada7Dias() {
        // 2026-10-13, 20, 27, 11-03, 10, 17, 24 → 7
        assertEquals(7, restantes(sub("2026-10-13", fin = "2026-11-25", periodo = "WEEKLY")))
    }

    @Test
    fun dia31_caeElUltimoDiaDeLosMesesCortos() {
        // 31-oct, 30-nov, 31-dic, 31-ene, 28-feb
        assertEquals(5, restantes(sub("2026-01-31", fin = "2027-03-01")))
    }

    @Test
    fun elCobroDeHoy_cuentaComoPendiente() {
        val s = sub("2026-10-10", fin = "2026-11-10")
        assertEquals(2, restantes(s))
        assertFalse(s.terminado(hoy))
    }

    @Test
    fun renovacionGuardadaEnElFuturo_seCuentaDesdeElla_yPagadasSoloAntesDeHoy() {
        // Hoy 10-oct, próxima renovación guardada 15-dic, fin 15-mar: dic, ene, feb, mar → 4.
        val s = sub("2026-12-15", fin = "2027-03-15")
        assertEquals(4, restantes(s))
        assertEquals(d("2026-12-15"), proximaRenovacion(s, hoy))
        assertFalse(s.terminado(hoy))
        assertEquals(400.0, plazoGasto(s, hoy)!!.pendiente)
        // Con inicio pasado: pagadas jul, ago, sep = 3 (oct y nov no llegan) → barra 3/7, no 5/9.
        val conInicio = plazoGasto(sub("2026-12-15", inicio = "2026-07-15", fin = "2027-03-15"), hoy)!!
        assertEquals(4, conInicio.cuotasRestantes)
        assertEquals(7, conInicio.cuotasTotales)
        assertEquals(3, conInicio.cuotasPagadas)
        assertTrue(sub("2026-12-15", fin = "2026-11-15").terminado(hoy))
    }

    @Test
    fun pagadasSonLosCobrosAnterioresAHoy_renovacionGuardadaElMesQueViene() {
        // Hoy 24-nov, inicio 15-mar, renovación guardada 15-dic, fin 15-feb: mar..nov = 9 pagadas, 3 restantes.
        val plazo = plazoGasto(sub("2026-12-15", inicio = "2026-03-15", fin = "2027-02-15"), d("2026-11-24"))!!
        assertEquals(9, plazo.cuotasPagadas)
        assertEquals(3, plazo.cuotasRestantes)
        assertEquals(12, plazo.cuotasTotales)
    }

    @Test
    fun terminadoConInicio_lasPagadasSeCortanEnElFin() {
        val plazo = plazoGasto(sub("2025-01-15", inicio = "2025-01-15", fin = "2025-12-15"), hoy)!!
        assertEquals(0, plazo.cuotasRestantes)
        assertEquals(12, plazo.cuotasPagadas)
        assertEquals(12, plazo.cuotasTotales)
        assertEquals(1f, plazo.progreso)
    }

    @Test
    fun sinFechaDeFin_noHayPlazoNiTerminado() {
        val s = sub("2026-01-15", inicio = "2026-01-15")
        assertNull(plazoGasto(s, hoy))
        assertFalse(s.terminado(hoy))
    }

    @Test
    fun finAntesDelProximoCobro_ceroCuotasYTerminado() {
        val s = sub("2026-01-15", fin = "2026-10-12") // próximo cobro 15-oct > fin
        assertEquals(0, restantes(s))
        assertTrue(s.terminado(hoy))
        assertEquals(0.0, plazoGasto(s, hoy)!!.pendiente)
    }

    @Test
    fun finPasado_terminado() {
        assertTrue(sub("2025-01-15", fin = "2025-12-15").terminado(hoy))
    }

    @Test
    fun inicioFuturo_noHayCobrosAntesDelInicio() {
        val s = sub("2026-10-15", inicio = "2027-01-15", fin = "2027-03-15")
        val plazo = plazoGasto(s, hoy)!!
        assertEquals(3, plazo.cuotasRestantes)
        assertEquals(3, plazo.cuotasTotales)
        assertFalse(s.terminado(hoy))
        assertEquals(d("2027-01-15"), proximaRenovacion(s, hoy))
        // Sí cuenta en el gasto mensual medio (decisión del usuario).
        assertTrue(s.cuentaEnTotales(hoy))
    }

    @Test
    fun inicioFuturo_elInicioEsLaPrimeraCuota_yRenovacionTrasElFin_terminado() {
        // La serie va anclada en el inicio: el plazo [1-dic, 15-dic] tiene el cobro del 1-dic
        // aunque la renovación guardada sea un día 24 (antes de la regla: 0 cuotas y terminado).
        val s = sub("2026-10-24", inicio = "2026-12-01", fin = "2026-12-15")
        assertEquals(1, restantes(s))
        assertFalse(s.terminado(hoy))
        // Sin cobros dentro solo si la renovación guardada (cota inferior) cae después del fin.
        val tras = sub("2026-12-24", inicio = "2026-12-01", fin = "2026-12-15")
        assertEquals(0, restantes(tras))
        assertTrue(tras.terminado(hoy))
    }

    @Test
    fun totalesCuentanHaciaAtrasDesdeUnAnclaReciente_yLaBarraSaleDePagadasEntreTotales() {
        val plazo = plazoGasto(sub("2026-10-15", inicio = "2026-01-15", fin = "2026-12-15", precio = 80.5), hoy)!!
        assertEquals(3, plazo.cuotasRestantes)
        assertEquals(12, plazo.cuotasTotales)
        assertEquals(9, plazo.cuotasPagadas)
        assertEquals(0.75f, plazo.progreso)
        assertEquals(241.5, plazo.pendiente)
    }

    @Test
    fun sinInicio_noHayTotalesNiBarra() {
        val plazo = plazoGasto(sub("2026-01-15", fin = "2026-12-15"), hoy)!!
        assertEquals(3, plazo.cuotasRestantes)
        assertNull(plazo.cuotasTotales)
        assertNull(plazo.cuotasPagadas)
        assertNull(plazo.progreso)
    }

    @Test
    fun pendienteConImporteDe4Decimales_seRedondeaAlFinal() {
        // 3 × 33,3333 = 99,9999 → 100,00
        assertEquals(100.0, plazoGasto(sub("2026-01-15", fin = "2026-12-15", precio = 33.3333), hoy)!!.pendiente)
    }

    @Test
    fun alAnio_sumaMinimoEntreRestantesYCuotasPorAnio() {
        fun cobros(s: Subscription) =
            gastosPorCategoria(listOf(s), hoy, VistaCategorias.AL_ANIO).single().gastos.single()
        // 3 cuotas restantes de 100 € → 300 €, no 1 200 €.
        val corto = cobros(sub("2026-01-15", fin = "2026-12-15"))
        assertEquals(3, corto.numCobros)
        assertEquals(300.0, corto.importe)
        assertEquals(12, cobros(sub("2026-01-15", fin = "2030-12-15")).numCobros)
        assertEquals(4, cobros(sub("2026-01-15", fin = "2030-12-15", intervalo = 3)).numCobros)
        assertEquals(52, cobros(sub("2026-10-13", fin = "2030-12-15", periodo = "WEEKLY")).numCobros)
        assertEquals(4, cobros(sub("2026-12-15", fin = "2027-03-15")).numCobros)
        // Inicio dentro de 8 meses y fin dentro de 3 años: en los próximos 12 meses (hasta 9-oct-2027)
        // solo caen jun, jul, ago y sep → 4 cuotas (400 €), no 12.
        val futuro = cobros(sub("2026-10-15", inicio = "2027-06-15", fin = "2029-10-15"))
        assertEquals(4, futuro.numCobros)
        assertEquals(400.0, futuro.importe)
    }

    // ── Terminado fuera de totales, calendario y avisos ──────────────────────

    @Test
    fun terminado_noCuentaEnTotalesNiEnAlAnio() {
        val terminado = sub("2025-01-15", fin = "2025-12-15", id = 2)
        val vigente = sub("2026-01-15", id = 3)
        assertFalse(terminado.cuentaEnTotales(hoy))
        assertTrue(vigente.cuentaEnTotales(hoy))
        assertFalse(sub("2026-01-15", activa = false).cuentaEnTotales(hoy))
        val anio = gastosPorCategoria(listOf(terminado, vigente), hoy, VistaCategorias.AL_ANIO)
        assertEquals(listOf(3L), anio.single().gastos.map { it.suscripcionId })
    }

    @Test
    fun terminado_noSaleEnElCalendarioNiEnLaProyeccion() {
        val terminado = sub("2025-01-15", fin = "2025-12-15")
        val proyeccion = calcularProyeccionCobros(listOf(terminado), hoy)
        assertTrue(proyeccion.meses.all { it.cobros.isEmpty() })
        assertTrue(proyeccion.diasMes.none { it.tieneCobros })
        assertTrue(proyeccion.totalMesCompleto.isEmpty())
    }

    @Test
    fun proyeccion_seCortaEnLaFechaDeFin() {
        // 15-oct, 15-nov, 15-dic y nada más.
        val proyeccion = calcularProyeccionCobros(listOf(sub("2026-01-15", fin = "2026-12-20")), hoy)
        assertEquals(listOf(1, 1, 1, 0, 0), proyeccion.meses.take(5).map { it.cobros.size })
    }

    @Test
    fun proyeccion_noTieneCobrosAntesDelInicio() {
        val s = sub("2026-10-15", inicio = "2027-01-15")
        val meses = calcularProyeccionCobros(listOf(s), hoy).meses
        assertEquals(listOf(0, 0, 0, 1, 1), meses.take(5).map { it.cobros.size })
    }

    @Test
    fun ultimoCobroDeEsteMes_siguеEnElCalendarioDelMes() {
        // Terminó el 5-oct (último cobro). Hoy es 10-oct: ya está terminado, pero ese cobro se pagó este mes.
        val s = sub("2026-01-05", fin = "2026-10-05")
        assertTrue(s.terminado(hoy))
        val cobros = cobrosDelMes(listOf(s), hoy).flatMap { it.cobros }
        assertEquals(listOf(d("2026-10-05")), cobros.map { it.fecha })
    }

    @Test
    fun terminado_noAvisa() {
        val terminado = sub("2026-01-11", fin = "2026-10-09") // el próximo sería el 11-oct
        assertTrue(avisosPendientes(listOf(terminado), hoy, umbralDias = 3, yaAvisados = emptySet()).isEmpty())
        val vigente = sub("2026-01-11", fin = "2026-12-31")
        assertEquals(1, avisosPendientes(listOf(vigente), hoy, umbralDias = 3, yaAvisados = emptySet()).size)
    }

    @Test
    fun sinFechas_laSerieEsLaDeSiempre() {
        val s = sub("2026-01-15")
        assertEquals(12, calcularProyeccionCobros(listOf(s), hoy).meses.sumOf { it.cobros.size })
        assertEquals(d("2026-10-15"), proximaRenovacion(s, hoy))
    }

    @Test
    fun hipotecaLarga_noSeCortaPorLaGuardia() {
        // 40 años mensuales = 480 cuotas (la guardia antigua cortaba en 400).
        assertEquals(480, restantes(sub("2026-10-15", fin = "2066-09-15")))
    }

    // ── Serialización ────────────────────────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun request_siempreLlevaLasFechas_vaciasSiNoHay() {
        fun cuerpo(inicio: String, fin: String) = json.encodeToString(
            NuevaSuscripcionRequest.serializer(),
            NuevaSuscripcionRequest(
                nombre = "N", precio = 10.0, periodoFacturacion = "MONTHLY", fechaRenovacion = "2026-11-01",
                intervaloMeses = 1, fechaInicio = inicio, fechaFin = fin
            )
        )
        val vacio = cuerpo("", "")
        assertTrue("\"fechaInicio\":\"\"" in vacio, vacio)
        assertTrue("\"fechaFin\":\"\"" in vacio, vacio)
        val conFechas = cuerpo("2026-01-15", "2026-12-15")
        assertTrue("\"fechaInicio\":\"2026-01-15\"" in conFechas, conFechas)
        assertTrue("\"fechaFin\":\"2026-12-15\"" in conFechas, conFechas)
    }

    @Test
    fun jsonSinFechas_seLee_yConFechasTambien() {
        val antiguo = """{"id":1,"name":"N","price":10.0,"billingCycle":"MONTHLY","renewalDate":"2026-11-01"}"""
        val s = json.decodeFromString(Subscription.serializer(), antiguo)
        assertNull(s.fechaInicio)
        assertNull(s.fechaFin)
        val nuevo = """{"id":1,"name":"N","price":10.0,"billingCycle":"MONTHLY","renewalDate":"2026-11-01",
            |"fechaInicio":"2026-01-15","fechaFin":null}""".trimMargin()
        val conFechas = json.decodeFromString(Subscription.serializer(), nuevo)
        assertEquals("2026-01-15", conFechas.fechaInicio)
        assertNull(conFechas.fechaFin)
    }

    // ── Calculadora de préstamo (total → cuota) ─────────────────────────────

    private fun cuota(total: Double, inicio: String?, fin: String?, periodo: String = "MONTHLY", intervalo: Int = 1) =
        calcularCuotaPrestamo(total, periodo, intervalo, inicio?.let(::d), fin?.let(::d))

    @Test
    fun calculadora_divideElTotalEntreLasCuotasDelPlazo() {
        assertEquals(CuotaPrestamo(12, 600_000, exacta = true), cuota(720.0, "2026-03-15", "2027-02-15"))
        // 1000 / 12 = 83,333… → 83,33; 83,33 × 12 = 999,96: la última cuota varía.
        assertEquals(CuotaPrestamo(12, 833_300, exacta = false), cuota(1000.0, "2026-01-31", "2026-12-31"))
        // Del 31 de enero: la segunda cuota cae el 28 de febrero (fin de mes).
        assertEquals(CuotaPrestamo(2, 500_000, exacta = true), cuota(100.0, "2026-01-31", "2026-02-28"))
        assertEquals(CuotaPrestamo(1, 5_000_000, exacta = true), cuota(500.0, "2026-05-10", "2026-05-10"))
        assertEquals(CuotaPrestamo(4, 3_000_000, exacta = true), cuota(1200.0, "2026-01-10", "2026-12-10", intervalo = 3))
        // Semanal: cada 7 días, 4 cobros del 1 al 22.
        assertEquals(4, cuota(100.0, "2026-01-01", "2026-01-22", periodo = "WEEKLY")?.cuotas)
    }

    @Test
    fun calculadora_sinDatosValidos_noCalcula() {
        assertNull(cuota(720.0, "2027-02-15", "2026-03-15"), "fin < inicio")
        assertNull(cuota(0.0, "2026-03-15", "2027-02-15"), "total 0")
        assertNull(cuota(-5.0, "2026-03-15", "2027-02-15"), "total negativo")
        assertNull(cuota(720.0, null, "2027-02-15"), "sin inicio")
        assertNull(cuota(720.0, "2026-03-15", null), "sin fin")
    }

    @Test
    fun calculadora_renovacion_primerCobroDesdeHoy_yLaBarraCuadra() {
        val hoy = d("2026-11-25")
        val renovacion = proximaRenovacionPrestamo("MONTHLY", 1, d("2026-03-15"), d("2027-02-15"), hoy)
        assertEquals(d("2026-12-15"), renovacion)
        val plazo = plazoGasto(sub(renovacion.toString(), inicio = "2026-03-15", fin = "2027-02-15", precio = 60.0), hoy)!!
        assertEquals(12, plazo.cuotasTotales)
        assertEquals(9, plazo.cuotasPagadas)
        // Préstamo ya terminado: todos los cobros antes de hoy → la fecha de fin.
        assertEquals(d("2025-06-10"), proximaRenovacionPrestamo("MONTHLY", 1, d("2025-01-10"), d("2025-06-10"), hoy))
    }

    // ── Con fecha de inicio la serie va anclada en el inicio (casos del backend) ──

    private fun comprobar(s: Subscription, hoy: String, proximo: String, restantes: Int, pagadas: Int) {
        val p = plazoGasto(s, d(hoy))!!
        assertEquals(d(proximo), proximaRenovacion(s, d(hoy)))
        assertEquals(restantes, p.cuotasRestantes)
        assertEquals(pagadas, p.cuotasPagadas)
        assertEquals(12, p.cuotasTotales)
    }

    @Test
    fun conInicio_elDia31SeRespeta_aunqueLaRenovacionGuardadaSeaEl30() {
        val s = sub("2026-11-30", inicio = "2026-01-31", fin = "2026-12-31")
        comprobar(s, hoy = "2026-11-25", proximo = "2026-11-30", restantes = 2, pagadas = 10) // A
        comprobar(s, hoy = "2026-12-01", proximo = "2026-12-31", restantes = 1, pagadas = 11) // B
        val diciembre = calcularProyeccionCobros(listOf(s), d("2026-12-01")).meses[0].cobros.map { it.fecha }
        assertEquals(listOf(d("2026-12-31")), diciembre)
    }

    @Test
    fun conInicio_laRenovacionGuardadaSoloEsCotaInferior() {
        // C: renovación en la serie y en el futuro (Vitaldent).
        comprobar(sub("2026-12-15", inicio = "2026-03-15", fin = "2027-02-15"),
            hoy = "2026-11-24", proximo = "2026-12-15", restantes = 3, pagadas = 9)
        // D: renovación fuera de la serie (por defecto hoy): manda el día del inicio.
        comprobar(sub("2026-11-25", inicio = "2026-03-15", fin = "2027-02-15"),
            hoy = "2026-11-25", proximo = "2026-12-15", restantes = 3, pagadas = 9)
    }
}
