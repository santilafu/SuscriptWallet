package com.subia.controller

import com.subia.service.CatalogService
import com.subia.service.CategoryMappingService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.time.Clock
import java.time.LocalDate

/**
 * Controlador MVC para el buscador/directorio de aplicaciones del catálogo.
 *
 * Sirve la página /catalog-browser que permite explorar todos los servicios conocidos,
 * filtrarlos por categoría y añadirlos directamente como suscripciones activas.
 */
@Controller
@RequestMapping("/catalog-browser")
class CatalogBrowserController(
    private val catalogService: CatalogService,
    private val categoryMappingService: CategoryMappingService,
    private val clock: Clock
) {

    @GetMapping
    fun browse(model: Model): String {
        val allItems = catalogService.getAllItems()
        model.addAttribute("allItems", allItems)
        // Las filas "prueba" son copias de otras: el contador solo cuenta servicios distintos.
        model.addAttribute("serviceCount", allItems.count { it.categoryKey != "prueba" })
        // Fecha base para el fin de prueba (antes `T(LocalDate).now()` en la plantilla, prohibido en Thymeleaf 3.1.3)
        model.addAttribute("today", LocalDate.now(clock))

        // Mapa categoryKey → ID de la entidad JPA de Categoría (único punto de verdad: CategoryMappingService).
        // Permite que el template genere el form POST correcto para cada card del catálogo.
        model.addAttribute("categoryKeyToId", categoryMappingService.categoryKeyToId())
        return "catalog-browser"
    }
}
