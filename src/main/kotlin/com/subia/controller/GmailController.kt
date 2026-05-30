package com.subia.controller

import com.subia.model.Subscription
import com.subia.repository.UserRepository
import com.subia.service.CategoryService
import com.subia.service.DetectedSubscription
import com.subia.service.GmailScanError
import com.subia.service.GmailScanException
import com.subia.service.GmailScanService
import com.subia.service.SubscriptionService
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.time.LocalDate
import java.util.Base64

/**
 * Controlador del escaneo de Gmail para detectar suscripciones.
 *
 * Flujo OAuth (authorization code, puntual):
 *   /gmail/connect  → redirige a Google (consentimiento gmail.readonly)
 *   /oauth/gmail/callback → recibe el code, escanea y guarda los resultados en sesión
 *   /gmail/results  → muestra los servicios detectados para que el usuario los añada
 *   /gmail/add-selected → da de alta en bloque las suscripciones marcadas
 *
 * El parámetro `state` se valida contra la sesión para prevenir CSRF en el flujo OAuth.
 * No se guarda ningún token ni contenido de correo (ver [GmailScanService]).
 */
@Controller
class GmailController(
    private val gmailScanService: GmailScanService,
    private val categoryService: CategoryService,
    private val subscriptionService: SubscriptionService,
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(GmailController::class.java)
    private val random = SecureRandom()

    /**
     * Inicia el flujo: genera el state, guarda en sesión el state y el rango (meses) elegido,
     * y redirige al consentimiento de Google.
     */
    @GetMapping("/gmail/connect")
    fun connect(
        @RequestParam(required = false, defaultValue = "12") months: Int,
        session: HttpSession
    ): String {
        if (!gmailScanService.isConfigured) {
            return "redirect:/settings?gmailError=notconfigured"
        }
        val state = newState()
        session.setAttribute(STATE_ATTR, state)
        session.setAttribute(MONTHS_ATTR, months.coerceIn(1, GmailScanService.MAX_MONTHS))
        return "redirect:" + gmailScanService.buildAuthorizationUrl(state)
    }

    /** Callback de Google: valida el state, intercambia el code, escanea y redirige a resultados. */
    @GetMapping("/oauth/gmail/callback")
    fun callback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) error: String?,
        session: HttpSession
    ): String {
        val expected = session.getAttribute(STATE_ATTR) as? String
        session.removeAttribute(STATE_ATTR)
        val months = (session.getAttribute(MONTHS_ATTR) as? Int) ?: GmailScanService.DEFAULT_MONTHS
        session.removeAttribute(MONTHS_ATTR)

        if (error != null || code.isNullOrBlank() || state.isNullOrBlank() || state != expected) {
            log.warn("Callback de Gmail rechazado (error={}, stateValido={})", error, state == expected)
            return "redirect:/settings?gmailError=denied"
        }
        return try {
            val token = gmailScanService.exchangeCodeForToken(code)
            val detected = gmailScanService.scan(token, months)
            session.setAttribute(RESULT_ATTR, ArrayList(detected))
            "redirect:/gmail/results"
        } catch (ex: GmailScanException) {
            log.error("Escaneo Gmail fallido ({})", ex.reason, ex)
            "redirect:/settings?gmailError=${ex.reason.paramValue()}"
        } catch (ex: Exception) {
            log.error("Error inesperado escaneando Gmail: {}", ex.message, ex)
            "redirect:/settings?gmailError=scan"
        }
    }

    /** Muestra los servicios detectados para que el usuario decida cuáles añadir. */
    @GetMapping("/gmail/results")
    @Suppress("UNCHECKED_CAST")
    fun results(session: HttpSession, model: Model): String {
        val detected = session.getAttribute(RESULT_ATTR) as? List<DetectedSubscription> ?: emptyList()
        model.addAttribute("detected", detected)
        model.addAttribute("categoryKeyToId", categoryKeyToId())
        return "gmail-results"
    }

    /**
     * Da de alta en bloque las suscripciones marcadas por el usuario.
     * POST /gmail/add-selected con `selected` = índices (sobre la lista guardada en sesión).
     *
     * Los datos se toman de la sesión (no de campos ocultos manipulables): para cada índice
     * se usa el precio/ciclo efectivos (los del correo si se detectaron, si no los del catálogo).
     */
    @PostMapping("/gmail/add-selected")
    @Suppress("UNCHECKED_CAST")
    fun addSelected(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(required = false) selected: List<Int>?,
        session: HttpSession
    ): String {
        val detected = session.getAttribute(RESULT_ATTR) as? List<DetectedSubscription> ?: emptyList()
        val indices = selected.orEmpty()
        if (detected.isEmpty() || indices.isEmpty()) {
            return "redirect:/gmail/results?added=0"
        }
        val userId = resolveUserId(userDetails)
        val categoryKeyToId = categoryKeyToId()

        var added = 0
        for (i in indices) {
            val d = detected.getOrNull(i) ?: continue
            val categoryId = categoryKeyToId[d.catalogItem.categoryKey] ?: continue
            val category = categoryService.findById(categoryId)
            subscriptionService.save(
                Subscription(
                    name = d.serviceName,
                    description = d.catalogItem.description,
                    price = d.effectivePrice,
                    currency = d.effectiveCurrency,
                    billingCycle = d.effectiveCycle,
                    renewalDate = LocalDate.now().plusMonths(1),
                    category = category,
                    active = true,
                    notes = ""
                ),
                userId
            )
            added++
        }
        // Vaciamos los resultados para no re-añadir al recargar y volvemos a la lista.
        session.removeAttribute(RESULT_ATTR)
        return "redirect:/subscriptions?imported=$added"
    }

    private fun resolveUserId(userDetails: UserDetails): Long =
        userRepository.findByEmail(userDetails.username)?.id
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")

    private fun newState(): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** Mapea la clave de categoría del catálogo al ID de la entidad JPA (mismo criterio que CatalogBrowserController). */
    private fun categoryKeyToId(): Map<String, Long> =
        categoryService.findAll().mapNotNull { cat ->
            val key = when (cat.name) {
                "IA"                 -> "ia"
                "Streaming"          -> "streaming"
                "Música"             -> "musica"
                "Software"           -> "software"
                "Cloud"              -> "cloud"
                "Gaming"             -> "gaming"
                "Seguridad"          -> "seguridad"
                "Noticias y Lectura" -> "noticias"
                "Salud y Deporte"    -> "salud"
                "Desarrollo"         -> "desarrollo"
                "Prueba gratuita"    -> "prueba"
                "Finanzas"           -> "finanzas"
                "Educación"          -> "educacion"
                "Creatividad y foto" -> "creatividad"
                "Citas y social"     -> "citas"
                else                 -> null
            }
            if (key != null) key to cat.id else null
        }.toMap()

    companion object {
        private const val STATE_ATTR = "gmailOauthState"
        private const val MONTHS_ATTR = "gmailScanMonths"
        private const val RESULT_ATTR = "gmailDetected"

        /** Valor del parámetro `gmailError` para cada motivo de fallo. */
        private fun GmailScanError.paramValue(): String = when (this) {
            GmailScanError.TOKEN -> "token"
            GmailScanError.SCOPE -> "scope"
            GmailScanError.RATE  -> "rate"
            GmailScanError.GENERIC -> "scan"
        }
    }
}
