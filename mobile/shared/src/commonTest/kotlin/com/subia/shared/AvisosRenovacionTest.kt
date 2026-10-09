package com.subia.shared

import com.subia.shared.model.Subscription
import com.subia.shared.model.TipoAviso
import com.subia.shared.model.avisosPendientes
import com.subia.shared.model.claveAviso
import com.subia.shared.model.esHoraDeSilencio
import com.subia.shared.model.idNotificacion
import com.subia.shared.model.minutosHasta
import com.subia.shared.model.purgarAvisosAntiguos
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Pruebas de la decisión pura "qué avisos tocan hoy" ([avisosPendientes]). */
class AvisosRenovacionTest {

    private fun sub(
        id: Long = 1L,
        nombre: String = "Netflix",
        periodo: String = "MONTHLY",
        fecha: String = "2026-10-15",
        activa: Boolean = true,
        esPrueba: Boolean = false,
        finPrueba: String? = null
    ) = Subscription(
        id = id, nombre = nombre, precio = 12.99, periodoFacturacion = periodo,
        fechaRenovacion = fecha, activa = activa, esPrueba = esPrueba, fechaFinPrueba = finPrueba
    )

    private fun d(iso: String) = LocalDate.parse(iso)

    @Test
    fun renovacionPasada_avanzaAlCicloActual() {
        // Dada de alta en mayo: el backend nunca avanza la fecha y antes no avisaba nunca más.
        val avisos = avisosPendientes(listOf(sub(fecha = "2026-05-15")), d("2026-10-12"), 3, emptySet())
        assertEquals(1, avisos.size)
        assertEquals(TipoAviso.COBRO, avisos[0].tipo)
        assertEquals(d("2026-10-15"), avisos[0].fecha)
        assertEquals(3, avisos[0].diasRestantes)
    }

    @Test
    fun anualPasado_avisaDelSegundoAnio() {
        val avisos = avisosPendientes(listOf(sub(periodo = "YEARLY", fecha = "2025-10-20")), d("2026-10-18"), 3, emptySet())
        assertEquals(d("2026-10-20"), avisos.single().fecha)
    }

    @Test
    fun dia31_caeEnElUltimoDiaDeFebrero() {
        val avisos = avisosPendientes(listOf(sub(fecha = "2026-01-31")), d("2027-02-25"), 3, emptySet())
        assertEquals(d("2027-02-28"), avisos.single().fecha)
    }

    @Test
    fun ventana_recuperaAvisoSiElTrabajoNoCorrioElDiaExacto() {
        // Umbral 3: el día "exacto" era el 12; el worker no corrió hasta el 14 → aún se avisa.
        val avisos = avisosPendientes(listOf(sub(fecha = "2026-10-15")), d("2026-10-14"), 3, emptySet())
        assertEquals(1, avisos.single().diasRestantes)
    }

    @Test
    fun cobroHoy_cuentaDentroDeLaVentana() {
        val avisos = avisosPendientes(listOf(sub(fecha = "2026-10-15")), d("2026-10-15"), 3, emptySet())
        assertEquals(0, avisos.single().diasRestantes)
    }

    @Test
    fun fueraDeLaVentana_noAvisa() {
        assertTrue(avisosPendientes(listOf(sub(fecha = "2026-10-15")), d("2026-10-11"), 3, emptySet()).isEmpty())
    }

    @Test
    fun yaAvisado_noSeDuplica() {
        val ya = setOf(claveAviso(1L, TipoAviso.COBRO, d("2026-10-15")))
        assertTrue(avisosPendientes(listOf(sub(fecha = "2026-10-15")), d("2026-10-13"), 3, ya).isEmpty())
    }

    @Test
    fun yaAvisadoElCicloAnterior_elSiguienteSiSeAvisa() {
        val ya = setOf(claveAviso(1L, TipoAviso.COBRO, d("2026-10-15")))
        val avisos = avisosPendientes(listOf(sub(fecha = "2026-10-15")), d("2026-11-13"), 3, ya)
        assertEquals(d("2026-11-15"), avisos.single().fecha)
    }

    @Test
    fun desactivada_noAvisa() {
        assertTrue(avisosPendientes(listOf(sub(activa = false)), d("2026-10-13"), 3, emptySet()).isEmpty())
    }

    @Test
    fun pruebaVigente_avisaDelFinDePrueba_noDelCobro() {
        val prueba = sub(fecha = "2026-10-14", esPrueba = true, finPrueba = "2026-10-14")
        val avisos = avisosPendientes(listOf(prueba), d("2026-10-12"), 3, emptySet())
        assertEquals(TipoAviso.FIN_PRUEBA, avisos.single().tipo)
        assertEquals(d("2026-10-14"), avisos.single().fecha)
    }

    @Test
    fun pruebaVigenteLejana_noAvisaDeNada() {
        val prueba = sub(fecha = "2026-10-13", esPrueba = true, finPrueba = "2026-10-30")
        assertTrue(avisosPendientes(listOf(prueba), d("2026-10-12"), 3, emptySet()).isEmpty())
    }

