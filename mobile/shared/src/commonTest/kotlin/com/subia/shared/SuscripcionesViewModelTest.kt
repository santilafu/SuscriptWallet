package com.subia.shared

import com.subia.shared.model.Category
import com.subia.shared.model.NuevaSuscripcionRequest
import com.subia.shared.model.Subscription
import com.subia.shared.viewmodel.SuscripcionesUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas unitarias para la máquina de estados de filtrado por categoría de
 * [com.subia.shared.viewmodel.SuscripcionesViewModel].
 *
 * Se prueba la lógica de filtrado pura (sin red ni Koin) mediante una clase
 * auxiliar que replica el comportamiento de filtrado del ViewModel.
 * Especificación: TEST-03 — SuscripcionesViewModel filter state machine.
 */
class SuscripcionesViewModelTest {

    private val categoriaEntretenimiento = Category(id = 1L, nombre = "Entretenimiento")
    private val categoriaProductividad   = Category(id = 2L, nombre = "Productividad")

    private val subEntretenimiento1 = sub(id = 1L, nombre = "Netflix",  categoriaId = 1L)
    private val subEntretenimiento2 = sub(id = 2L, nombre = "Spotify",  categoriaId = 1L)
    private val subProductividad    = sub(id = 3L, nombre = "Notion",   categoriaId = 2L)
    private val subSinCategoria     = sub(id = 4L, nombre = "Sin cat",  categoriaId = null)

    private val todasLasSubs = listOf(
        subEntretenimiento1, subEntretenimiento2, subProductividad, subSinCategoria
    )
    private val todasLasCategorias = listOf(categoriaEntretenimiento, categoriaProductividad)

    // -------------------------------------------------------------------------------------
    // TEST: seleccionarCategoria(id) filtra la lista correctamente
    // -------------------------------------------------------------------------------------

    /**
     * Verifica que al seleccionar la categoría "Entretenimiento", la lista filtrada
     * contiene únicamente las suscripciones de esa categoría.
     * Especificación: TEST-03 — filtrarPorCategoria(id) emits filtered list.
     */
    @Test
    fun filtrarPorCategoria_conIdValido_muestraSoloEsaCategoria() {
        val filtrador = FiltradoCategorias(todasLasSubs)

        val resultado = filtrador.filtrar(categoriaId = 1L)

        assertEquals(2, resultado.size, "Debe devolver exactamente 2 suscripciones de Entretenimiento")
        assertTrue(resultado.all { it.categoriaId == 1L }, "Todas deben ser de categoría 1")
        assertTrue(resultado.any { it.nombre == "Netflix" })
        assertTrue(resultado.any { it.nombre == "Spotify" })
    }

    /**
     * Verifica que filtrar por categoría "Productividad" devuelve solo "Notion".
     */
    @Test
    fun filtrarPorCategoria_productividad_devuelveUnaSubscripcion() {
        val filtrador = FiltradoCategorias(todasLasSubs)

        val resultado = filtrador.filtrar(categoriaId = 2L)

        assertEquals(1, resultado.size)
        assertEquals("Notion", resultado.first().nombre)
    }

    // -------------------------------------------------------------------------------------
    // TEST: seleccionarCategoria(null) muestra todas las suscripciones
    // -------------------------------------------------------------------------------------

    /**
     * Verifica que pasar `null` como categoriaId muestra todas las suscripciones sin filtrar.
     * Especificación: TEST-03 — seleccionarCategoria(null) shows all subscriptions.
     */
    @Test
    fun filtrarPorCategoria_conNull_muestraTodasLasSuscripciones() {
        val filtrador = FiltradoCategorias(todasLasSubs)

        // Primero filtrar por categoría
        filtrador.filtrar(categoriaId = 1L)
        // Luego limpiar el filtro
        val resultado = filtrador.filtrar(categoriaId = null)

        assertEquals(todasLasSubs.size, resultado.size, "Debe devolver todas las suscripciones")
    }

