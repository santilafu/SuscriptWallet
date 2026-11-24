package com.subia.controller

import com.subia.model.BillingCycle
import com.subia.model.Subscription
import com.subia.repository.UserRepository
import com.subia.service.CategoryService
import com.subia.service.Periodicidad
import com.subia.service.SubscriptionService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Clock
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
    private val catalogService: com.subia.service.CatalogService,
    private val userRepository: UserRepository,
    private val clock: Clock
) {

    private fun resolveUserId(userDetails: UserDetails): Long =
        userRepository.findByEmail(userDetails.username)?.id
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")

    /**
     * Muestra la lista de todas las suscripciones del usuario autenticado.
     * GET /subscriptions
     */
    @GetMapping
    fun list(@AuthenticationPrincipal userDetails: UserDetails, model: Model): String {
        val userId = resolveUserId(userDetails)
        val stored = subscriptionService.findAll(userId)
        val subs = stored.map { it.projected() }
        val today = LocalDate.now(clock)
        model.addAttribute("subscriptions", subs)
        // Plazo (cuotas restantes / terminado) por id, desde el ancla guardada y no la proyectada.
        model.addAttribute("plazos", plazos(stored, today))
        // Días restantes de prueba calculados aquí: Thymeleaf 3.1.3 (Spring Boot 3.5) prohíbe
        // T(...) en las plantillas y la lista reventaba al calcularlo con T(ChronoUnit).
        model.addAttribute("trialDaysLeft", subs.filter { it.trialEndsAt != null }
            .associate { it.id to java.time.temporal.ChronoUnit.DAYS.between(today, it.trialEndsAt) })
        model.addAttribute("categories", categoryService.findAll())
        model.addAttribute("serviceDomains", catalogService.getDomainMap())
        // URL directa del logo (si el catálogo la tiene): la web la prueba antes que icon.horse.
        model.addAttribute("serviceIcons", catalogService.getIconUrlMap())
        model.addAttribute("cancelUrls", catalogService.getCancelUrlMap())
        return "subscriptions/list"
    }

    /**
     * Muestra el formulario para crear una nueva suscripción.
     * GET /subscriptions/new
     *
     * Pasa al template:
     * - "categories": lista de categorías para el selector.
     * - "periodicidadActual": valor preseleccionado del desplegable de periodicidad ("" = ninguno).
     * - "sub": mapa con valores vacíos (el template comparte lógica con el formulario de edición).
     */
    @GetMapping("/new")
    fun newForm(model: Model): String {
        model.addAttribute("categories", categoryService.findAll())
        model.addAttribute("currencies", com.subia.model.Currencies.SUPPORTED)
        model.addAttribute("sub", emptyForm())
        // La plantilla distinguía alta/edición con `instanceof T(...)`, prohibido en Thymeleaf 3.1.3
        model.addAttribute("isEntity", false)
        // Sin opción preseleccionada: el usuario debe elegir la periodicidad.
        model.addAttribute("periodicidadActual", "")
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
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam name: String,
        @RequestParam(defaultValue = "") description: String,
        @RequestParam price: BigDecimal,
        @RequestParam(defaultValue = "EUR") currency: String,
        @RequestParam(required = false) periodicidad: String?,
        @RequestParam(required = false) billingCycle: BillingCycle?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) renewalDate: LocalDate,
        @RequestParam categoryId: Long,
        @RequestParam(defaultValue = "false") active: Boolean,
        @RequestParam(defaultValue = "") notes: String,
        @RequestParam(defaultValue = "false") isTrial: Boolean,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) trialEndsAt: LocalDate?,
        // Plazo (opcional). El formulario siempre envía los dos campos: vacío = sin fecha (borra).
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fechaInicio: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fechaFin: LocalDate?
    ): String {
        if (isTrial && trialEndsAt == null)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "trialEndsAt is required when isTrial is true")
        val userId = resolveUserId(userDetails)
        val category = categoryService.findById(categoryId)
        val (cycle, intervalo) = periodicidadRecibida(periodicidad, billingCycle, stored = null)
        subscriptionService.save(
            Subscription(
                name = name, description = description, price = price, currency = currency,
                billingCycle = cycle, intervaloMeses = intervalo, renewalDate = renewalDate, category = category,
                active = active, notes = notes, isTrial = isTrial, trialEndsAt = trialEndsAt,
                fechaInicio = fechaInicio, fechaFin = fechaFin
            ),
            userId
        )
        return "redirect:/subscriptions"
    }

    /**
     * Muestra el formulario de edición con los datos actuales de la suscripción.
     * GET /subscriptions/{id}/edit
     */
    @GetMapping("/{id}/edit")
    fun editForm(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        model: Model
    ): String {
        val userId = resolveUserId(userDetails)
        val stored = subscriptionService.findById(id, userId)
        val sub = stored.projected()
        model.addAttribute("sub", sub)
        model.addAttribute("isEntity", true)
        model.addAttribute("plazo", com.subia.service.RenewalDates.plazo(stored, LocalDate.now(clock)))
        model.addAttribute("periodicidadActual", valorPeriodicidad(sub))
        model.addAttribute("categories", categoryService.findAll())
        model.addAttribute("currencies", com.subia.model.Currencies.SUPPORTED)
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
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        @RequestParam name: String,
        @RequestParam(defaultValue = "") description: String,
        @RequestParam price: BigDecimal,
        @RequestParam(defaultValue = "EUR") currency: String,
        @RequestParam(required = false) periodicidad: String?,
        @RequestParam(required = false) billingCycle: BillingCycle?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) renewalDate: LocalDate,
        @RequestParam categoryId: Long,
        @RequestParam(defaultValue = "false") active: Boolean,
        @RequestParam(defaultValue = "") notes: String,
        @RequestParam(defaultValue = "false") isTrial: Boolean,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) trialEndsAt: LocalDate?,
        // Plazo (opcional). El formulario siempre envía los dos campos: vacío = sin fecha (borra).
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fechaInicio: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fechaFin: LocalDate?
    ): String {
        if (isTrial && trialEndsAt == null)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "trialEndsAt is required when isTrial is true")
        val userId = resolveUserId(userDetails)
        // verify ownership
        val stored = subscriptionService.findById(id, userId)
        val category = categoryService.findById(categoryId)
        // Antes la entidad se reconstruía sin intervalo y un trimestral volvía a mensual.
        val (cycle, intervalo) = periodicidadRecibida(periodicidad, billingCycle, stored)
        // R3-01: el formulario muestra la próxima renovación proyectada; si vuelve sin tocar se
        // conserva el ancla guardada (día 31, 29-feb…) en vez de guardar el día recortado.
        val renewal = com.subia.service.RenewalDates.anchorForUpdate(
            stored, renewalDate, cycle, intervalo, LocalDate.now(clock)
        )
        subscriptionService.save(
            Subscription(
                id = id, name = name, description = description, price = price, currency = currency,
                billingCycle = cycle, intervaloMeses = intervalo, renewalDate = renewal, category = category,
                active = active, notes = notes, isTrial = isTrial, trialEndsAt = trialEndsAt,
                fechaInicio = fechaInicio, fechaFin = fechaFin,
                userId = userId
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
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam name: String,
        @RequestParam(defaultValue = "") description: String,
        @RequestParam price: BigDecimal,
        @RequestParam(defaultValue = "EUR") currency: String,
        @RequestParam billingCycle: BillingCycle,
        @RequestParam categoryId: Long,
        @RequestParam(defaultValue = "false") isTrial: Boolean,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) trialEndsAt: LocalDate?
    ): String {
        val userId = resolveUserId(userDetails)
        val category = categoryService.findById(categoryId)
        subscriptionService.save(
            Subscription(
                name = name, description = description, price = price, currency = currency,
                billingCycle = billingCycle,
                renewalDate = if (isTrial && trialEndsAt != null) trialEndsAt else LocalDate.now(clock).plusMonths(1),
                category = category, active = true, notes = "",
                isTrial = isTrial, trialEndsAt = trialEndsAt
            ),
            userId
        )
        return "redirect:/subscriptions"
    }

    /**
     * Muestra la pantalla de confirmación antes de eliminar una suscripción.
     * GET /subscriptions/{id}/delete
     */
    @GetMapping("/{id}/delete")
    fun deleteConfirm(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        model: Model
    ): String {
        val userId = resolveUserId(userDetails)
        model.addAttribute("sub", subscriptionService.findById(id, userId).projected())
        return "subscriptions/confirm-delete"
    }

    /**
     * Elimina definitivamente una suscripción.
     * POST /subscriptions/{id}/delete
     */
    @PostMapping("/{id}/delete")
    fun delete(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long
    ): String {
        val userId = resolveUserId(userDetails)
        subscriptionService.delete(id, userId)
        return "redirect:/subscriptions"
    }

    /**
     * Devuelve un mapa con los valores por defecto para el formulario de nueva suscripción.
     *
     * Se usa un mapa en lugar de una instancia de [Subscription] para evitar tener que
     * crear un objeto con campos obligatorios (como category y billingCycle) sin valor.
     * El template Thymeleaf distingue entre mapa y objeto con instanceof.
     */
    @GetMapping("/export")
    fun exportCsv(
        @AuthenticationPrincipal userDetails: UserDetails,
        response: HttpServletResponse
    ) {
        val userId = resolveUserId(userDetails)
        val subs = subscriptionService.findAll(userId).map { it.projected() }

        response.contentType = "text/csv; charset=UTF-8"
        response.setHeader("Content-Disposition", "attachment; filename=\"suscripciones.csv\"")

        val writer = response.writer
        // Columnas nuevas siempre al FINAL para no mover las que ya usa quien importa el CSV.
        writer.println("Nombre,Descripcion,Precio,Moneda,Ciclo,Fecha renovacion,Categoria,Activa,Prueba gratuita,Fin prueba,Notas,Fecha inicio,Fecha fin")
        for (s in subs) {
            val line = listOf(
                csvEscape(s.name),
                csvEscape(s.description),
                s.price.toPlainString(),
                s.currency,
                s.billingCycle.name,
                s.renewalDate.toString(),
                csvEscape(s.category.name),
                if (s.active) "Sí" else "No",
                if (s.isTrial) "Sí" else "No",
                s.trialEndsAt?.toString() ?: "",
                csvEscape(s.notes),
                s.fechaInicio?.toString() ?: "",
                s.fechaFin?.toString() ?: ""
            ).joinToString(",")
            writer.println(line)
        }
        writer.flush()
    }

    /**
     * Copia (no persistida) con la próxima renovación real en `renewalDate` (N-12): la fecha
     * guardada no se avanza nunca y la lista mostraba cobros de hace meses.
     */
    private fun plazos(subs: List<Subscription>, today: LocalDate): Map<Long, com.subia.service.RenewalDates.Plazo> =
        subs.mapNotNull { s -> com.subia.service.RenewalDates.plazo(s, today)?.let { s.id to it } }.toMap()

    private fun Subscription.projected(): Subscription =
        copy(renewalDate = com.subia.service.RenewalDates.nextRenewal(this, LocalDate.now(clock)))

    /**
     * Periodicidad que envía el formulario web, ya resuelta con [Periodicidad.resolver].
     *
     * El desplegable manda un único parámetro `periodicidad`: `WEEKLY` o el número de meses
     * (`1`, `2`, `3`, `6`, `12`). Un solo valor evita tener que sincronizar dos campos con
     * JavaScript. Si no llega (un formulario antiguo abierto durante un despliegue), se usa
     * `billingCycle` con la misma regla que la API: al editar con el mismo ciclo se conserva el
     * intervalo guardado.
     *
     * @throws IllegalArgumentException si el valor no es válido (la web responde 400).
     */
    private fun periodicidadRecibida(
        periodicidad: String?,
        billingCycle: BillingCycle?,
        stored: Subscription?
    ): Pair<BillingCycle, Int> {
        val valor = periodicidad?.trim().orEmpty()
        if (valor.isEmpty()) {
            requireNotNull(billingCycle) { "La periodicidad es obligatoria" }
            return Periodicidad.resolver(billingCycle, null, stored)
        }
        if (valor == BillingCycle.WEEKLY.name) return Periodicidad.resolver(BillingCycle.WEEKLY, 1)
        val meses = requireNotNull(valor.toIntOrNull()) { "Periodicidad no válida: ${valor.take(10)}" }
        return Periodicidad.resolver(BillingCycle.MONTHLY, meses)
    }

    /** Valor del desplegable de periodicidad que corresponde a una suscripción guardada. */
    private fun valorPeriodicidad(sub: Subscription): String =
        if (sub.billingCycle == BillingCycle.WEEKLY) BillingCycle.WEEKLY.name
        else Periodicidad.mesesPorCobro(sub.billingCycle, sub.intervaloMeses).toString()

    private fun csvEscape(raw: String): String {
        // Inyección de fórmulas: Excel/LibreOffice ejecutan celdas que empiezan por = + - @
        val value = if (raw.isNotEmpty() && raw[0] in "=+-@\t\r") "'$raw" else raw
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private fun emptyForm() = mapOf(
        "id"          to 0L,
        "name"        to "",
        "description" to "",
        "price"       to "",
        "currency"     to "EUR",
        "billingCycle" to null,
        "renewalDate"  to LocalDate.now(clock),
        "category"    to null,
        "active"      to true,
        "notes"       to "",
        "isTrial"     to false,
        "trialEndsAt" to null,
        "fechaInicio" to null,
        "fechaFin"    to null
    )
}