package com.subia.controller

import com.subia.model.BillingCycle
import com.subia.model.Subscription
import com.subia.service.CategoryService
import com.subia.service.SubscriptionService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Controlador MVC para la gestión de suscripciones.
 *
 * Gestiona las rutas bajo /subscriptions y delega la lógica de negocio en [SubscriptionService].
 * Sigue el patrón Post/Redirect/Get (PRG): después de cualquier escritura (POST) se redirige
 * a la lista para evitar el reenvío del formulario al recargar la página.
 *
 * El formulario de creación/edición incluye un selector de catálogo que funciona mediante
 * llamadas AJAX a /api/catalog (gestionado por [CatalogController]).
 */
@Controller
@RequestMapping("/subscriptions")
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val categoryService: CategoryService,
    private val catalogService: com.subia.service.CatalogService
) {

    /**
     * Muestra la lista de todas las suscripciones (activas e inactivas).
     * GET /subscriptions
     */
    @GetMapping
    fun list(model: Model): String {
        model.addAttribute("subscriptions", subscriptionService.findAll())
        model.addAttribute("categories", categoryService.findAll())
        model.addAttribute("serviceDomains", catalogService.getDomainMap())
        model.addAttribute("cancelUrls", catalogService.getCancelUrlMap())
        return "subscriptions/list"
    }

    /**
     * Muestra el formulario para crear una nueva suscripción.
     * GET /subscriptions/new
     *
     * Pasa al template:
     * - "categories": lista de categorías para el selector.
     * - "billingCycles": todos los valores del enum para el selector de ciclo.
     * - "sub": mapa con valores vacíos (el template comparte lógica con el formulario de edición).
     */
    @GetMapping("/new")
    fun newForm(model: Model): String {
        model.addAttribute("categories", categoryService.findAll())
        model.addAttribute("billingCycles", BillingCycle.values())
        model.addAttribute("sub", emptyForm())
        return "subscriptions/form"
    }

    /**
     * Procesa el formulario de creación de una suscripción nueva.
     * POST /subscriptions
     *
     * Spring convierte automáticamente los parámetros del form a los tipos Kotlin:
     * BigDecimal, BillingCycle (por nombre), LocalDate (con la anotación @DateTimeFormat).
     */
    @PostMapping
    fun create(
        @RequestParam name: String,
        @RequestParam(defaultValue = "") description: String,
        @RequestParam price: BigDecimal,
        @RequestParam(defaultValue = "EUR") currency: String,
        @RequestParam billingCycle: BillingCycle,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) renewalDate: LocalDate,
        @RequestParam categoryId: Long,
        @RequestParam(defaultValue = "false") active: Boolean,
        @RequestParam(defaultValue = "") notes: String,
        @RequestParam(defaultValue = "false") isTrial: Boolean,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) trialEndsAt: LocalDate?
    ): String {
        if (isTrial && trialEndsAt == null)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "trialEndsAt is required when isTrial is true")
        val category = categoryService.findById(categoryId)
        subscriptionService.save(
            Subscription(
                name = name, description = description, price = price, currency = currency,
                billingCycle = billingCycle, renewalDate = renewalDate, category = category,
                active = active, notes = notes, isTrial = isTrial, trialEndsAt = trialEndsAt
            )
        )
        return "redirect:/subscriptions"
    }

    /**
     * Muestra el formulario de edición con los datos actuales de la suscripción.
     * GET /subscriptions/{id}/edit
     */
    @GetMapping("/{id}/edit")
    fun editForm(@PathVariable id: Long, model: Model): String {
        model.addAttribute("sub", subscriptionService.findById(id))
        model.addAttribute("categories", categoryService.findAll())
        model.addAttribute("billingCycles", BillingCycle.values())
        return "subscriptions/form"
    }

    /**
     * Procesa el formulario de edición de una suscripción existente.
     * POST /subscriptions/{id}
     *
     * Se construye una nueva instancia de [Subscription] con el mismo ID para que JPA haga UPDATE.
     */
    @PostMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestParam name: String,
        @RequestParam(defaultValue = "") description: String,
        @RequestParam price: BigDecimal,
        @RequestParam(defaultValue = "EUR") currency: String,
        @RequestParam billingCycle: BillingCycle,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) renewalDate: LocalDate,
        @RequestParam categoryId: Long,
        @RequestParam(defaultValue = "false") active: Boolean,
        @RequestParam(defaultValue = "") notes: String,
        @RequestParam(defaultValue = "false") isTrial: Boolean,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) trialEndsAt: LocalDate?
    ): String {
        if (isTrial && trialEndsAt == null)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "trialEndsAt is required when isTrial is true")
        val category = categoryService.findById(categoryId)
        subscriptionService.save(
            Subscription(
                id = id, name = name, description = description, price = price, currency = currency,
                billingCycle = billingCycle, renewalDate = renewalDate, category = category,
                active = active, notes = notes, isTrial = isTrial, trialEndsAt = trialEndsAt
            )
        )
        return "redirect:/subscriptions"
    }

    /**
     * Crea una suscripción nueva a partir de un item del catálogo de servicios.
     * POST /subscriptions/add-from-catalog
     *
     * El template catalog-browser.html genera un formulario por cada card del catálogo.
     * Si la suscripción es una prueba, la fecha de renovación se fija al fin del trial;
     * de lo contrario se establece a un mes desde hoy.
     */
    @PostMapping("/add-from-catalog")
    fun addFromCatalog(
        @RequestParam name: String,
        @RequestParam(defaultValue = "") description: String,
        @RequestParam price: BigDecimal,
        @RequestParam(defaultValue = "EUR") currency: String,
        @RequestParam billingCycle: BillingCycle,
        @RequestParam categoryId: Long,
        @RequestParam(defaultValue = "false") isTrial: Boolean,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) trialEndsAt: LocalDate?
    ): String {
        val category = categoryService.findById(categoryId)
        subscriptionService.save(
            Subscription(
                name = name, description = description, price = price, currency = currency,
                billingCycle = billingCycle,
                renewalDate = if (isTrial && trialEndsAt != null) trialEndsAt else LocalDate.now().plusMonths(1),
                category = category, active = true, notes = "",
                isTrial = isTrial, trialEndsAt = trialEndsAt
            )
        )
        return "redirect:/subscriptions"
    }

    /**
     * Muestra la pantalla de confirmación antes de eliminar una suscripción.
     * GET /subscriptions/{id}/delete
     */
    @GetMapping("/{id}/delete")
    fun deleteConfirm(@PathVariable id: Long, model: Model): String {
        model.addAttribute("sub", subscriptionService.findById(id))
        return "subscriptions/confirm-delete"
    }

    /**
     * Elimina definitivamente una suscripción.
     * POST /subscriptions/{id}/delete
     */
    @PostMapping("/{id}/delete")
    fun delete(@PathVariable id: Long): String {
        subscriptionService.delete(id)
        return "redirect:/subscriptions"
    }

    /**
     * Devuelve un mapa con los valores por defecto para el formulario de nueva suscripción.
     *
     * Se usa un mapa en lugar de una instancia de [Subscription] para evitar tener que
     * crear un objeto con campos obligatorios (como category y billingCycle) sin valor.
     * El template Thymeleaf distingue entre mapa y objeto con instanceof.
     */
    private fun emptyForm() = mapOf(
        "id"          to 0L,
        "name"        to "",
        "description" to "",
        "price"       to "",
        "currency"     to "EUR",
        "billingCycle" to null,
        "renewalDate"  to LocalDate.now(),
        "category"    to null,
        "active"      to true,
        "notes"       to "",
        "isTrial"     to false,
        "trialEndsAt" to null
    )
}