package com.subia.shared.network

import com.subia.shared.model.AuthTokens
import com.subia.shared.model.RefreshRequest
import com.subia.shared.platform.createHttpEngine
import com.subia.shared.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Cliente HTTP centralizado con inyección de token Bearer y refresco automático ante 401.
 * Usa un [Mutex] para serializar el refresco y evitar condiciones de carrera cuando varias
 * corrutinas reciben 401 simultáneamente.
 */
class ApiClient(
    private val baseUrl: String,
    private val tokenStorage: TokenStorage
) {
    private val refreshMutex = Mutex()

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val client = HttpClient(createHttpEngine()) {
        install(ContentNegotiation) { json(jsonConfig) }
        install(Logging) {
            level = LogLevel.INFO
            logger = Logger.DEFAULT
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
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<T>()
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
            return Result.failure(NetworkException(e.message ?: "Error de red"))
        }

        if (response.status == HttpStatusCode.Unauthorized) {
            val refreshed = refreshMutex.withLock {
                if (!tokenStorage.hasTokens()) return Result.failure(SessionExpiredException())
                tryRefresh(tokenStorage.getTokens()!!.refreshToken)
            }
            if (!refreshed) return Result.failure(SessionExpiredException())

            val retried = try { block() } catch (e: Exception) {
                return Result.failure(NetworkException(e.message ?: "Error de red"))
            }
            return parseResponse(retried)
        }

        return parseResponse(response)
    }

    private suspend fun tryRefresh(refreshToken: String): Boolean {
        return try {
            val tokens: AuthTokens = client.post(ApiRoutes.REFRESH) {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }.body()
            tokenStorage.saveTokens(tokens)
            true
        } catch (e: Exception) {
            tokenStorage.clearTokens()
            false
        }
    }

    @PublishedApi
    internal suspend inline fun <reified T> parseResponse(response: HttpResponse): Result<T> =
        if (response.status.isSuccess()) {
            runCatching { response.body<T>() }
        } else {
            Result.failure(ApiException(response.status.value, response.status.description))
        }
}

/** El refresh token no es válido o ha caducado — el usuario debe volver a iniciar sesión. */
class SessionExpiredException : Exception("Sesión expirada. Por favor, inicia sesión de nuevo.")

/** Error de conectividad o timeout. */
class NetworkException(message: String) : Exception(message)

/** El servidor devolvió un código de error HTTP. */
class ApiException(val statusCode: Int, message: String) : Exception("Error $statusCode: $message")