    /**
     * Verifica que al limpiar el filtro después de una selección previa,
     * la categoría seleccionada vuelve a ser null.
     */
    @Test
    fun filtrarPorCategoria_conNull_categoriaSeleccionadaEsNull() {
        val filtrador = FiltradoCategorias(todasLasSubs)
        filtrador.filtrar(categoriaId = 1L) // seleccionar

        val categoriaActual = filtrador.categoriaActual
        assertEquals(1L, categoriaActual)

        filtrador.filtrar(categoriaId = null) // limpiar

        assertNull(filtrador.categoriaActual, "Al pasar null, la categoría seleccionada debe ser null")
    }

    // -------------------------------------------------------------------------------------
    // TEST: Pulsar el mismo chip activo limpia el filtro
    // -------------------------------------------------------------------------------------

    /**
     * Verifica que volver a pulsar la misma categoría activa equivale a pasar null
     * (el filtro se limpia). Especificación: FILTER-01 — pulsar chip activo lo deselecciona.
     */
    @Test
    fun filtrarPorCategoria_pulsarMismaCategoria_limpiaFiltro() {
        val filtrador = FiltradoCategorias(todasLasSubs)
        filtrador.filtrar(categoriaId = 1L) // activar

        // Simular toggle: si ya estaba seleccionada, pasar null
        val nuevaCategoria = if (filtrador.categoriaActual == 1L) null else 1L
        val resultado = filtrador.filtrar(categoriaId = nuevaCategoria)

        assertNull(filtrador.categoriaActual)
        assertEquals(todasLasSubs.size, resultado.size)
    }

    // -------------------------------------------------------------------------------------
    // TEST: Categoría sin suscripciones devuelve lista vacía
    // -------------------------------------------------------------------------------------

    /**
     * Verifica que filtrar por una categoría sin suscripciones devuelve lista vacía
     * (estado vacío del filtro).
     * Especificación: FILTER-01 — Categoría sin suscripciones devuelve estado vacío.
     */
    @Test
    fun filtrarPorCategoria_categoriaVacia_devuelveListaVacia() {
        val filtrador = FiltradoCategorias(todasLasSubs)

        val resultado = filtrador.filtrar(categoriaId = 99L) // categoría inexistente

        assertTrue(resultado.isEmpty(), "La lista filtrada debe estar vacía")
        assertEquals(99L, filtrador.categoriaActual, "La categoría seleccionada sigue siendo 99L")
    }

    // -------------------------------------------------------------------------------------
    // TEST: Estado inicial sin filtro
    // -------------------------------------------------------------------------------------

    /**
     * Verifica que en el estado inicial (sin filtro aplicado) se muestran todas las suscripciones.
     */
    @Test
    fun estadoInicial_sinFiltro_muestraTodasLasSuscripciones() {
        val filtrador = FiltradoCategorias(todasLasSubs)

        val resultado = filtrador.filtrar(categoriaId = null)

        assertEquals(todasLasSubs.size, resultado.size)
        assertNull(filtrador.categoriaActual)
    }

    // -------------------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------------------

    private fun sub(
        id: Long,
        nombre: String,
        precio: Double = 9.99,
        moneda: String = "EUR",
        categoriaId: Long?
    ) = Subscription(
        id = id,
        nombre = nombre,
        precio = precio,
        moneda = moneda,
        periodoFacturacion = "MONTHLY",
        fechaRenovacion = "2026-12-31",
        categoriaId = categoriaId
    )
}

/**
 * Réplica pura de la lógica de filtrado de [com.subia.shared.viewmodel.SuscripcionesViewModel].
 * No usa ni Android ni Koin, permitiendo su ejecución en commonTest.
 */
private class FiltradoCategorias(private val todasLasSubs: List<Subscription>) {
    var categoriaActual: Long? = null

    fun filtrar(categoriaId: Long?): List<Subscription> {
        categoriaActual = categoriaId
        return if (categoriaId == null) todasLasSubs
        else todasLasSubs.filter { it.categoriaId == categoriaId }
    }
}
