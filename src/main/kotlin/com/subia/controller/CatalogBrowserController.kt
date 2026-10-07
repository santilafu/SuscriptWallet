package com.subia.controller

import com.subia.service.CatalogService
import com.subia.service.CategoryMappingService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

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
    private val categoryMappingService: CategoryMappingService
) {

    @GetMapping
    fun browse(model: Model): String {
        model.addAttribute("allItems", catalogService.getAllItems())

        // Mapa categoryKey → ID de la entidad JPA de Categoría (único punto de verdad: CategoryMappingService).
        // Permite que el template genere el form POST correcto para cada card del catálogo.
        model.addAttribute("categoryKeyToId", categoryMappingService.categoryKeyToId())
        return "catalog-browser"
    }
}
