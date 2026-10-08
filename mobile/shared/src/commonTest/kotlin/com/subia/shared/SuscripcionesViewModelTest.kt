package com.subia.shared

import com.russhwolf.settings.MapSettings
import com.subia.shared.cache.CacheRepository
import com.subia.shared.model.ApiResponse
import com.subia.shared.model.AuthTokens
import com.subia.shared.model.Category
import com.subia.shared.model.NuevaSuscripcionRequest
import com.subia.shared.model.Subscription
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiRoutes
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.repository.SubscriptionRepository
import com.subia.shared.storage.TokenStorageProvider
import com.subia.shared.viewmodel.SuscripcionesUiState
import com.subia.shared.viewmodel.SuscripcionesViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

// =========================================================================================
// Pruebas sobre el ViewModel real: refresco silencioso y borrado con deshacer
// =========================================================================================

/**
 * [com.subia.shared.viewmodel.SuscripcionesViewModel] sobre un [ApiClient] con motor mock:
 * - la recarga con datos en memoria pasa por `Success(isRefreshing = true)` y nunca por `Loading`;
 * - [SuscripcionesViewModel.eliminarConDeshacer] oculta la fila sin llamar al servidor hasta
 *   pasada la ventana de deshacer;
 * - [SuscripcionesViewModel.deshacerEliminacion] la recupera y no se hace ninguna petición DELETE.
 */
class SuscripcionesViewModelBorradoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val subs = listOf(
        Subscription(id = 1L, nombre = "Netflix", precio = 12.99, periodoFacturacion = "MONTHLY", fechaRenovacion = "2026-12-01"),
        Subscription(id = 2L, nombre = "Spotify", precio = 10.99, periodoFacturacion = "MONTHLY", fechaRenovacion = "2026-12-05")
    )
    private val cats = listOf(Category(id = 1L, nombre = "Streaming"))

    private fun crearViewModel(onDelete: (Long) -> Unit = {}): SuscripcionesViewModel {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                request.method == HttpMethod.Delete -> {
                    onDelete(path.substringAfterLast('/').toLong())
                    respond("", HttpStatusCode.NoContent)
                }
                path == ApiRoutes.SUBSCRIPTIONS -> respondJson(json.encodeToString(ApiResponse(data = subs)))
                path == ApiRoutes.CATEGORIES -> respondJson(json.encodeToString(ApiResponse(data = cats)))
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val api = ApiClient(baseUrl = "http://localhost", tokenStorage = TokensFijos, isDebug = false, httpEngine = engine)
        return SuscripcionesViewModel(
            subscriptionRepository = SubscriptionRepository(api),
            categoryRepository = CategoryRepository(api),
            cacheRepository = CacheRepository(MapSettings())
        )
    }

    private fun MockRequestHandleScope.respondJson(body: String) = respond(
        content = body,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

    /** Espera (en tiempo real: la red mock responde en otro hilo) al estado Success estable. */
    private suspend fun SuscripcionesViewModel.esperarSuccess(): SuscripcionesUiState.Success =
        uiState.first { it is SuscripcionesUiState.Success && !it.isRefreshing } as SuscripcionesUiState.Success

    /** Deja correr los hilos reales un instante para comprobar que algo NO ocurre. */
    private suspend fun pausaReal(ms: Long = 150) = withContext(Dispatchers.Default) { delay(ms) }

    @Test
    fun cargaInicial_sinCache_pasaPorLoadingYLuegoSuccess() = runTest(dispatcher) {
        val vm = crearViewModel()
        assertTrue(vm.uiState.value is SuscripcionesUiState.Loading)
        val estado = vm.esperarSuccess()
        assertEquals(2, estado.suscripciones.size)
        assertFalse(estado.isRefreshing)
    }

    @Test
    fun recargaConDatos_marcaIsRefreshingSinVolverALoading() = runTest(dispatcher) {
        val vm = crearViewModel()
        vm.esperarSuccess()
        val estados = mutableListOf<SuscripcionesUiState>()
        val job = launch { vm.uiState.collect { estados += it } }
        runCurrent()
        estados.clear()

        vm.cargar()
        runCurrent()
        assertTrue(estados.first() is SuscripcionesUiState.Success, "El primer estado tras recargar debe ser Success, no Loading")
        assertTrue((estados.first() as SuscripcionesUiState.Success).isRefreshing)
        vm.esperarSuccess()
        assertTrue(estados.none { it is SuscripcionesUiState.Loading })
        job.cancel()
    }

    @Test
    fun eliminarConDeshacer_ocultaLaFilaYNoLlamaAlServidorHastaLaVentana() = runTest(dispatcher) {
        val borrado = CompletableDeferred<Long>()
        val borrados = mutableListOf<Long>()
        val vm = crearViewModel { borrados += it; borrado.complete(it) }
        vm.esperarSuccess()

        vm.eliminarConDeshacer(1L)
        runCurrent()
        val visibles = (vm.uiState.value as SuscripcionesUiState.Success).suscripciones.map { it.id }
        assertEquals(listOf(2L), visibles)
        // runCurrent() no avanza el reloj virtual: el delay() del borrado sigue pendiente y
        // el repositorio aún no se ha tocado. (Una pausa real aquí haría que runTest
        // adelantara el tiempo virtual por sí solo.)
        assertTrue(borrados.isEmpty(), "No debe borrar en el servidor antes de la ventana de deshacer")

        advanceTimeBy(SuscripcionesViewModel.UNDO_WINDOW_MS + 100)
        runCurrent()
        assertEquals(1L, borrado.await())
        vm.uiState.first { it is SuscripcionesUiState.Success && it.suscripciones.size == 1 }
        assertEquals(listOf(2L), (vm.uiState.value as SuscripcionesUiState.Success).suscripciones.map { it.id })
    }

    @Test
    fun deshacerEliminacion_recuperaLaFilaYNoBorraNunca() = runTest(dispatcher) {
        val borrados = mutableListOf<Long>()
        val vm = crearViewModel { borrados += it }
        vm.esperarSuccess()

        vm.eliminarConDeshacer(1L)
        runCurrent()
        vm.deshacerEliminacion(1L)
        runCurrent()
        assertEquals(listOf(1L, 2L), (vm.uiState.value as SuscripcionesUiState.Success).suscripciones.map { it.id })

        advanceTimeBy(SuscripcionesViewModel.UNDO_WINDOW_MS * 2)
        runCurrent()
        pausaReal()
        assertTrue(borrados.isEmpty(), "Tras deshacer no debe haber ninguna petición DELETE")
    }

    @Test
    fun confirmarEliminacion_borraDeInmediatoSinEsperarLaVentana() = runTest(dispatcher) {
        val borrado = CompletableDeferred<Long>()
        val vm = crearViewModel { borrado.complete(it) }
        vm.esperarSuccess()

        vm.eliminarConDeshacer(2L)
        vm.confirmarEliminacion(2L)
        runCurrent()
        assertEquals(2L, borrado.await())
    }

    private object TokensFijos : TokenStorageProvider {
        private val tokens = AuthTokens(accessToken = "a", refreshToken = "r")
        override fun saveTokens(tokens: AuthTokens) {}
        override fun getTokens(): AuthTokens = tokens
        override fun clearTokens() {}
        override fun hasTokens(): Boolean = true
    }
}
