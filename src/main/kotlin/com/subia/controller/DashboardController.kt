package com.subia.controller

import com.subia.repository.UserRepository
import com.subia.service.DashboardService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.server.ResponseStatusException

/**
 * Controlador MVC para la pantalla principal (dashboard).
 *
 * Gestiona la ruta raíz "/" y "/dashboard".
 * Delega todos los cálculos en [DashboardService] y pasa el resultado al template Thymeleaf.
 */
@Controller
class DashboardController(
    private val dashboardService: DashboardService,
    private val userRepository: UserRepository
) {

    /**
     * Redirige la raíz del sitio al dashboard.
     * GET /  →  redirect:/dashboard
     */
    @GetMapping("/")
    fun home() = "redirect:/dashboard"

    /**
     * Muestra el dashboard con gastos, gasto por categoría y próximas renovaciones.
     * GET /dashboard
     *
     * Pasa el [com.subia.dto.DashboardDto] al template bajo la clave "dashboard".
     */
    @GetMapping("/dashboard")
    fun dashboard(@AuthenticationPrincipal userDetails: UserDetails, model: Model): String {
        val userId = userRepository.findByEmail(userDetails.username)?.id
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")
        model.addAttribute("dashboard", dashboardService.getDashboard(userId))
        return "dashboard"
    }
}