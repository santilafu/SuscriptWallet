package com.subia.shared

import com.subia.shared.model.AuthTokens
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ROL_ADMIN
import com.subia.shared.network.esAdmin
import com.subia.shared.network.rolDeAccessToken
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.storage.TokenStorageProvider
import com.subia.shared.viewmodel.CategoriasUiState
import com.subia.shared.viewmodel.CategoriasViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ronda 2 de la auditoría Android (2026-10-10): rol de administrador leído del access token y
 * categorías que solo puede crear un admin.
 */
class Ronda2AuditoriaTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** JWT de prueba (firma falsa: la app no la verifica, solo lee el claim). */
    @OptIn(ExperimentalEncodingApi::class)
    private fun jwt(payload: String): String {
        val b64 = Base64.UrlSafe
        val cabecera = b64.encode("""{"alg":"HS256"}""".encodeToByteArray()).trimEnd('=')
        val cuerpo = b64.encode(payload.encodeToByteArray()).trimEnd('=')
        return "$cabecera.$cuerpo.firma"
    }

    @Test
    fun rolDeAccessToken_leeElClaimRole() {
        assertEquals(ROL_ADMIN, rolDeAccessToken(jwt("""{"iss":"subia","sub":"a@b.c","role":"ADMIN"}""")))
        // Payload con caracteres no ASCII (longitud que obliga a rellenar el base64url)
        assertEquals("USER", rolDeAccessToken(jwt("""{"sub":"usuario-ñ@b.c","role":"USER"}""")))
    }

    @Test
    fun rolDeAccessToken_sinClaimONoJwt_esNull() {
        assertNull(rolDeAccessToken(jwt("""{"sub":"a@b.c"}""")))
        assertNull(rolDeAccessToken("no-es-un-jwt"))
        assertNull(rolDeAccessToken("a.%%%.c"))
        assertNull(rolDeAccessToken(""))
    }

    @Test
    fun esAdmin_usaElRolDeLaRespuestaYSiFaltaElDelJwt() {
        val jwtAdmin = jwt("""{"role":"ADMIN"}""")
        val jwtUser = jwt("""{"role":"USER"}""")
        assertTrue(esAdmin(AuthTokens("x.y.z", "r", role = "ADMIN")))
        // El campo de la respuesta manda sobre el claim
        assertFalse(esAdmin(AuthTokens(jwtAdmin, "r", role = "USER")))
        // Backend sin `role` en la respuesta: respaldo en el JWT
        assertTrue(esAdmin(AuthTokens(jwtAdmin, "r")))
        assertFalse(esAdmin(AuthTokens(jwtUser, "r")))
        assertFalse(esAdmin(AuthTokens("opaco", "r")))
        assertFalse(esAdmin(null))
    }

    @Test
    fun authTokens_leeRoleDeLaRespuesta() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val conRol = json.decodeFromString<com.subia.shared.model.ApiResponse<AuthTokens>>(
            """{"data":{"accessToken":"a","refreshToken":"r","expiresInSeconds":900,"tokenType":"Bearer","role":"ADMIN"},"error":null}"""
        ).data
        assertEquals("ADMIN", conRol?.role)
        val sinRol = json.decodeFromString<com.subia.shared.model.ApiResponse<AuthTokens>>(
            """{"data":{"accessToken":"a","refreshToken":"r"}}"""
        ).data
        assertNull(sinRol?.role)
    }

    private fun categoriasVm(esAdmin: () -> Boolean): CategoriasViewModel {
        val engine = MockEngine { _ ->
            respond(
                """{"data":[{"id":1,"name":"Streaming","color":"#ff0000","icon":"tv"}]}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val api = ApiClient(baseUrl = "http://localhost", tokenStorage = Tokens, isDebug = false, httpEngine = engine)
        return CategoriasViewModel(CategoryRepository(api), esAdmin)
    }

    @Test
    fun categorias_usuarioNormal_noPuedeCrear() {
        val vm = categoriasVm(esAdmin = { false })
        assertFalse(vm.puedeCrear.value)
    }

    @Test
    fun categorias_admin_puedeCrear() {
        assertTrue(categoriasVm(esAdmin = { true }).puedeCrear.value)
    }

    @Test
    fun categorias_porDefecto_ocultaCrear() {
        val engine = MockEngine { _ -> respondError(HttpStatusCode.NotFound) }
        val api = ApiClient(baseUrl = "http://localhost", tokenStorage = Tokens, isDebug = false, httpEngine = engine)
        assertFalse(CategoriasViewModel(CategoryRepository(api)).puedeCrear.value)
    }

    @Test
    fun categorias_sesionCaducada_exponeSesionExpirada() {
        val api = ApiClient(baseUrl = "http://localhost", tokenStorage = SinTokens, isDebug = false,
            httpEngine = MockEngine { _ -> respondError(HttpStatusCode.Unauthorized) })
        val vm = CategoriasViewModel(CategoryRepository(api))
        assertTrue(vm.uiState.value is CategoriasUiState.SesionExpirada, "Estado: ${vm.uiState.value}")
    }

    private object Tokens : TokenStorageProvider {
        private val tokens = AuthTokens(accessToken = "a", refreshToken = "r")
        override fun saveTokens(tokens: AuthTokens) {}
        override fun getTokens(): AuthTokens = tokens
        override fun clearTokens() {}
        override fun hasTokens(): Boolean = true
    }

    private object SinTokens : TokenStorageProvider {
        override fun saveTokens(tokens: AuthTokens) {}
        override fun getTokens(): AuthTokens? = null
        override fun clearTokens() {}
        override fun hasTokens(): Boolean = false
    }
}
