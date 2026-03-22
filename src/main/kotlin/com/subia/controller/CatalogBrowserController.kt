package com.subia.controller

import com.subia.service.CatalogService
import com.subia.service.CategoryService
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
    private val categoryService: CategoryService
) {

    @GetMapping
    fun browse(model: Model): String {
        model.addAttribute("allItems", catalogService.getAllItems())

        // Construye un mapa categoryKey → ID de la entidad JPA de Categoría.
        // Permite que el template genere el form POST correcto para cada card del catálogo.
        val categoryKeyToId = categoryService.findAll()
            .associate { cat ->
                val n = cat.name.lowercase()
                val key = when {
                    n.contains("ia") || n.contains("ai") || n.contains("intelig") -> "ia"
                    n.contains("stream") || n.contains("video") -> "streaming"
                    n.contains("músic") || n.contains("music") || n.contains("music") -> "musica"
                    n.contains("soft") || n.contains("produc") -> "software"
                    n.contains("cloud") || n.contains("nube") || n.contains("almac") -> "cloud"
                    n.contains("gam") || n.contains("jueg") -> "gaming"
                    n.contains("segur") || n.contains("privac") -> "seguridad"
                    n.contains("notici") || n.contains("lectur") -> "noticias"
                    n.contains("salud") || n.contains("deport") -> "salud"
                    n.contains("desarroll") || n.contains("devops") -> "desarrollo"
                    n.contains("prueba") || n.contains("trial") -> "prueba"
                    n.contains("finanz") -> "finanzas"
                    n.contains("educac") || n.contains("educación") -> "educacion"
                    n.contains("creativid") || n.contains("foto") -> "creatividad"
                    n.contains("citas") || n.contains("dating") -> "citas"
                    else -> n
                }
                key to cat.id
            }
        model.addAttribute("categoryKeyToId", categoryKeyToId)
        return "catalog-browser"
    }
}
