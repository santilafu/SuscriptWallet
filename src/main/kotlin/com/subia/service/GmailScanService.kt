package com.subia.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.subia.model.BillingCycle
import com.subia.model.CatalogItem
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * Suscripción detectada al escanear el correo del usuario.
 *
 * Los campos `email*` contienen el precio/ciclo/moneda inferidos del cuerpo del correo
 * (cuando ha sido posible). Si no se pudo inferir, valen `null` y se usa el dato del catálogo.
 * Los getters `effective*` resuelven ese "precio del correo si existe, si no el del catálogo".
 *
 * @property serviceName  Nombre del servicio reconocido en el catálogo.
 * @property domain       Dominio del remitente que coincidió con el catálogo.
 * @property senderEmail  Dirección concreta desde la que llegó el correo.
 * @property lastSeen     Fecha del correo más reciente de ese servicio (yyyy-MM-dd).
 * @property catalogItem  Item del catálogo con precio/ciclo/categoría sugeridos.
 * @property emailPrice   Precio detectado en el cuerpo del correo (null si no se pudo extraer).
 * @property emailCurrency Código ISO de la moneda detectada en el correo (null si no se pudo).
 * @property emailCycle   Ciclo de facturación detectado en el correo (null si no se pudo).
 */
data class DetectedSubscription(
    val serviceName: String,
    val domain: String,
    val senderEmail: String,
    val lastSeen: String,
    val catalogItem: CatalogItem,
    val emailPrice: BigDecimal? = null,
    val emailCurrency: String? = null,
    val emailCycle: BillingCycle? = null
) : java.io.Serializable {
    /** Precio a proponer: el detectado en el correo si existe, si no el del catálogo. */
    val effectivePrice: BigDecimal get() = emailPrice ?: catalogItem.price
    /** Moneda a proponer: la detectada en el correo si existe, si no la del catálogo. */
    val effectiveCurrency: String get() = emailCurrency ?: catalogItem.currency
    /** Ciclo a proponer: el detectado en el correo si existe, si no el del catálogo. */
    val effectiveCycle: BillingCycle get() = emailCycle ?: catalogItem.billingCycle
    /** true si el precio propuesto proviene del propio correo (y no del catálogo). */
    val priceFromEmail: Boolean get() = emailPrice != null
}

/** Motivo por el que ha fallado un escaneo, para mostrar un mensaje adecuado al usuario. */
enum class GmailScanError { TOKEN, SCOPE, RATE, GENERIC }

/** Excepción de dominio del escaneo, con el motivo ya clasificado. */
class GmailScanException(val reason: GmailScanError, cause: Throwable? = null) :
    RuntimeException("Escaneo de Gmail fallido: $reason", cause)

