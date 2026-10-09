package com.subia.shared.network

import com.subia.shared.model.ApiResponse
import com.subia.shared.model.AuthTokens
import com.subia.shared.model.RefreshRequest
import com.subia.shared.platform.createHttpEngine
import com.subia.shared.storage.TokenStorage
import com.subia.shared.storage.TokenStorageProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Cliente HTTP centralizado con inyección de token Bearer y refresco automático ante 401.
 * Usa un [Mutex] para serializar el refresco y evitar condiciones de carrera cuando varias
 * corrutinas reciben 401 simultáneamente.
 *
 * @param baseUrl    URL base del servidor (sin trailing slash).
 * @param tokenStorage Almacenamiento seguro de tokens JWT.
 * @param isDebug    Si es `true`, habilita el log HTTP (sólo para builds de depuración).
 *                   En `false` (producción) se desactiva para evitar exponer tokens JWT en logcat.
 * @param httpEngine Motor HTTP a usar. Si es `null`, se usa el motor nativo de la plataforma.
 *                   Pasar un [io.ktor.client.engine.mock.MockEngine] permite hacer pruebas unitarias sin red.
 */
class ApiClient(
    private val baseUrl: String,
    @PublishedApi internal val tokenStorage: TokenStorageProvider,
    private val isDebug: Boolean = false,
    httpEngine: HttpClientEngine? = null
) {
    @PublishedApi
    internal val refreshMutex = Mutex()

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @PublishedApi
    internal val client = HttpClient(httpEngine ?: createHttpEngine()) {
        install(ContentNegotiation) { json(jsonConfig) }
        // Sin timeouts una petición puede quedarse colgada para siempre (spinner eterno con
        // Render "dormido" o al escanear Gmail). Los fallos llegan como excepción → NetworkException.
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
        install(Logging) {
            level = if (isDebug) LogLevel.INFO else LogLevel.NONE
            logger = object : Logger { override fun log(message: String) = println(message) }
        }
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }

    // ---- Métodos públicos ----

    /** GET autenticado. Devuelve [Result] con el cuerpo deserializado o el error. */
    suspend inline fun <reified T> get(path: String): Result<T> =
        authenticatedRequest { client.get(path) { tokenStorage.getTokens()?.let { header(HttpHeaders.Authorization, "Bearer ${it.accessToken}") } } }

    /** POST con cuerpo. Si [authenticated] es false no adjunta token (p.ej. login). */
    suspend inline fun <reified T, reified B : Any> post(
        path: String,
        body: B,
        authenticated: Boolean = true
    ): Result<T> = if (authenticated) {
        authenticatedRequest {
            client.post(path) {
                tokenStorage.getTokens()?.let { header(HttpHeaders.Authorization, "Bearer ${it.accessToken}") }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    } else {
        runCatching {
            val resp = client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (resp.status == HttpStatusCode.NoContent) @Suppress("UNCHECKED_CAST") Unit as T
            else resp.body<ApiResponse<T>>().data ?: error("Respuesta vacía del servidor")
        }
    }

    /** PUT autenticado con cuerpo. */
    suspend inline fun <reified T, reified B : Any> put(path: String, body: B): Result<T> =
        authenticatedRequest {
            client.put(path) {
                tokenStorage.getTokens()?.let { header(HttpHeaders.Authorization, "Bearer ${it.accessToken}") }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }

    /** DELETE autenticado. */
    suspend fun delete(path: String): Result<Unit> =
        authenticatedRequest<Unit> {
            client.delete(path) {
                tokenStorage.getTokens()?.let { header(HttpHeaders.Authorization, "Bearer ${it.accessToken}") }
            }
        }

    // ---- Lógica interna ----

    @PublishedApi
    internal suspend inline fun <reified T> authenticatedRequest(
        crossinline block: suspend () -> HttpResponse
    ): Result<T> {
        if (!tokenStorage.hasTokens()) {
            return Result.failure(SessionExpiredException())
        }

        val response = try {
            block()
        } catch (e: Exception) {
            relanzarSiCancelada(e)
            return Result.failure(NetworkException(e.message ?: "Error de red"))
        }

        if (response.status == HttpStatusCode.Unauthorized) {
            val refresco = refreshMutex.withLock {
                if (!tokenStorage.hasTokens()) return Result.failure(SessionExpiredException())
                tryRefresh(tokenStorage.getTokens()!!.refreshToken)
            }
            when (refresco) {
                ResultadoRefresco.Renovado -> Unit
                ResultadoRefresco.Rechazado -> return Result.failure(SessionExpiredException())
                // Sin red no sabemos si la sesión sigue viva: se conservan los tokens y se
                // informa como fallo de red (no como sesión caducada, que llevaría al login).
                ResultadoRefresco.SinRed -> return Result.failure(NetworkException("Refresco sin red"))
            }

            val retried = try { block() } catch (e: Exception) {
                relanzarSiCancelada(e)
                return Result.failure(NetworkException(e.message ?: "Error de red"))
            }
            return parseResponse(retried)
        }

        return parseResponse(response)
    }

    /**
     * Intenta renovar los tokens. Solo los borra si el servidor RECHAZA el refresh (respuesta
     * no exitosa o sin tokens): un corte de red o la cancelación de la corrutina (p. ej. el
     * worker de avisos con timeout o reemplazado) no deben cerrar la sesión del usuario.
     */
    @PublishedApi
    internal suspend fun tryRefresh(refreshToken: String): ResultadoRefresco {
        val response = try {
            client.post(ApiRoutes.REFRESH) {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }
        } catch (e: Exception) {
            relanzarSiCancelada(e)
            return ResultadoRefresco.SinRed
        }
        if (!response.status.isSuccess()) {
            tokenStorage.clearTokens()
            return ResultadoRefresco.Rechazado
        }
        val tokens = try {
            response.body<ApiResponse<AuthTokens>>().data
        } catch (e: Exception) {
            relanzarSiCancelada(e)
            // Cuerpo cortado a medias o ilegible: no hay prueba de que la sesión no valga.
            return ResultadoRefresco.SinRed
        }
        if (tokens == null) {
            tokenStorage.clearTokens()
            return ResultadoRefresco.Rechazado
        }
        tokenStorage.saveTokens(tokens)
        return ResultadoRefresco.Renovado
    }

    /**
     * Propaga la cancelación de la corrutina en vez de tratarla como fallo de red. Se comprueba
     * el estado del job y no solo el tipo: algunos timeouts de Ktor/coroutines también son
     * CancellationException sin que nuestra corrutina esté cancelada (esos sí son "sin red").
     */
    @PublishedApi
    internal suspend fun relanzarSiCancelada(e: Exception) {
        if (e is CancellationException) currentCoroutineContext().ensureActive()
    }

    @PublishedApi
    internal suspend inline fun <reified T> parseResponse(response: HttpResponse): Result<T> {
        if (!response.status.isSuccess())
            return Result.failure(ApiException(response.status.value, response.status.description))
        if (response.status == HttpStatusCode.NoContent)
            @Suppress("UNCHECKED_CAST") return Result.success(Unit as T)
        return runCatching { response.body<ApiResponse<T>>().data ?: error("Respuesta vacía del servidor") }
    }
}

/** Resultado de intentar renovar los tokens con el refresh token. */
enum class ResultadoRefresco { Renovado, Rechazado, SinRed }

/** El refresh token no es válido o ha caducado — el usuario debe volver a iniciar sesión. */
class SessionExpiredException : Exception("Sesión expirada. Por favor, inicia sesión de nuevo.")

/** Error de conectividad o timeout. */
class NetworkException(message: String) : Exception(message)

/** El servidor devolvió un código de error HTTP. */
class ApiException(val statusCode: Int, message: String) : Exception("Error $statusCode: $message")
