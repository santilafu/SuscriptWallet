package com.subia.controller

import com.subia.dto.RegisterRequest
import com.subia.exception.VerificationTokenException
import com.subia.exception.WeakPasswordException
import com.subia.exception.InvalidGoogleTokenException
import com.subia.security.GoogleOAuthService
import com.subia.security.UserDetailsServiceImpl
import com.subia.service.UserService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest

@Controller
class AuthWebController(
    private val userService: UserService,
    private val googleOAuthService: GoogleOAuthService,
    private val userDetailsService: UserDetailsServiceImpl
) {

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
        }
    }

    // ── Verify pending ────────────────────────────────────────────────────

    @GetMapping("/verify-pending")
    fun verifyPending(): String = "auth/verify-pending"

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
        if (error != null) model.addAttribute("error", error)
        return "auth/reset-password"
    }

    @PostMapping("/reset-password")
    fun resetPassword(
        @RequestParam token: String,
        @RequestParam newPassword: String,
        @RequestParam confirmPassword: String
    ): String {
        if (newPassword != confirmPassword) {
            return "redirect:/reset-password?token=$token&error=passwordMismatch"
        }
        return try {
            userService.resetPassword(token, newPassword)
            "redirect:/login?passwordReset=true"
        } catch (ex: VerificationTokenException) {
            "redirect:/reset-password?token=$token&error=invalidToken"
        } catch (ex: WeakPasswordException) {
            "redirect:/reset-password?token=$token&error=weakPassword"
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
            userService.findOrCreateByGoogle(email, googleId)

            val userDetails = userDetailsService.loadUserByUsername(email)
            val auth = UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.authorities
            )
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = auth
            SecurityContextHolder.setContext(context)

            val session = request.getSession(true)
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
            )

            "redirect:/dashboard"
        } catch (ex: InvalidGoogleTokenException) {
            "redirect:/login?error=googleFailed"
        }
    }
}
