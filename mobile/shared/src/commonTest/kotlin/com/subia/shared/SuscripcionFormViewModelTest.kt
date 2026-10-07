package com.subia.shared

import com.subia.shared.model.AuthTokens
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiException
import com.subia.shared.network.NetworkException
import com.subia.shared.repository.CatalogRepository
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.repository.SubscriptionRepository
import com.subia.shared.storage.TokenStorageProvider
import com.subia.shared.viewmodel.FormError
import com.subia.shared.viewmodel.FormUiState
import com.subia.shared.viewmodel.SuscripcionFormViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    // ── Fixtures ─────────────────────────────────────────────────────────────

    /**
     * ViewModel real sobre un [ApiClient] con motor mock que responde 404 a todo:
     * suficiente para probar la validación, que ocurre antes de cualquier petición.
     */
    private fun crearViewModel(onRequest: (String) -> Unit = {}): SuscripcionFormViewModel {
        val engine = MockEngine { request ->
            onRequest(request.url.encodedPath)
            respondError(HttpStatusCode.NotFound)
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
}
