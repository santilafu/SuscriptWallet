package com.subia.shared

import com.subia.shared.model.ApiResponse
import com.subia.shared.model.AuthTokens
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiRoutes
import com.subia.shared.network.SessionExpiredException
import com.subia.shared.storage.TokenStorageProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pruebas de integración para [ApiClient] con servidor mock de Ktor.
 *
 * Verifica el comportamiento ante respuestas 401: el cliente debe intentar refrescar
 * el token y reintentar la petición original exactamente una vez.
 * Especificación: TEST-02 — token refresh tras 401.
 */
class ApiClientTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    // -------------------------------------------------------------------------------------
    // TEST-02: 401 → token refresh → reintento con nuevo token
    // -------------------------------------------------------------------------------------

    /**
     * Verifica que ante un 401 el cliente:
     * 1. Intenta refrescar el token vía POST /api/auth/refresh.
     * 2. Reintenta la petición original con el nuevo token.
     * 3. Devuelve el resultado de la segunda petición exitosa.
     *
     * Especificación: TEST-02 — ApiClient retries after 401.
     */
    @Test
    fun apiClient_tras401_refresca_token_y_reintenta() = runTest {
        var llamadasAlEndpointPrincipal = 0
        var refreshCalled = false

        val nuevoToken = AuthTokens(accessToken = "nuevo-access-token", refreshToken = "nuevo-refresh-token")
        val refreshResponse = ApiResponse(data = nuevoToken)
        val dataResponse = ApiResponse(data = "éxito")

        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath == ApiRoutes.REFRESH -> {
                    refreshCalled = true
                    respond(
                        content = json.encodeToString(refreshResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                llamadasAlEndpointPrincipal++ == 0 -> {
                    // Primera llamada al endpoint principal → 401 Unauthorized
                    respondError(HttpStatusCode.Unauthorized)
                }
                else -> {
                    // Segunda llamada (reintento tras refresh) → 200 OK
                    respond(
                        content = json.encodeToString(dataResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
        }

        val tokenStorage = InMemoryTokenStorage(
            initialTokens = AuthTokens(accessToken = "token-viejo", refreshToken = "refresh-viejo")
        )
        val client = ApiClient(
            baseUrl = "http://localhost",
            tokenStorage = tokenStorage,
            isDebug = false,
            httpEngine = mockEngine
        )

        val resultado = client.get<String>("/api/test")

        assertTrue(resultado.isSuccess, "La petición debe tener éxito tras el reintento: ${resultado.exceptionOrNull()?.message}")
        assertEquals("éxito", resultado.getOrNull())
        assertTrue(refreshCalled, "El endpoint de refresh debe haberse llamado")
        assertEquals(2, llamadasAlEndpointPrincipal, "El endpoint principal debe haberse llamado 2 veces: 401 + reintento")
        assertEquals("nuevo-access-token", tokenStorage.getTokens()?.accessToken,
            "El nuevo access token debe estar almacenado")
    }

    /**
     * Verifica que si el refresh también falla (refresh token inválido),
     * el cliente devuelve [SessionExpiredException].
     */
    @Test
    fun apiClient_siRefreshFalla_devuelve_SessionExpiredException() = runTest {
        val mockEngine = MockEngine { _ ->
            respondError(HttpStatusCode.Unauthorized)
        }

        val tokenStorage = InMemoryTokenStorage(
            initialTokens = AuthTokens(accessToken = "token-caducado", refreshToken = "refresh-caducado")
        )
        val client = ApiClient(
            baseUrl = "http://localhost",
            tokenStorage = tokenStorage,
            isDebug = false,
            httpEngine = mockEngine
        )

        val resultado = client.get<String>("/api/test")

        assertTrue(resultado.isFailure)
        assertTrue(
            resultado.exceptionOrNull() is SessionExpiredException,
            "Debe lanzar SessionExpiredException cuando el refresh también falla, " +
            "pero fue: ${resultado.exceptionOrNull()?.javaClass?.simpleName}"
        )
    }

    /**
     * Verifica que si no hay tokens en el almacenamiento, se devuelve
     * [SessionExpiredException] directamente sin realizar ninguna petición de red.
     */
    @Test
    fun apiClient_sinTokens_devuelve_SessionExpiredException_sinLlamadaDeRed() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine { _ ->
            requestCount++
            respond("", HttpStatusCode.OK)
        }

        val tokenStorage = InMemoryTokenStorage(initialTokens = null)
        val client = ApiClient(
            baseUrl = "http://localhost",
            tokenStorage = tokenStorage,
            isDebug = false,
            httpEngine = mockEngine
        )

        val resultado = client.get<String>("/api/test")

        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull() is SessionExpiredException)
        assertEquals(0, requestCount, "No debe realizarse ninguna petición de red si no hay tokens")
    }
}

/**
 * Implementación de [TokenStorageProvider] en memoria para tests.
 * No accede a EncryptedSharedPreferences ni a Keychain — puro Kotlin.
 *
 * @param initialTokens Tokens con los que se inicializa el almacenamiento,
 *                      o `null` si está vacío.
 */
private class InMemoryTokenStorage(private var tokens: AuthTokens?) : TokenStorageProvider {
    override fun getTokens(): AuthTokens? = tokens
    override fun hasTokens(): Boolean = tokens != null
    override fun saveTokens(newTokens: AuthTokens) { tokens = newTokens }
    override fun clearTokens() { tokens = null }
}
