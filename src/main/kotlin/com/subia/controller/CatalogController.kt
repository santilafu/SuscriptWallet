package com.subia.controller

import com.subia.model.CatalogItem
import com.subia.service.CatalogService
import com.subia.service.CategoryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Controlador REST que expone el catálogo de servicios conocidos.
 *
 * El frontend lo consume mediante fetch() para rellenar el desplegable de servicios
 * cuando el usuario selecciona una categoría en el formulario de nueva suscripción.
 *
 * Ruta base: /api/catalog
 */
@RestController
@RequestMapping("/api/catalog")
class CatalogController(
    private val catalogService: CatalogService,
    private val categoryService: CategoryService
) {

    /**
     * Devuelve los servicios del catálogo filtrados por categoría.
     *
     * Si se proporciona [categoryId], busca la categoría en la base de datos y devuelve
     * los servicios que coincidan con su nombre. Si no se proporciona, devuelve todo el catálogo.
     *
     * Ejemplo de llamada: GET /api/catalog?categoryId=1
     *
     * @param categoryId ID de la categoría guardada en la base de datos (opcional).
     * @return Lista de [CatalogItem] serializada como JSON por Jackson.
     */
    @GetMapping
    fun getItems(@RequestParam(required = false) categoryId: Long?): List<CatalogItem> {
        // Si no se pasa categoryId, devuelve todo el catálogo
        if (categoryId == null) return catalogService.getAllItems()

        // Busca la categoría y filtra el catálogo por su nombre
        val category = runCatching { categoryService.findById(categoryId) }.getOrNull()
            ?: return catalogService.getAllItems()

        return catalogService.getItemsForCategory(category.name)
    }
}