/**
 * Detecta suscripciones escaneando el correo del usuario vía la API de Gmail.
 *
 * Privacidad por diseño:
 * - Flujo OAuth puntual (`access_type=online`): NO se solicita ni se guarda refresh token.
 * - El access token se usa en memoria durante el escaneo y se descarta.
 * - Se leen las cabeceras `From`/`Date` de los correos de facturación y, SOLO para los
 *   remitentes que coinciden con un servicio conocido, su cuerpo, con el único fin de
 *   estimar el precio real. Nada de eso se persiste: solo se proponen al usuario los
 *   servicios reconocidos con su precio estimado.
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
        val resp = try {
            rest.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse::class.java)
        } catch (ex: RestClientResponseException) {
            log.warn("Google rechazó el intercambio de código ({}): {}", ex.statusCode, ex.responseBodyAsString)
            throw GmailScanException(GmailScanError.TOKEN, ex)
        }
        return resp?.accessToken ?: throw GmailScanException(GmailScanError.TOKEN)
    }

    /**
     * Escanea los correos de facturación de los últimos [months] meses y devuelve los servicios
     * del catálogo detectados por su remitente (uno por servicio, el más reciente). Para cada
     * servicio reconocido intenta además estimar el precio real leyendo el cuerpo del correo.
     *
     * @throws GmailScanException con el motivo clasificado si la API de Gmail falla.
     */
    fun scan(accessToken: String, months: Int = DEFAULT_MONTHS): List<DetectedSubscription> {
        val window = months.coerceIn(1, MAX_MONTHS)
        val byDomain: Map<String, CatalogItem> = catalogService.getAllItems()
            .filter { !it.domain.isNullOrBlank() }
            .groupBy { it.domain!!.lowercase() }
            .mapValues { (_, items) -> items.first() }

        val query = "newer_than:${window}m (subscription OR receipt OR invoice OR payment OR renewal OR " +
            "billing OR \"your plan\" OR factura OR recibo OR suscripción OR suscripcion OR pago OR " +
            "cobro OR renovación OR renovacion OR \"tu plan\")"

        val refs = collectMessageRefs(accessToken, query)

        var processed = 0
        val detected = LinkedHashMap<String, DetectedSubscription>()
        for (ref in refs) {
            if (detected.size >= MAX_DETECTIONS) break
            // Paso 1: solo cabeceras (barato) para reconocer el remitente.
            val meta = fetchMessage(accessToken, ref.id, full = false) ?: continue
            processed++

            val from = meta.payload?.headers
                ?.firstOrNull { it.name.equals("From", ignoreCase = true) }?.value ?: continue
            val email = extractEmail(from)
            val host = email.substringAfter('@', "").lowercase()
            if (host.isBlank()) continue

            val match = matchDomain(host, byDomain) ?: continue
            // Conservamos solo el primer (más reciente) correo por servicio: la lista de Gmail
            // viene ordenada de más nuevo a más antiguo.
            if (detected.containsKey(match.key)) continue

            // Paso 2: SOLO en los correos reconocidos descargamos el cuerpo para estimar el precio.
            val price = fetchMessage(accessToken, ref.id, full = true)?.let { extractPrice(it) }
            detected[match.key] = DetectedSubscription(
                serviceName = match.value.name,
                domain = match.key,
                senderEmail = email,
                lastSeen = epochMillisToDate(meta.internalDate),
                catalogItem = match.value,
                emailPrice = price?.amount,
                emailCurrency = price?.currency,
                emailCycle = price?.cycle
            )
        }
        log.info("Escaneo Gmail ({}m): {} correos leídos, {} servicios detectados", window, processed, detected.size)
        return detected.values.toList()
    }

    /**
     * Casa el host del remitente contra el catálogo: coincidencia exacta, por subdominio
     * (`mail.netflix.com` → `netflix.com`) o por dominio registrable (recorta subdominios).
     */
    private fun matchDomain(host: String, byDomain: Map<String, CatalogItem>): Map.Entry<String, CatalogItem>? {
        byDomain[host]?.let { return mapEntry(host, it) }
        val direct = byDomain.entries.firstOrNull { (dom, _) -> host == dom || host.endsWith(".$dom") }
        if (direct != null) return direct
        // último recurso: el dominio registrable aproximado (dos últimas etiquetas)
        val registrable = registrableDomain(host)
        return byDomain[registrable]?.let { mapEntry(registrable, it) }
    }

    private fun mapEntry(key: String, value: CatalogItem): Map.Entry<String, CatalogItem> =
        java.util.AbstractMap.SimpleEntry(key, value)

    /** Aproxima el dominio registrable quedándose con las dos últimas etiquetas (netflix.com). */
    private fun registrableDomain(host: String): String {
        val parts = host.split('.')
        return if (parts.size >= 2) parts.takeLast(2).joinToString(".") else host
    }

    /** Lista referencias de mensajes paginando hasta [MAX_MESSAGES] o agotar resultados. */
    private fun collectMessageRefs(accessToken: String, query: String): List<MessageRef> {
        val refs = ArrayList<MessageRef>()
        var pageToken: String? = null
        try {
            do {
                val uri = UriComponentsBuilder.fromUriString("https://gmail.googleapis.com/gmail/v1/users/me/messages")
                    .queryParam("q", query)
                    .queryParam("maxResults", PAGE_SIZE)
                    .apply { if (pageToken != null) queryParam("pageToken", pageToken) }
                    .build(true)
                    .toUriString()
                val page = rest.get().uri(uri)
                    .header("Authorization", "Bearer $accessToken")
                    .retrieve()
                    .body(MessageListResponse::class.java)
                refs.addAll(page?.messages.orEmpty())
                pageToken = page?.nextPageToken
            } while (pageToken != null && refs.size < MAX_MESSAGES)
        } catch (ex: RestClientResponseException) {
            throw classify(ex)
        }
        return refs
    }

    /**
     * Pide un mensaje a Gmail. Con [full]=false solo trae las cabeceras `From`/`Date` (barato, para
     * reconocer el remitente); con [full]=true trae el cuerpo (para estimar el precio).
     * Un fallo puntual de un mensaje no aborta el escaneo.
     */
    private fun fetchMessage(accessToken: String, id: String, full: Boolean): MessageResponse? =
        try {
            val format = if (full) "format=full"
                         else "format=metadata&metadataHeaders=From&metadataHeaders=Date"
            rest.get()
                .uri("https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?$format")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(MessageResponse::class.java)
        } catch (ex: RestClientResponseException) {
            // 401/403 sí son fatales (token/scope); el resto se ignora para ese mensaje.
            if (ex.statusCode.value() == 401 || ex.statusCode.value() == 403) throw classify(ex)
            log.debug("No se pudo leer el mensaje {}: {}", id, ex.message)
            null
        } catch (ex: Exception) {
            log.debug("No se pudo leer el mensaje {}: {}", id, ex.message)
            null
        }

    /** Traduce un error HTTP de Gmail al motivo de dominio correspondiente. */
    private fun classify(ex: RestClientResponseException): GmailScanException {
        val reason = when (ex.statusCode.value()) {
            401 -> GmailScanError.TOKEN
            403 -> GmailScanError.SCOPE
            429 -> GmailScanError.RATE
            else -> GmailScanError.GENERIC
        }
        log.warn("Gmail API respondió {} → {}: {}", ex.statusCode, reason, ex.responseBodyAsString.take(300))
        return GmailScanException(reason, ex)
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

    // ── Extracción de precio del cuerpo ─────────────────────────────────────────

    /** Precio estimado a partir del cuerpo de un correo. */
    private data class ExtractedPrice(val amount: BigDecimal, val currency: String, val cycle: BillingCycle?)

    /** Símbolo/código → moneda ISO. */
    private val currencyOf = mapOf(
        "€" to "EUR", "eur" to "EUR",
        "$" to "USD", "usd" to "USD",
        "£" to "GBP", "gbp" to "GBP"
    )

    // Captura "€12,99", "12.99 €", "EUR 9.99", "9,99 USD"… (símbolo antes o después del número).
    private val priceRegex = Regex(
        """(€|\$|£|EUR|USD|GBP)\s?(\d{1,4}(?:[.,]\d{1,2})?)|(\d{1,4}(?:[.,]\d{1,2})?)\s?(€|\$|£|EUR|USD|GBP)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Recorre las partes del mensaje buscando texto y devuelve el primer precio plausible.
     * Devuelve null si no encuentra ninguno (entonces se usa el precio del catálogo).
     */
    private fun extractPrice(msg: MessageResponse): ExtractedPrice? {
        val text = bodyText(msg.payload).ifBlank { return null }
        val match = priceRegex.find(text) ?: return null
        val (symA, numA, numB, symB) = match.destructured
        val rawNum = numA.ifBlank { numB }
        val rawSym = symA.ifBlank { symB }.lowercase()
        val amount = parseAmount(rawNum) ?: return null
        if (amount <= BigDecimal.ZERO || amount > MAX_PLAUSIBLE_PRICE) return null
        val currency = currencyOf[rawSym] ?: return null
        return ExtractedPrice(amount, currency, detectCycle(text))
    }

    /** Normaliza "12,99"/"12.99" a BigDecimal (coma como separador decimal europeo). */
    private fun parseAmount(raw: String): BigDecimal? =
        raw.replace(',', '.').toBigDecimalOrNull()

    /** Deduce el ciclo a partir de palabras del cuerpo; null si no es claro. */
    private fun detectCycle(text: String): BillingCycle? {
        val t = text.lowercase()
        return when {
            Regex("""(/\s?(año|ano|year)|anual|annual|yearly|al año|per year)""").containsMatchIn(t) -> BillingCycle.YEARLY
            Regex("""(/\s?(semana|week)|semanal|weekly|per week)""").containsMatchIn(t) -> BillingCycle.WEEKLY
            Regex("""(/\s?(mes|month)|mensual|monthly|al mes|per month)""").containsMatchIn(t) -> BillingCycle.MONTHLY
            else -> null
        }
    }

    /** Concatena el texto plano (o HTML como fallback) de un payload MIME, decodificando base64url. */
    private fun bodyText(payload: Payload?): String {
        payload ?: return ""
        val sb = StringBuilder()
        fun walk(p: Payload, depth: Int) {
            if (depth > 8 || sb.length > MAX_BODY_CHARS) return
            val mime = p.mimeType ?: ""
            if (mime.startsWith("text/")) {
                p.body?.data?.let { sb.append(decodeBase64Url(it)).append('\n') }
            }
            p.parts?.forEach { walk(it, depth + 1) }
        }
        walk(payload, 0)
        // Si solo había HTML, quitamos etiquetas para que la regex vea el texto.
        return sb.toString().replace(Regex("<[^>]+>"), " ").take(MAX_BODY_CHARS)
    }

    private fun decodeBase64Url(data: String): String =
        try {
            String(Base64.getUrlDecoder().decode(data), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            ""
        }

    // ── DTOs de respuesta de Google/Gmail (solo los campos que usamos) ──────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TokenResponse(
        @com.fasterxml.jackson.annotation.JsonProperty("access_token") val accessToken: String?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MessageListResponse(
        val messages: List<MessageRef>? = null,
        val nextPageToken: String? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MessageRef(val id: String = "")

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MessageResponse(val internalDate: String? = null, val payload: Payload? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Payload(
        val mimeType: String? = null,
        val headers: List<Header>? = null,
        val body: Body? = null,
        val parts: List<Payload>? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Body(val data: String? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Header(val name: String = "", val value: String = "")

    companion object {
        /** Rango de escaneo por defecto, en meses. */
        const val DEFAULT_MONTHS = 12
        /** Rango máximo permitido, en meses. */
        const val MAX_MONTHS = 24
        /** Resultados por página al listar mensajes. */
        private const val PAGE_SIZE = 100
        /** Tope de referencias de mensajes a recorrer (varias páginas). */
        private const val MAX_MESSAGES = 300
        /** Tope de servicios distintos a proponer. */
        private const val MAX_DETECTIONS = 100
        /** Precio por encima del cual se descarta como falso positivo. */
        private val MAX_PLAUSIBLE_PRICE = BigDecimal("10000")
        /** Tope de caracteres de cuerpo a inspeccionar por correo. */
        private const val MAX_BODY_CHARS = 20_000
    }
}
