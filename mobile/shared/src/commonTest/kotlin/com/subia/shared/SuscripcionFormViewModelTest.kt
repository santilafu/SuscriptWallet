package com.subia.shared

import com.subia.shared.model.AuthTokens
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiException
import com.subia.shared.network.NetworkException
import com.subia.shared.repository.CatalogRepository
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.repository.SubscriptionRepository
import com.subia.shared.storage.TokenStorageProvider
import com.subia.shared.model.CatalogItem
import com.subia.shared.model.Category
import com.subia.shared.viewmodel.Campo
import com.subia.shared.viewmodel.FormError
import com.subia.shared.viewmodel.FormUiState
import com.subia.shared.viewmodel.SuscripcionFormViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.content.TextContent
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pruebas de la validación del formulario de suscripción.
 *
 * La validación se expone como función pura ([SuscripcionFormViewModel.validar]) para que
 * la UI de cada plataforma solo tenga que mapear un [FormError] tipado a su texto localizado:
 * el ViewModel ya no contiene literales en castellano.
 */
class SuscripcionFormViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // ── validar(): orden y casos ─────────────────────────────────────────────

    @Test
    fun validar_formularioCompleto_noDevuelveError() {
        assertNull(SuscripcionFormViewModel.validar("Netflix", "12,99", "2026-11-01", 3L))
    }

    @Test
    fun validar_nombreEnBlanco_devuelveNombreVacio() {
        assertEquals(FormError.NombreVacio, SuscripcionFormViewModel.validar("   ", "9.99", "2026-11-01", 3L))
    }

    @Test
    fun validar_precioNoNumerico_devuelvePrecioInvalido() {
        assertEquals(FormError.PrecioInvalido, SuscripcionFormViewModel.validar("Netflix", "abc", "2026-11-01", 3L))
    }

    @Test
    fun validar_precioCero_devuelvePrecioInvalido() {
        assertEquals(FormError.PrecioInvalido, SuscripcionFormViewModel.validar("Netflix", "0", "2026-11-01", 3L))
    }

    @Test
    fun validar_precioConComaDecimal_esValido() {
        assertNull(SuscripcionFormViewModel.validar("Netflix", "4,50", "2026-11-01", 3L))
    }

    @Test
    fun validar_fechaRenovacionVacia_devuelveFechaRenovacionVacia() {
        assertEquals(FormError.FechaRenovacionVacia, SuscripcionFormViewModel.validar("Netflix", "9.99", "", 3L))
    }

    @Test
    fun validar_categoriaNula_devuelveCategoriaNoSeleccionada() {
        assertEquals(FormError.CategoriaNoSeleccionada, SuscripcionFormViewModel.validar("Netflix", "9.99", "2026-11-01", null))
    }

    @Test
    fun validar_categoriaCero_devuelveCategoriaNoSeleccionada() {
        assertEquals(FormError.CategoriaNoSeleccionada, SuscripcionFormViewModel.validar("Netflix", "9.99", "2026-11-01", 0L))
    }

    /** El primer error en orden visual gana: nombre antes que precio. */
    @Test
    fun validar_variosErrores_devuelveElPrimeroEnOrdenDeCampos() {
        assertEquals(FormError.NombreVacio, SuscripcionFormViewModel.validar("", "", "", null))
    }

    // ── mapearErrorGuardado() ────────────────────────────────────────────────

    @Test
    fun mapearErrorGuardado_networkException_devuelveSinConexion() {
        assertEquals(FormError.SinConexion, SuscripcionFormViewModel.mapearErrorGuardado(NetworkException("timeout")))
    }

    @Test
    fun mapearErrorGuardado_apiException_devuelveGuardadoFallido() {
        assertEquals(FormError.GuardadoFallido, SuscripcionFormViewModel.mapearErrorGuardado(ApiException(500, "Internal")))
    }

    // ── enviar(): el ViewModel expone el error tipado en el estado ───────────

    @Test
    fun enviar_sinCategoria_exponeCategoriaNoSeleccionadaEnElEstado() = runTest {
        val vm = crearViewModel()
        vm.nombre.value = "Spotify"
        vm.precio.value = "10.99"
        vm.fechaRenovacion.value = "2026-11-01"
        vm.categoriaId.value = null

        vm.enviar(esEdicion = false)

        assertEquals(FormUiState.Error(FormError.CategoriaNoSeleccionada), vm.uiState.value)
    }

    @Test
    fun enviar_nombreVacio_exponeNombreVacioSinLlamarAlServidor() = runTest {
        var peticionesDeGuardado = 0
        val vm = crearViewModel { path -> if (path.contains("subscriptions")) peticionesDeGuardado++ }
        vm.precio.value = "10.99"
        vm.fechaRenovacion.value = "2026-11-01"
        vm.categoriaId.value = 1L

        vm.enviar(esEdicion = false)

        assertEquals(FormUiState.Error(FormError.NombreVacio), vm.uiState.value)
        assertEquals(0, peticionesDeGuardado, "La validación debe cortar antes de llamar al servidor")
    }

    // ── validarCampos(): mapa campo → error (validación en vivo) ─────────────

    @Test
    fun validarCampos_todoVacio_devuelveLosCuatroErrores() {
        val errores = SuscripcionFormViewModel.validarCampos("", "", "", null)
        assertEquals(
            mapOf(
                Campo.Nombre to FormError.NombreVacio,
                Campo.Precio to FormError.PrecioInvalido,
                Campo.FechaRenovacion to FormError.FechaRenovacionVacia,
                Campo.Categoria to FormError.CategoriaNoSeleccionada
            ),
            errores
        )
    }

    @Test
    fun validarCampos_soloPrecioMalo_devuelveSoloElImporte() {
        val errores = SuscripcionFormViewModel.validarCampos("Netflix", "-3", "2026-11-01", 2L)
        assertEquals(mapOf(Campo.Precio to FormError.PrecioInvalido), errores)
    }

    @Test
    fun validarCampos_formularioCompleto_devuelveMapaVacio() {
        assertTrue(SuscripcionFormViewModel.validarCampos("Netflix", "12,99", "2026-11-01", 2L).isEmpty())
    }

    // ── parsearPrecio(): coma, punto y miles ─────────────────────────────────

    @Test
    fun parsearPrecio_aceptaComaYPuntoComoDecimal() {
        assertEquals(9.99, SuscripcionFormViewModel.parsearPrecio("9,99"))
        assertEquals(9.99, SuscripcionFormViewModel.parsearPrecio("9.99"))
        assertEquals(12.0, SuscripcionFormViewModel.parsearPrecio("12"))
    }

    @Test
    fun parsearPrecio_conSeparadorDeMiles_usaElUltimoComoDecimal() {
        assertEquals(1234.56, SuscripcionFormViewModel.parsearPrecio("1.234,56"))
        assertEquals(1234.56, SuscripcionFormViewModel.parsearPrecio("1,234.56"))
    }

    @Test
    fun parsearPrecio_ignoraEspacios() {
        assertEquals(10.5, SuscripcionFormViewModel.parsearPrecio(" 10,5 "))
    }

    @Test
    fun parsearPrecio_textoNoNumerico_devuelveNull() {
        assertNull(SuscripcionFormViewModel.parsearPrecio("abc"))
        assertNull(SuscripcionFormViewModel.parsearPrecio(""))
        assertNull(SuscripcionFormViewModel.parsearPrecio("9,,9"))
    }

    @Test
    fun formatearPrecioParaCampo_quitaDecimalesInutiles() {
        assertEquals("12", SuscripcionFormViewModel.formatearPrecioParaCampo(12.0))
        assertEquals("9.99", SuscripcionFormViewModel.formatearPrecioParaCampo(9.99))
        assertEquals("1.08", SuscripcionFormViewModel.formatearPrecioParaCampo(12.99 / 12.0))
    }

    // ── filtrarCatalogo(): autocompletado del nombre ─────────────────────────

    private val catalogo = listOf(
        CatalogItem(id = 1, nombre = "Netflix", precioMensual = 12.99),
        CatalogItem(id = 2, nombre = "Spotify", precioMensual = 10.99),
        CatalogItem(id = 3, nombre = "Movistar+", precioMensual = 14.0),
        CatalogItem(id = 4, nombre = "Disney+", precioMensual = 8.99)
    )

    @Test
    fun filtrarCatalogo_textoVacio_noSugiereNada() {
        assertTrue(SuscripcionFormViewModel.filtrarCatalogo("", catalogo, null).isEmpty())
    }

    @Test
    fun filtrarCatalogo_ignoraMayusculasYPrioritzaPrefijo() {
        val resultado = SuscripcionFormViewModel.filtrarCatalogo("net", catalogo, null)
        assertEquals(listOf("Netflix"), resultado.map { it.nombre })
    }

    @Test
    fun filtrarCatalogo_siElTextoEsElServicioYaElegido_noReabre() {
        val netflix = catalogo[0]
        assertTrue(SuscripcionFormViewModel.filtrarCatalogo("Netflix", catalogo, netflix).isEmpty())
    }

    // ── Flujos en vivo: errores, puedeGuardar y hayCambios ───────────────────

    @Test
    fun puedeGuardar_soloCuandoTodosLosCamposSonValidos() = runTest {
        val vm = crearViewModel()
        assertFalse(vm.puedeGuardar.value)
        assertEquals(FormError.NombreVacio, vm.errores.value[Campo.Nombre])

        vm.nombre.value = "Spotify"
        vm.precio.value = "10,99"
        vm.fechaRenovacion.value = "2026-11-01"
        vm.categoriaId.value = 1L

        assertTrue(vm.errores.value.isEmpty())
        assertTrue(vm.puedeGuardar.value)
    }

    @Test
    fun hayCambios_esFalsoAlAbrirYVerdaderoTrasEscribir() = runTest {
        val vm = crearViewModel()
        assertFalse(vm.hayCambios.value)
        vm.nombre.value = "N"
        assertTrue(vm.hayCambios.value)
        vm.marcarComoSinCambios()
        assertFalse(vm.hayCambios.value)
    }

    @Test
    fun seleccionarServicioDelCatalogo_prerrellenaPrecioCicloYLogo() = runTest {
        val vm = crearViewModel()
        vm.seleccionarServicioDelCatalogo(
            CatalogItem(id = 1, nombre = "Netflix", precioMensual = 12.99, periodoFacturacion = "MONTHLY", domain = "netflix.com")
        )
        assertEquals("Netflix", vm.nombre.value)
        assertEquals("12.99", vm.precio.value)
        assertEquals("MONTHLY", vm.periodoFacturacion.value)
        assertEquals("netflix.com", vm.catalogoSeleccionado.value?.domain)
        assertTrue(vm.sugerencias.value.isEmpty(), "Tras elegir, el desplegable no debe reabrirse")
    }

    /** El precio del catálogo es solo un punto de partida: el que se guarda es el que deja el usuario. */
    @Test
    fun seleccionarServicioDelCatalogo_precioEditadoEsElQueSeEnvia() = runTest {
        var cuerpoEnviado: String? = null
        val vm = crearViewModel(onRequestBody = { cuerpoEnviado = it })
        vm.seleccionarServicioDelCatalogo(
            CatalogItem(id = 1, nombre = "Netflix", precioMensual = 12.99, periodoFacturacion = "MONTHLY")
        )
        vm.precio.value = "9,50"
        vm.fechaRenovacion.value = "2026-11-01"
        vm.categoriaId.value = 3L
        vm.enviar(esEdicion = false)
        vm.uiState.first { it !is FormUiState.Loading && it != FormUiState.Idle }
        val cuerpo = cuerpoEnviado ?: error("No se envió ninguna petición")
        assertTrue("\"price\":9.5" in cuerpo, "Se esperaba el precio editado en el cuerpo: $cuerpo")
    }

    @Test
    fun cargarParaEditar_porId_precargaLosCamposYNoMarcaCambios() = runTest {
        val vm = crearViewModel(respuestas = mapOf(
            "/subscriptions/7" to """{"data":{"id":7,"name":"Spotify","price":10.99,"moneda":"EUR","billingCycle":"MONTHLY","renewalDate":"2026-11-03","categoryId":2,"notes":"familiar"}}"""
        ))
        vm.cargarParaEditar(7L)
        vm.cargandoEdicion.first { !it } // espera a la respuesta del motor mock
        assertEquals("Spotify", vm.nombre.value)
        assertEquals("10.99", vm.precio.value)
        assertEquals("2026-11-03", vm.fechaRenovacion.value)
        assertEquals(2L, vm.categoriaId.value)
        assertEquals("familiar", vm.notas.value)
        assertFalse(vm.hayCambios.value, "Tras cargar para editar no hay cambios pendientes")
        // puedeGuardar es un StateFlow derivado de los errores: esperamos a que se propague
        assertTrue(vm.puedeGuardar.first { it })
        assertFalse(vm.cargandoEdicion.value)
    }

    @Test
    fun cargarParaEditar_porId_siFallaExponeErrorYDejaElFormularioVacio() = runTest {
        val vm = crearViewModel()
        vm.cargarParaEditar(99L)
        val estado = vm.uiState.first { it is FormUiState.Error }
        assertEquals("", vm.nombre.value)
        assertEquals(FormUiState.Error(FormError.GuardadoFallido), estado)
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    /**
     * ViewModel real sobre un [ApiClient] con motor mock que responde 404 a todo:
     * suficiente para probar la validación, que ocurre antes de cualquier petición.
     */
    private fun crearViewModel(
        respuestas: Map<String, String> = emptyMap(),
        onRequest: (String) -> Unit = {},
        onRequestBody: (String) -> Unit = {}
    ): SuscripcionFormViewModel {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            onRequest(path)
            when (val body = request.body) {
                is TextContent -> onRequestBody(body.text)
                is ByteArrayContent -> onRequestBody(body.bytes().decodeToString())
                else -> {}
            }
            val cuerpo = respuestas.entries.firstOrNull { path.endsWith(it.key) }?.value
            if (cuerpo != null) {
                respond(cuerpo, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }
        val api = ApiClient(
            baseUrl = "http://localhost",
            tokenStorage = TokensFijos,
            isDebug = false,
            httpEngine = engine
        )
        return SuscripcionFormViewModel(
            subscriptionRepository = SubscriptionRepository(api),
            catalogRepository = CatalogRepository(api),
            categoryRepository = CategoryRepository(api)
        )
    }

    private object TokensFijos : TokenStorageProvider {
        private val tokens = AuthTokens(accessToken = "a", refreshToken = "r")
        override fun saveTokens(tokens: AuthTokens) {}
        override fun getTokens(): AuthTokens = tokens
        override fun clearTokens() {}
        override fun hasTokens(): Boolean = true
    }

    // ── buscarCategoriaDeClave(): categoría al elegir del catálogo ─────────

    private val categoriasSembradas = listOf(
        Category(id = 1, nombre = "Streaming"),
        Category(id = 2, nombre = "Hogar y suministros"),
        Category(id = 3, nombre = "Telecomunicaciones"),
        Category(id = 4, nombre = "Noticias y Lectura"),
        Category(id = 5, nombre = "Seguros")
    )

    @Test
    fun buscarCategoriaDeClave_resuelveNombresSembradosDistintosDeLaClave() {
        assertEquals(2L, SuscripcionFormViewModel.buscarCategoriaDeClave("hogar", categoriasSembradas)?.id)
        assertEquals(3L, SuscripcionFormViewModel.buscarCategoriaDeClave("telecos", categoriasSembradas)?.id)
        assertEquals(4L, SuscripcionFormViewModel.buscarCategoriaDeClave("noticias", categoriasSembradas)?.id)
    }

    @Test
    fun buscarCategoriaDeClave_nombreIgualALaClave() {
        assertEquals(5L, SuscripcionFormViewModel.buscarCategoriaDeClave("seguros", categoriasSembradas)?.id)
        assertEquals(1L, SuscripcionFormViewModel.buscarCategoriaDeClave("streaming", categoriasSembradas)?.id)
    }

    @Test
    fun buscarCategoriaDeClave_sinCoincidenciaEsNull() {
        assertNull(SuscripcionFormViewModel.buscarCategoriaDeClave("transporte", categoriasSembradas))
        assertNull(SuscripcionFormViewModel.buscarCategoriaDeClave("", categoriasSembradas))
    }

    @Test
    fun filtrarCatalogo_sinDuplicadosPorNombre_prefiereLaCategoriaReal() {
        val items = listOf(
            CatalogItem(id = 1, nombre = "Spotify Premium", precioMensual = 11.99, categoriaKey = "prueba"),
            CatalogItem(id = 2, nombre = "Spotify Premium", precioMensual = 11.99, categoriaKey = "musica")
        )
        val resultado = SuscripcionFormViewModel.filtrarCatalogo("spoti", items, null)
        assertEquals(1, resultado.size)
        assertEquals("musica", resultado.first().categoriaKey)
    }
}
