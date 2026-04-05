package com.subia.controller

import com.subia.model.SecurityEventType
import com.subia.service.SecurityEventService
import com.subia.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class DeleteAccountController(
    private val userService: UserService,
    private val securityEventService: SecurityEventService
) {

    @GetMapping("/delete-account")
    fun deleteAccountPage(
        @AuthenticationPrincipal principal: UserDetails?,
        model: Model
    ): String {
        if (principal != null) {
            val user = userService.findByEmail(principal.username)
            model.addAttribute("userEmail", user?.email ?: principal.username)
            model.addAttribute("hasPassword", user?.passwordHash != null)
            model.addAttribute("loggedIn", true)
        } else {
            model.addAttribute("loggedIn", false)
        }
        return "auth/delete-account"
    }

    @PostMapping("/delete-account")
    fun deleteAccount(
        @AuthenticationPrincipal principal: UserDetails,
        @RequestParam(required = false) password: String?,
        request: HttpServletRequest,
        session: HttpSession
    ): String {
        val userId = getUserIdFromPrincipal(principal)
        val user = userService.findById(userId)
            ?: return "redirect:/delete-account?error=notFound"

        // Si el usuario tiene contraseña, verificarla
        if (user.passwordHash != null) {
            if (password.isNullOrBlank()) {
                return "redirect:/delete-account?error=passwordRequired"
            }
            if (!userService.checkPassword(userId, password)) {
                return "redirect:/delete-account?error=wrongPassword"
            }
        }

        userService.deleteAccount(userId)

        val ip = securityEventService.extractIp(request)
        val userAgent = request.getHeader("User-Agent")
        securityEventService.logEvent(null, SecurityEventType.ACCOUNT_DELETED, ip, userAgent)

        session.invalidate()

        return "redirect:/account-deleted"
    }

    @GetMapping("/account-deleted")
    fun accountDeleted(): String = "auth/account-deleted"

    private fun getUserIdFromPrincipal(principal: UserDetails): Long {
        // UserDetailsServiceImpl sets username = email; look up by email
        val user = userService.findByEmail(principal.username)
            ?: throw IllegalStateException("Usuario autenticado no encontrado: ${principal.username}")
        return user.id
    }
}
