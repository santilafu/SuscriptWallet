package com.subia.controller

import com.subia.service.CategoryService
import com.subia.service.DetectedSubscription
import com.subia.service.GmailScanService
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.security.SecureRandom
import java.util.Base64

/**
 * Controlador del escaneo de Gmail para detectar suscripciones.
 *
 * Flujo OAuth (authorization code, puntual):
 *   /gmail/connect  → redirige a Google (consentimiento gmail.readonly)
 *   /oauth/gmail/callback → recibe el code, escanea y guarda los resultados en sesión
 *   /gmail/results  → muestra los servicios detectados para que el usuario los añada
 *
 * El parámetro `state` se valida contra la sesión para prevenir CSRF en el flujo OAuth.
 * No se guarda ningún token ni contenido de correo (ver [GmailScanService]).
 */
@Controller
class GmailController(
    private val gmailScanService: GmailScanService,
    private val categoryService: CategoryService
) {
    private val log = LoggerFactory.getLogger(GmailController::class.java)
    private val random = SecureRandom()

    /** Inicia el flujo: genera el state, lo guarda en sesión y redirige al consentimiento de Google. */
    @GetMapping("/gmail/connect")
    fun connect(session: HttpSession): String {
        if (!gmailScanService.isConfigured) {
            return "redirect:/settings?gmailError=notconfigured"
        }
        val state = newState()
        session.setAttribute(STATE_ATTR, state)
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

        if (error != null || code.isNullOrBlank() || state.isNullOrBlank() || state != expected) {
            log.warn("Callback de Gmail rechazado (error={}, stateValido={})", error, state == expected)
            return "redirect:/settings?gmailError=denied"
        }
        return try {
            val token = gmailScanService.exchangeCodeForToken(code)
            val detected = gmailScanService.scan(token)
            session.setAttribute(RESULT_ATTR, ArrayList(detected))
            "redirect:/gmail/results"
        } catch (ex: Exception) {
            log.error("Error escaneando Gmail: {}", ex.message, ex)
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
        private const val RESULT_ATTR = "gmailDetected"
    }
}
