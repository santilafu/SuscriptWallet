package com.subia.shared

import com.subia.shared.model.AuthTokens
import com.subia.shared.model.Subscription
import com.subia.shared.model.TipoAviso
import com.subia.shared.model.avisosPendientes
import com.subia.shared.model.proximaRenovacion
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
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ronda 3 de la auditoría:
 * - R3-01: el backend envía `renewalAnchor`; al editar sin tocar la fecha se reenvía el ancla
 *   (el día 31 / 29-feb no se pierde).
 * - R3-02: la proyección y los avisos parten del ancla con el "hoy" del MÓVIL, así que un servidor
 *   que va un día por delante no se salta el cobro de "hoy" local.
 */
class Ronda3AuditoriaTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val json = Json { ignoreUnknownKeys = true }

    private fun sub(renewal: String, ancla: String?, ciclo: String = "MONTHLY") = Subscription(
        id = 7, nombre = "Netflix", precio = 12.99, periodoFacturacion = ciclo,
        fechaRenovacion = renewal, categoriaId = 3, fechaAncla = ancla
    )

    // ── Serialización ────────────────────────────────────────────────────────

    @Test
    fun deserializa_renewalAnchor_yEsCompatibleSinEl() {
        val con = json.decodeFromString(Subscription.serializer(),
            """{"id":7,"name":"N","price":1.0,"billingCycle":"MONTHLY","renewalDate":"2026-11-30","renewalAnchor":"2026-01-31"}""")
        assertEquals("2026-01-31", con.fechaAncla)
        assertEquals("2026-01-31", con.anclaRenovacion)
        val sin = json.decodeFromString(Subscription.serializer(),
            """{"id":7,"name":"N","price":1.0,"billingCycle":"MONTHLY","renewalDate":"2026-11-30"}""")
        assertNull(sin.fechaAncla)
        assertEquals("2026-11-30", sin.anclaRenovacion)
    }

    // ── Proyección desde el ancla ────────────────────────────────────────────

    @Test
    fun proyeccion_desdeElAncla_vuelveAl31EnDiciembre() {
        val s = sub(renewal = "2026-11-30", ancla = "2026-01-31")
        assertEquals(LocalDate(2026, 11, 30), proximaRenovacion(s, LocalDate(2026, 11, 10)))
        // Desde la fecha proyectada saldría el 30-dic; desde el ancla, el 31.
        assertEquals(LocalDate(2026, 12, 31), proximaRenovacion(s, LocalDate(2026, 12, 1)))
    }

    @Test
    fun anualDel29Feb_vuelveAl29EnBisiesto() {
        val s = sub(renewal = "2027-02-28", ancla = "2024-02-29", ciclo = "YEARLY")
        assertEquals(LocalDate(2028, 2, 29), proximaRenovacion(s, LocalDate(2027, 3, 1)))
    }

    @Test
    fun servidorUnDiaPorDelante_noSeSaltaElCobroDeHoyLocal() {
        // Ancla día 9. En Madrid ya es 10-oct y el servidor proyecta al 9-nov; en el móvil
        // (América) aún es 9-oct: el cobro es HOY y debe avisarse.
        val s = sub(renewal = "2026-11-09", ancla = "2026-01-09")
        val hoyLocal = LocalDate(2026, 10, 9)
        assertEquals(hoyLocal, proximaRenovacion(s, hoyLocal))
        val avisos = avisosPendientes(listOf(s), hoyLocal, umbralDias = 1, yaAvisados = emptySet())
        assertEquals(1, avisos.size)
        assertEquals(TipoAviso.COBRO, avisos[0].tipo)
        assertEquals(0, avisos[0].diasRestantes)
    }

    @Test
    fun sinAncla_backendAntiguo_seComportaComoAntes() {
        val s = sub(renewal = "2026-11-09", ancla = null)
        assertEquals(LocalDate(2026, 11, 9), proximaRenovacion(s, LocalDate(2026, 10, 9)))
    }

    // ── Formulario: reenviar el ancla ────────────────────────────────────────

    @Test
    fun editarSinTocarLaFecha_reenviaElAncla() = runTest {
        val (vm, cuerpo) = vmCon(LocalDate(2026, 11, 10))
        vm.cargarParaEditar(sub(renewal = "2026-11-30", ancla = "2026-01-31"))
        assertEquals("2026-11-30", vm.fechaRenovacion.value)
        vm.nombre.value = "Netflix Premium"
        enviarYEsperar(vm)
        assertTrue("\"renewalDate\":\"2026-01-31\"" in cuerpo(), cuerpo())
    }

    @Test
    fun editarCambiandoLaFecha_enviaLaNueva() = runTest {
        val (vm, cuerpo) = vmCon(LocalDate(2026, 11, 10))
        vm.cargarParaEditar(sub(renewal = "2026-11-30", ancla = "2026-01-31"))
        vm.fechaRenovacion.value = "2026-11-20"
        enviarYEsperar(vm)
        assertTrue("\"renewalDate\":\"2026-11-20\"" in cuerpo(), cuerpo())
    }

    @Test
    fun editarCambiandoElPeriodo_enviaLaFechaMostrada() = runTest {
        val (vm, cuerpo) = vmCon(LocalDate(2026, 11, 10))
        vm.cargarParaEditar(sub(renewal = "2026-11-30", ancla = "2026-01-31"))
        vm.periodoFacturacion.value = "YEARLY"
        enviarYEsperar(vm)
        assertTrue("\"renewalDate\":\"2026-11-30\"" in cuerpo(), cuerpo())
    }

    @Test
    fun editarSemanal_cargaLaProximaLocalYReenviaElAncla() = runTest {
        // Ancla jueves 1-ene; el servidor (por delante) ya proyectó al 22-oct; en el móvil es jueves 15-oct.
        val (vm, cuerpo) = vmCon(LocalDate(2026, 10, 15))
        vm.cargarParaEditar(sub(renewal = "2026-10-22", ancla = "2026-01-01", ciclo = "WEEKLY"))
        assertEquals("2026-10-15", vm.fechaRenovacion.value)
        enviarYEsperar(vm)
        assertTrue("\"renewalDate\":\"2026-01-01\"" in cuerpo(), cuerpo())
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private suspend fun enviarYEsperar(vm: SuscripcionFormViewModel) {
        vm.enviar(esEdicion = true, id = 7)
        vm.uiState.first { it !is FormUiState.Loading && it != FormUiState.Idle }
    }

    private fun vmCon(hoy: LocalDate): Pair<SuscripcionFormViewModel, () -> String> {
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
