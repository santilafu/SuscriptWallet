package com.subia.controller

import com.subia.model.Category
import com.subia.service.CategoryService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

/**
 * Controlador MVC para la gestión de categorías.
 *
 * Gestiona las rutas bajo /categories y delega la lógica de negocio en [CategoryService].
 * Sigue el patrón Post/Redirect/Get (PRG): después de cualquier operación de escritura
 * (POST) se redirige a la lista para evitar el reenvío del formulario al recargar la página.
 */
@Controller
@RequestMapping("/categories")
class CategoryController(private val categoryService: CategoryService) {

    /**
     * Muestra la lista de todas las categorías.
     * GET /categories
     */
    @GetMapping
    fun list(model: Model): String {
        model.addAttribute("categories", categoryService.findAll())
        return "categories/list"
    }

    /**
     * Muestra el formulario para crear una nueva categoría con valores por defecto.
     * GET /categories/new
     */
    @GetMapping("/new")
    fun newForm(model: Model): String {
        // Se pasa una categoría vacía para que el template Thymeleaf pueda
        // leer los valores actuales del objeto de forma uniforme (mismo template para crear y editar)
        model.addAttribute("category", Category(name = "", color = "#6c757d", icon = ""))
        return "categories/form"
    }

    /**
     * Procesa el formulario de creación de categoría.
     * POST /categories
     *
     * @param name  Nombre de la categoría (obligatorio).
     * @param color Color hexadecimal (por defecto gris "#6c757d").
     * @param icon  Emoji opcional.
     */
    @PostMapping
    fun create(
        @RequestParam name: String,
        @RequestParam(defaultValue = "#6c757d") color: String,
        @RequestParam(defaultValue = "") icon: String
    ): String {
        categoryService.save(Category(name = name, color = color, icon = icon))
        return "redirect:/categories"
    }

    /**
     * Muestra el formulario de edición con los datos actuales de la categoría.
     * GET /categories/{id}/edit
     */
    @GetMapping("/{id}/edit")
    fun editForm(@PathVariable id: Long, model: Model): String {
        model.addAttribute("category", categoryService.findById(id))
        return "categories/form"
    }

    /**
     * Procesa el formulario de edición de una categoría existente.
     * POST /categories/{id}
     *
     * Se crea una nueva instancia de [Category] con el mismo ID para que JPA haga UPDATE.
     */
    @PostMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestParam name: String,
        @RequestParam(defaultValue = "#6c757d") color: String,
        @RequestParam(defaultValue = "") icon: String
    ): String {
        categoryService.save(Category(id = id, name = name, color = color, icon = icon))
        return "redirect:/categories"
    }

    /**
     * Elimina una categoría.
     * POST /categories/{id}/delete
     *
     * Si la categoría tiene suscripciones asociadas, [CategoryService] lanza [IllegalStateException].
     * En ese caso se captura el mensaje y se pasa como atributo flash para mostrarlo en la lista.
     * Los atributos flash sobreviven a un único redirect y luego desaparecen.
     */
    @PostMapping("/{id}/delete")
    fun delete(@PathVariable id: Long, redirectAttributes: RedirectAttributes): String {
        try {
            categoryService.delete(id)
        } catch (e: IllegalStateException) {
            redirectAttributes.addFlashAttribute("errorMessage", e.message)
        }
        return "redirect:/categories"
    }
}
