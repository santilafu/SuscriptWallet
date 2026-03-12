package com.subia.controller

import com.subia.service.DashboardService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

/**
 * Controlador MVC para la pantalla principal (dashboard).
 *
 * Gestiona la ruta raíz "/" y "/dashboard".
 * Delega todos los cálculos en [DashboardService] y pasa el resultado al template Thymeleaf.
 */
@Controller
class DashboardController(private val dashboardService: DashboardService) {

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
    fun dashboard(model: Model): String {
        model.addAttribute("dashboard", dashboardService.getDashboard())
        return "dashboard"
    }
}