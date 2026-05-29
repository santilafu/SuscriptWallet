package com.subia.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.subia.model.CatalogItem
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Suscripción detectada al escanear el correo del usuario.
 *
 * @property serviceName  Nombre del servicio reconocido en el catálogo.
 * @property domain       Dominio del remitente que coincidió con el catálogo.
 * @property senderEmail  Dirección concreta desde la que llegó el correo.
 * @property lastSeen     Fecha del correo más reciente de ese servicio (yyyy-MM-dd).
 * @property catalogItem  Item del catálogo con precio/ciclo/categoría sugeridos.
 */
data class DetectedSubscription(
    val serviceName: String,
    val domain: String,
    val senderEmail: String,
    val lastSeen: String,
    val catalogItem: CatalogItem
) : java.io.Serializable

/**
 * Detecta suscripciones escaneando el correo del usuario vía la API de Gmail.
 *
 * Privacidad por diseño:
 * - Flujo OAuth puntual (`access_type=online`): NO se solicita ni se guarda refresh token.
 * - El access token se usa en memoria durante el escaneo y se descarta.
 * - Solo se leen las cabeceras `From` y `Date` (formato `metadata`); nunca el cuerpo del correo.
 * - No se persiste ningún dato del correo: solo se proponen al usuario los servicios reconocidos.
 *
 * Requiere un OAuth Client de tipo "Web" con su secret y el scope `gmail.readonly`
 * (ver `app.google.client-id` / `app.google.client-secret`).
 */
@Service
class GmailScanService(
    private val catalogService: CatalogService,
    @Value("\${app.google.client-id:}") private val clientId: String,
    @Value("\${app.google.client-secret:}") private val clientSecret: String,
    @Value("\${app.base-url:http://localhost:8081}") private val baseUrl: String
) {
    private val log = LoggerFactory.getLogger(GmailScanService::class.java)
    private val rest = RestClient.create()

    private val scope = "https://www.googleapis.com/auth/gmail.readonly"
    private val redirectUri: String get() = "$baseUrl/oauth/gmail/callback"

    /** true si hay client-id y secret configurados (sin secret no se puede hacer el intercambio de código). */
    val isConfigured: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    /** Construye la URL de consentimiento de Google a la que redirigir al usuario. */
    fun buildAuthorizationUrl(state: String): String =
        UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", scope)
            .queryParam("access_type", "online")
            .queryParam("include_granted_scopes", "true")
            .queryParam("prompt", "consent")
            .queryParam("state", state)
            .build(true)
            .toUriString()

    /** Intercambia el código de autorización por un access token de corta duración. */
    fun exchangeCodeForToken(code: String): String {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("code", code)
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("redirect_uri", redirectUri)
            add("grant_type", "authorization_code")
        }
        val resp = rest.post()
            .uri("https://oauth2.googleapis.com/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TokenResponse::class.java)
        return resp?.accessToken
            ?: throw IllegalStateException("Google no devolvió un access token")
    }

    /**
     * Escanea los correos de facturación del último año y devuelve los servicios del catálogo
     * detectados por su remitente (uno por servicio, el más reciente).
     */
    fun scan(accessToken: String): List<DetectedSubscription> {
        val byDomain: Map<String, CatalogItem> = catalogService.getAllItems()
            .filter { !it.domain.isNullOrBlank() }
            .groupBy { it.domain!!.lowercase() }
            .mapValues { (_, items) -> items.first() }

        val query = "newer_than:1y (subscription OR receipt OR invoice OR payment OR renewal OR " +
            "factura OR suscripción OR suscripcion OR pago OR renovación OR renovacion)"

        val listUri = UriComponentsBuilder.fromUriString("https://gmail.googleapis.com/gmail/v1/users/me/messages")
            .queryParam("q", query)
            .queryParam("maxResults", 100)
            .build(true)
            .toUriString()

        val list = rest.get().uri(listUri)
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body(MessageListResponse::class.java)

        val detected = LinkedHashMap<String, DetectedSubscription>()
        for (ref in list?.messages.orEmpty()) {
            if (detected.size >= 40) break
            val msg = try {
                rest.get()
                    .uri("https://gmail.googleapis.com/gmail/v1/users/me/messages/${ref.id}" +
                        "?format=metadata&metadataHeaders=From&metadataHeaders=Date")
                    .header("Authorization", "Bearer $accessToken")
                    .retrieve()
                    .body(MessageResponse::class.java)
            } catch (ex: Exception) {
                log.debug("No se pudo leer el mensaje {}: {}", ref.id, ex.message)
                null
            } ?: continue

            val from = msg.payload?.headers
                ?.firstOrNull { it.name.equals("From", ignoreCase = true) }?.value ?: continue
            val email = extractEmail(from)
            val host = email.substringAfter('@', "").lowercase()
            if (host.isBlank()) continue

            val match = byDomain.entries.firstOrNull { (dom, _) ->
                host == dom || host.endsWith(".$dom")
            } ?: continue

            // Conservamos solo el primer (más reciente) correo por servicio: la lista de Gmail
            // viene ordenada de más nuevo a más antiguo.
            detected.putIfAbsent(
                match.key,
                DetectedSubscription(
                    serviceName = match.value.name,
                    domain = match.key,
                    senderEmail = email,
                    lastSeen = epochMillisToDate(msg.internalDate),
                    catalogItem = match.value
                )
            )
        }
        log.info("Escaneo Gmail: {} servicios detectados", detected.size)
        return detected.values.toList()
    }

    /** Extrae la dirección de un header From del tipo `Nombre <correo@dominio>` o `correo@dominio`. */
    private fun extractEmail(from: String): String {
        val lt = from.indexOf('<')
        val gt = from.indexOf('>')
        return if (lt in 0 until gt) from.substring(lt + 1, gt).trim() else from.trim()
    }

    private fun epochMillisToDate(millis: String?): String =
        millis?.toLongOrNull()?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } ?: ""

    // ── DTOs de respuesta de Google/Gmail (solo los campos que usamos) ──────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TokenResponse(
        @com.fasterxml.jackson.annotation.JsonProperty("access_token") val accessToken: String?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MessageListResponse(val messages: List<MessageRef>? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MessageRef(val id: String = "")

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MessageResponse(val internalDate: String? = null, val payload: Payload? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Payload(val headers: List<Header>? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Header(val name: String = "", val value: String = "")
}
