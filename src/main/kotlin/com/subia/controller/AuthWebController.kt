package com.subia.controller

import com.subia.dto.RegisterRequest
import com.subia.exception.InvalidRegistrationException
import com.subia.exception.VerificationTokenException
import com.subia.exception.WeakPasswordException
import com.subia.exception.InvalidGoogleTokenException
import com.subia.security.GoogleOAuthService
import com.subia.security.UserDetailsServiceImpl
import com.subia.service.UserService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Controller
class AuthWebController(
    private val userService: UserService,
    private val googleOAuthService: GoogleOAuthService,
    private val userDetailsService: UserDetailsServiceImpl,
    @Value("\${app.google.client-id:}") private val googleClientId: String
) {
    private val log = LoggerFactory.getLogger(AuthWebController::class.java)

    // ── Login ─────────────────────────────────────────────────────────────

    @GetMapping("/login")
    fun loginPage(
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) logout: String?,
        @RequestParam(required = false) verified: String?,
        @RequestParam(required = false) passwordReset: String?,
        @RequestParam(required = false) expired: String?,
        model: Model
    ): String {
        if (error != null) model.addAttribute("error", true)
        if (logout != null) model.addAttribute("logout", true)
        if (verified != null) model.addAttribute("verified", true)
        if (passwordReset != null) model.addAttribute("passwordReset", true)
        if (expired != null) model.addAttribute("expired", true)
        // Antes la plantilla leía `@environment.getProperty(...)`, prohibido en Thymeleaf 3.1.3
        model.addAttribute("googleClientId", googleClientId)
        return "auth/login"
    }

    // ── Register ──────────────────────────────────────────────────────────

    @GetMapping("/register")
    fun registerPage(model: Model): String {
        model.addAttribute("registerRequest", RegisterRequest())
        return "auth/register"
    }

    @PostMapping("/register")
    fun register(
        @RequestParam email: String,
        @RequestParam password: String,
        @RequestParam confirmPassword: String
    ): String {
        return try {
            userService.register(email, password, confirmPassword)
            "redirect:/verify-pending"
        } catch (ex: WeakPasswordException) {
            "redirect:/register?error=weakPassword"
        } catch (ex: InvalidRegistrationException) {
            // error=invalidEmail | passwordMismatch (antes se ignoraba confirmPassword, M-10)
            "redirect:/register?error=${ex.code}"
        }
    }

    // ── Verify pending ────────────────────────────────────────────────────

    @GetMapping("/verify-pending")
    fun verifyPending(): String = "auth/verify-pending"

    /** Destino del enlace de verificación inválido o caducado (antes la ruta no existía → 404). */
    @GetMapping("/verify-email-error")
    fun verifyEmailError(): String = "auth/verify-email-error"

    // ── Verify email ──────────────────────────────────────────────────────

    @GetMapping("/verify-email")
    fun verifyEmail(@RequestParam token: String): String {
        return try {
            userService.verifyEmail(token)
            "redirect:/login?verified=true"
        } catch (ex: VerificationTokenException) {
            "redirect:/verify-email-error"
        }
    }

    // ── Forgot password ───────────────────────────────────────────────────

    @GetMapping("/forgot-password")
    fun forgotPasswordPage(
        @RequestParam(required = false) sent: String?,
        model: Model
    ): String {
        if (sent != null) model.addAttribute("sent", true)
        return "auth/forgot-password"
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestParam email: String): String {
        userService.initiatePasswordReset(email)
        return "redirect:/forgot-password?sent=true"
    }

    // ── Reset password ────────────────────────────────────────────────────

    @GetMapping("/reset-password")
    fun resetPasswordPage(
        @RequestParam token: String,
        @RequestParam(required = false) error: String?,
        model: Model
    ): String {
        model.addAttribute("token", token)
        // R3-04: un enlace inválido, usado o caducado se avisa al abrirlo, sin formulario (antes
        // el usuario escribía la contraseña dos veces y solo entonces veía el error).
        val tokenValid = userService.isResetTokenValid(token)
        model.addAttribute("tokenValid", tokenValid)
        when {
            !tokenValid -> model.addAttribute("error", "invalidToken")
            error != null -> model.addAttribute("error", error)
        }
        return "auth/reset-password"
    }

    @PostMapping("/reset-password")
    fun resetPassword(
        @RequestParam token: String,
        @RequestParam newPassword: String,
        @RequestParam confirmPassword: String
    ): String {
        // El token viene del formulario: se codifica para que no pueda inyectar otros parámetros
        // (p. ej. `abc&error=...`) en la URL de redirección.
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8)
        if (newPassword != confirmPassword) {
            return "redirect:/reset-password?token=$encodedToken&error=passwordMismatch"
        }
        return try {
            userService.resetPassword(token, newPassword)
            "redirect:/login?passwordReset=true"
        } catch (ex: VerificationTokenException) {
            "redirect:/reset-password?token=$encodedToken&error=invalidToken"
        } catch (ex: WeakPasswordException) {
            "redirect:/reset-password?token=$encodedToken&error=weakPassword"
        }
    }

    // ── Google OAuth web callback (Option B — establishes web session) ────

    @PostMapping("/auth/google/callback")
    fun googleCallback(
        @RequestParam credential: String,
        request: HttpServletRequest
    ): String {
        return try {
            val payload = googleOAuthService.verifyAndGetPayload(credential)
            val email = payload.email
            val googleId = payload.subject
            // Ignora el bloqueo por intentos de contraseña y reinicia el contador (ronda 5)
            val user = userService.loginWithGoogle(email, googleId)

            val userDetails = userDetailsService.loadUserForGoogle(user.email)
            val auth = UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.authorities
            )
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = auth
            SecurityContextHolder.setContext(context)

            // Anti session fixation: `sessionFixation().newSession()` del SecurityConfig solo
            // actúa en el formLogin; aquí poblamos la sesión a mano, así que rotamos el id
            // antes de guardar el contexto para que una sesión pre-autenticación no sirva.
            val session = request.getSession(true)
            request.changeSessionId()
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
            )

            "redirect:/dashboard"
        } catch (ex: InvalidGoogleTokenException) {
            "redirect:/login?error=googleFailed"
        } catch (ex: AuthenticationException) {
            // Cuenta sin verificar u otra causa (el bloqueo por intentos ya no aplica a Google): antes era un 500.
            log.info("Login web con Google rechazado: {}", ex.javaClass.simpleName)
            "redirect:/login?error=googleFailed"
        }
    }
}