    @Test
    fun pruebaVencida_seTrataComoDePago() {
        // La prueba acabó en septiembre y nadie cambió isTrial: antes quedaba muda para siempre.
        val prueba = sub(fecha = "2026-09-01", esPrueba = true, finPrueba = "2026-09-01")
        val avisos = avisosPendientes(listOf(prueba), d("2026-10-30"), 3, emptySet())
        assertEquals(TipoAviso.COBRO, avisos.single().tipo)
        assertEquals(d("2026-11-01"), avisos.single().fecha)
    }

    @Test
    fun pruebaSinFechaDeFin_seTrataComoDePago() {
        val prueba = sub(fecha = "2026-10-15", esPrueba = true, finPrueba = null)
        assertEquals(TipoAviso.COBRO, avisosPendientes(listOf(prueba), d("2026-10-13"), 3, emptySet()).single().tipo)
    }

    @Test
    fun subirElUmbral_noPierdeElAviso() {
        // Cobro a 5 días: con 3 aún no toca; al pasar a 7 se avisa ya (antes se perdía).
        val s = listOf(sub(fecha = "2026-10-15"))
        assertTrue(avisosPendientes(s, d("2026-10-10"), 3, emptySet()).isEmpty())
        assertEquals(5, avisosPendientes(s, d("2026-10-10"), 7, emptySet()).single().diasRestantes)
    }

    @Test
    fun bajarElUmbral_noRepiteLoYaAvisado() {
        val s = listOf(sub(fecha = "2026-10-15"))
        val primero = avisosPendientes(s, d("2026-10-10"), 7, emptySet())
        val ya = primero.map { it.clave }.toSet()
        assertTrue(avisosPendientes(s, d("2026-10-13"), 3, ya).isEmpty())
    }

    @Test
    fun semanalConUmbralLargo_soloAvisaDelCobroMasProximo() {
        val avisos = avisosPendientes(listOf(sub(periodo = "WEEKLY", fecha = "2026-10-01")), d("2026-10-12"), 14, emptySet())
        assertEquals(d("2026-10-15"), avisos.single().fecha)
    }

    @Test
    fun fechaInvalida_seIgnora() {
        assertTrue(avisosPendientes(listOf(sub(fecha = "no-es-fecha")), d("2026-10-12"), 3, emptySet()).isEmpty())
    }

    @Test
    fun umbralNegativo_seTrataComoHoy() {
        val s = listOf(sub(id = 1, fecha = "2026-10-12"), sub(id = 2, fecha = "2026-10-13"))
        assertEquals(listOf(1L), avisosPendientes(s, d("2026-10-12"), -5, emptySet()).map { it.suscripcion.id })
    }

    @Test
    fun ordenPorFechaYNombre() {
        val s = listOf(
            sub(id = 1, nombre = "Spotify", fecha = "2026-10-14"),
            sub(id = 2, nombre = "Disney+", fecha = "2026-10-14"),
            sub(id = 3, nombre = "Agua", fecha = "2026-10-15")
        )
        assertEquals(listOf(2L, 1L, 3L), avisosPendientes(s, d("2026-10-12"), 3, emptySet()).map { it.suscripcion.id })
    }

    @Test
    fun clave_esEstablePorSuscripcionTipoYFecha() {
        assertEquals("COBRO:7@2026-10-15", claveAviso(7L, TipoAviso.COBRO, d("2026-10-15")))
        assertEquals("FIN_PRUEBA:7@2026-10-15", claveAviso(7L, TipoAviso.FIN_PRUEBA, d("2026-10-15")))
    }

    @Test
    fun purgar_quitaFechasPasadasYClavesRaras_yConservaHoyYFuturas() {
        val ya = setOf(
            "COBRO:1@2026-10-01",
            "COBRO:2@2026-10-12",
            "FIN_PRUEBA:3@2026-10-20",
            "basura"
        )
        assertEquals(setOf("COBRO:2@2026-10-12", "FIN_PRUEBA:3@2026-10-20"), purgarAvisosAntiguos(ya, d("2026-10-12")))
    }

    @Test
    fun idNotificacion_estableYDistintoPorSuscripcion() {
        assertEquals(idNotificacion(42L), idNotificacion(42L))
        assertEquals(42, idNotificacion(42L))
        assertNotEquals(idNotificacion(1L), idNotificacion(2L))
    }

    @Test
    fun minutosHasta_antesDeLaHora_esHoy() {
        assertEquals(90L, minutosHasta(LocalDateTime(2026, 10, 9, 8, 0), 9, 30))
    }

    @Test
    fun minutosHasta_despuesDeLaHora_esManana() {
        assertEquals(23L * 60 + 30, minutosHasta(LocalDateTime(2026, 10, 9, 10, 0), 9, 30))
        // Justo a la hora: el siguiente, no "ahora mismo" (evita dobles ejecuciones).
        assertEquals(24L * 60, minutosHasta(LocalDateTime(2026, 10, 9, 9, 30), 9, 30))
    }

    @Test
    fun horaDeSilencio_deNocheSi_deDiaNo() {
        assertTrue(esHoraDeSilencio(23))
        assertTrue(esHoraDeSilencio(3))
        assertFalse(esHoraDeSilencio(8))
        assertFalse(esHoraDeSilencio(21))
    }
}
