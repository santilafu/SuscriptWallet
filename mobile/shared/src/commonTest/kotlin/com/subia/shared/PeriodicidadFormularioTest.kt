package com.subia.shared

import com.subia.shared.model.AuthTokens
import com.subia.shared.model.BillingCycle
import com.subia.shared.model.CatalogItem
import com.subia.shared.model.Subscription
import com.subia.shared.network.ApiClient
import com.subia.shared.repository.CatalogRepository
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.repository.SubscriptionRepository
import com.subia.shared.storage.TokenStorageProvider
import com.subia.shared.viewmodel.FormUiState
import com.subia.shared.viewmodel.SuscripcionFormViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.content.TextContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Periodicidad (`intervaloMeses`) en el formulario: carga, envío, huella de cambios, catálogo,
 * reenvío del ancla (R3-01) y los importes en vivo para la UI.
 */
class PeriodicidadFormularioTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val hoy = LocalDate(2026, 11, 10)

    private fun sub(periodo: String = "MONTHLY", intervalo: Int = 1, ancla: String = "2026-01-31", renewal: String = "2026-11-30") =
        Subscription(
            id = 7, nombre = "Seguro", precio = 90.0, periodoFacturacion = periodo,
            fechaRenovacion = renewal, fechaAncla = ancla, categoriaId = 3, intervaloMeses = intervalo
        )

    // ── Carga ────────────────────────────────────────────────────────────────

    @Test
    fun cargarTrimestral_cargaElIntervaloSinMarcarCambios() = runTest {
        val (vm, _) = vmCon()
        vm.cargarParaEditar(sub(intervalo = 3, ancla = "2026-02-15", renewal = "2027-02-15"))
        assertEquals(3, vm.intervaloMeses.value)
        assertEquals("MONTHLY", vm.periodoFacturacion.value)
        assertFalse(vm.hayCambios.value)
    }

    @Test
    fun cargarAnualDeBackendAntiguo_cargaDoce() = runTest {
        val (vm, cuerpo) = vmCon()
        vm.cargarParaEditar(sub(periodo = "YEARLY", intervalo = 1))
        assertEquals(12, vm.intervaloMeses.value)
        enviarYEsperar(vm)
        assertTrue("\"intervaloMeses\":12" in cuerpo(), cuerpo())
        assertTrue("\"billingCycle\":\"YEARLY\"" in cuerpo(), cuerpo())
    }

    @Test
    fun cargarSemanal_cargaUno() = runTest {
        val (vm, _) = vmCon()
        vm.cargarParaEditar(sub(periodo = "WEEKLY", ancla = "2026-01-01", renewal = "2026-11-12"))
        assertEquals(1, vm.intervaloMeses.value)
    }

    // ── Envío ────────────────────────────────────────────────────────────────

    @Test
    fun editarTrimestralSinTocarNada_enviaIntervalo3YElAncla() = runTest {
        val (vm, cuerpo) = vmCon()
        vm.cargarParaEditar(sub(intervalo = 3, ancla = "2026-02-15", renewal = "2027-02-15"))
        enviarYEsperar(vm)
        assertTrue("\"intervaloMeses\":3" in cuerpo(), cuerpo())
        assertTrue("\"billingCycle\":\"MONTHLY\"" in cuerpo(), cuerpo())
        assertTrue("\"renewalDate\":\"2026-02-15\"" in cuerpo(), cuerpo())
    }

    @Test
    fun volverDeTrimestralAMensual_enviaElUno() = runTest {
        val (vm, cuerpo) = vmCon()
        vm.cargarParaEditar(sub(intervalo = 3, ancla = "2026-02-15", renewal = "2027-02-15"))
        vm.seleccionarIntervalo(1)
        enviarYEsperar(vm)
        assertTrue("\"intervaloMeses\":1" in cuerpo(), cuerpo())
    }

    @Test
    fun cambiarElIntervalo_enviaLaFechaMostradaYNoElAncla() = runTest {
        val (vm, cuerpo) = vmCon()
        vm.cargarParaEditar(sub(intervalo = 1, ancla = "2026-01-31", renewal = "2026-11-30"))
        assertEquals("2026-11-30", vm.fechaRenovacion.value)
        vm.seleccionarIntervalo(6)
        enviarYEsperar(vm)
        assertTrue("\"renewalDate\":\"2026-11-30\"" in cuerpo(), cuerpo())
        assertTrue("\"intervaloMeses\":6" in cuerpo(), cuerpo())
    }

    @Test
    fun alta_mensualPorDefecto_enviaIntervalo1() = runTest {
        val (vm, cuerpo) = vmCon()
        vm.nombre.value = "Luz"
        vm.precio.value = "40"
        vm.fechaRenovacion.value = "2026-12-01"
        vm.categoriaId.value = 3L
        vm.enviar(esEdicion = false)
        vm.uiState.first { it !is FormUiState.Loading && it != FormUiState.Idle }
        assertTrue("\"intervaloMeses\":1" in cuerpo(), cuerpo())
    }

    @Test
    fun selectorAntiguoDeCiclo_mantieneLaParejaCoherente() = runTest {
        // La pantalla actual (sprint 4 pendiente) escribe periodoFacturacion directamente.
        val (vm, cuerpo) = vmCon()
        vm.cargarParaEditar(sub(intervalo = 1))
        vm.periodoFacturacion.value = "YEARLY"
        enviarYEsperar(vm)
        assertTrue("\"intervaloMeses\":12" in cuerpo(), cuerpo())

        val (vm2, cuerpo2) = vmCon()
        vm2.cargarParaEditar(sub(periodo = "YEARLY", intervalo = 12))
        vm2.periodoFacturacion.value = "MONTHLY"
        enviarYEsperar(vm2)
        assertTrue("\"intervaloMeses\":1" in cuerpo2(), cuerpo2())
    }

    // ── Selección e huella ───────────────────────────────────────────────────

    @Test
    fun seleccionarIntervalo_derivaElCiclo_eIgnoraValoresNoPermitidos() = runTest {
        val (vm, _) = vmCon()
        vm.seleccionarIntervalo(12)
        assertEquals("YEARLY", vm.periodoFacturacion.value)
        vm.seleccionarIntervalo(3)
        assertEquals("MONTHLY", vm.periodoFacturacion.value)
        assertEquals(3, vm.intervaloMeses.value)
        vm.seleccionarIntervalo(5)
        assertEquals(3, vm.intervaloMeses.value)
    }

    @Test
    fun seleccionarPeriodo_ajustaElIntervalo() = runTest {
        val (vm, _) = vmCon()
        vm.seleccionarIntervalo(3)
        vm.seleccionarPeriodo("WEEKLY")
        assertEquals(1, vm.intervaloMeses.value)
        vm.seleccionarPeriodo("YEARLY")
        assertEquals(12, vm.intervaloMeses.value)
        vm.seleccionarPeriodo("MONTHLY")
        assertEquals(1, vm.intervaloMeses.value)
    }

    @Test
    fun intervaloCoherente_casos() {
        assertEquals(12, SuscripcionFormViewModel.intervaloCoherente("YEARLY", 1))
        assertEquals(1, SuscripcionFormViewModel.intervaloCoherente("WEEKLY", 6))
        assertEquals(6, SuscripcionFormViewModel.intervaloCoherente("MONTHLY", 6))
        assertEquals(1, SuscripcionFormViewModel.intervaloCoherente("MONTHLY", 12))
        assertEquals(1, SuscripcionFormViewModel.intervaloCoherente("MONTHLY", 5))
    }

    @Test
    fun huella_detectaElCambioDeIntervalo() = runTest {
        val (vm, _) = vmCon()
        vm.cargarParaEditar(sub(intervalo = 3, ancla = "2026-02-15", renewal = "2027-02-15"))
        assertFalse(vm.hayCambios.value)
        vm.seleccionarIntervalo(6)
        assertTrue(vm.hayCambios.value)
        vm.seleccionarIntervalo(3)
        assertFalse(vm.hayCambios.value)
    }

    // ── Catálogo ─────────────────────────────────────────────────────────────

    @Test
    fun catalogo_mensualEsUno_yAnualEsDoce() = runTest {
        val (vm, _) = vmCon()
        val item = CatalogItem(id = 1, nombre = "Netflix", precioMensual = 12.99, precioAnual = 129.99, periodoFacturacion = "MONTHLY")
        vm.prerellenarDesdeCatalogo(item, BillingCycle.YEARLY)
        assertEquals(12, vm.intervaloMeses.value)
        assertEquals("YEARLY", vm.periodoFacturacion.value)
        vm.prerellenarDesdeCatalogo(item, BillingCycle.MONTHLY)
        assertEquals(1, vm.intervaloMeses.value)
        assertEquals("MONTHLY", vm.periodoFacturacion.value)
    }

    // ── Importes en vivo ─────────────────────────────────────────────────────

    @Test
    fun enVivo_equivalenteMensualYTotalAnual() = runTest {
        val (vm, _) = vmCon()
        vm.precio.value = "90"
        vm.seleccionarIntervalo(3)
        assertEquals(30.0, vm.equivalenteMensualEnVivo.value)
        assertEquals(360.0, vm.totalAnualEnVivo.value)

        vm.precio.value = "10"
        assertEquals(3.33, vm.equivalenteMensualEnVivo.value)
        assertEquals(40.0, vm.totalAnualEnVivo.value)

        vm.precio.value = "120"
        vm.seleccionarIntervalo(12)
        assertEquals(10.0, vm.equivalenteMensualEnVivo.value)
        assertEquals(120.0, vm.totalAnualEnVivo.value)
    }

    @Test
    fun enVivo_semanalUsa52Semanas() = runTest {
        val (vm, _) = vmCon()
        vm.precio.value = "5"
        vm.seleccionarPeriodo("WEEKLY")
        assertEquals(21.67, vm.equivalenteMensualEnVivo.value)
        assertEquals(260.0, vm.totalAnualEnVivo.value)
    }

    @Test
    fun enVivo_importeNoValido_esNull() = runTest {
        val (vm, _) = vmCon()
        assertNull(vm.equivalenteMensualEnVivo.value)
        vm.precio.value = "abc"
        assertNull(vm.equivalenteMensualEnVivo.value)
        assertNull(vm.totalAnualEnVivo.value)
        vm.precio.value = "-3"
        assertNull(vm.totalAnualEnVivo.value)
        vm.precio.value = "1e20"
        assertNull(vm.totalAnualEnVivo.value)
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private suspend fun enviarYEsperar(vm: SuscripcionFormViewModel) {
        vm.enviar(esEdicion = true, id = 7)
        vm.uiState.first { it !is FormUiState.Loading && it != FormUiState.Idle }
    }

    /** ViewModel con motor mock que guarda el último cuerpo enviado y responde 404 a todo. */
    private fun vmCon(): Pair<SuscripcionFormViewModel, () -> String> {
        var cuerpo = ""
        val engine = MockEngine { request ->
            when (val body = request.body) {
                is TextContent -> cuerpo = body.text
                is ByteArrayContent -> cuerpo = body.bytes().decodeToString()
                else -> {}
            }
            respondError(HttpStatusCode.NotFound)
        }
        val api = ApiClient(baseUrl = "http://localhost", tokenStorage = Tokens, isDebug = false, httpEngine = engine)
        val vm = SuscripcionFormViewModel(
            subscriptionRepository = SubscriptionRepository(api),
            catalogRepository = CatalogRepository(api),
            categoryRepository = CategoryRepository(api),
            hoy = { hoy }
        )
        return vm to { cuerpo }
    }

    private object Tokens : TokenStorageProvider {
        private val tokens = AuthTokens(accessToken = "a", refreshToken = "r")
        override fun saveTokens(tokens: AuthTokens) {}
        override fun getTokens(): AuthTokens = tokens
        override fun clearTokens() {}
        override fun hasTokens(): Boolean = true
    }
}
