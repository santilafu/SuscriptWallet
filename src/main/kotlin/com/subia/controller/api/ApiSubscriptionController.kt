package com.subia.controller.api

import com.subia.dto.api.ApiResponse
import com.subia.dto.api.SubscriptionDto
import com.subia.dto.api.SubscriptionRequestDto
import com.subia.model.Subscription
import com.subia.repository.UserRepository
import com.subia.service.CategoryService
import com.subia.service.SubscriptionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import com.subia.service.RenewalDates
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/subscriptions")
class ApiSubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val categoryService: CategoryService,
    private val userRepository: UserRepository,
    private val clock: Clock
) {

    private fun resolveUserId(jwt: Jwt): Long {
        val email = jwt.subject
        return userRepository.findByEmail(email)?.id
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")
    }

    @GetMapping
    fun listAll(@AuthenticationPrincipal jwt: Jwt): ApiResponse<List<SubscriptionDto>> {
        val userId = resolveUserId(jwt)
        return ApiResponse(data = subscriptionService.findAll(userId).map { it.toDto() })
    }

    @GetMapping("/{id}")
    fun getById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long
    ): ApiResponse<SubscriptionDto> {
        val userId = resolveUserId(jwt)
        return ApiResponse(data = subscriptionService.findById(id, userId).toDto())
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody req: SubscriptionRequestDto
    ): ApiResponse<SubscriptionDto> {
        val userId = resolveUserId(jwt)
        val category = categoryService.findById(req.categoryId)
        val sub = Subscription(
            name = req.name, description = req.description, price = req.price,
            currency = req.currency, billingCycle = req.billingCycle,
            renewalDate = req.renewalDate, active = req.active, notes = req.notes,
            category = category,
            isTrial = req.isTrial,
            trialEndsAt = trialEndFor(req)
        )
        return ApiResponse(data = subscriptionService.save(sub, userId).toDto())
    }

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
        @Valid @RequestBody req: SubscriptionRequestDto
    ): ApiResponse<SubscriptionDto> {
        val userId = resolveUserId(jwt)
        // Verify ownership
        val stored = subscriptionService.findById(id, userId)
        val category = categoryService.findById(req.categoryId)
        // R3-01: si el cliente devuelve la fecha proyectada sin tocarla, se conserva el ancla
        // guardada (día 31, 29-feb…) en vez de moverla al día recortado.
        val renewal = RenewalDates.anchorForUpdate(stored, req.renewalDate, req.billingCycle, LocalDate.now(clock))
        val sub = Subscription(
            id = id, name = req.name, description = req.description, price = req.price,
            currency = req.currency, billingCycle = req.billingCycle,
            renewalDate = renewal, active = req.active, notes = req.notes,
            category = category,
            isTrial = req.isTrial,
            trialEndsAt = trialEndFor(req),
            userId = userId
        )
        return ApiResponse(data = subscriptionService.save(sub).toDto())
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long
    ) {
        val userId = resolveUserId(jwt)
        subscriptionService.delete(id, userId)
    }

    /**
     * Antes `LocalDate.parse` sin control: una fecha mal formada devolvía un 500. Ahora vacío =
     * sin fecha y formato inválido = 400 (IllegalArgumentException → VALIDATION_ERROR).
     */
    /**
     * La web exige fecha de fin en una prueba gratuita; la API la aceptaba sin ella y la prueba
     * quedaba "vigente" para siempre (fuera de totales y sin aviso). Para no romper la app con un
     * 400, si falta se usa la fecha del primer cobro (`renewalDate`), que es lo que hace la web al
     * añadir una prueba desde el catálogo.
     */
    private fun trialEndFor(req: SubscriptionRequestDto): LocalDate? {
        val parsed = parseTrialEnd(req.trialEndsAt)
        return if (req.isTrial && parsed == null) req.renewalDate else parsed
    }

    private fun parseTrialEnd(raw: String?): LocalDate? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return try {
            LocalDate.parse(value)
        } catch (ex: DateTimeParseException) {
            throw IllegalArgumentException("trialEndsAt debe tener formato yyyy-MM-dd")
        }
    }

    private fun Subscription.toDto(): SubscriptionDto {
        // Hibernate puede dejar `category` a null si la fila quedó huérfana (categoría borrada).
        // Kotlin la declara no-nula, pero en runtime puede serlo: la tratamos defensivamente
        // para que una sola suscripción mal enlazada no reviente toda la lista con un 500.
        @Suppress("SENSELESS_COMPARISON")
        val cat: com.subia.model.Category? = category
        return SubscriptionDto(
            id = id, name = name, description = description, price = price,
            currency = currency, billingCycle = billingCycle.name,
            // Próxima renovación real (N-12): la guardada no se avanza nunca y la app mostraba y
            // volvía a guardar fechas pasadas.
            renewalDate = RenewalDates.nextRenewal(this, LocalDate.now(clock)),
            // Ancla guardada (R3-01/R3-02): la app proyecta desde aquí con SU "hoy" local y la
            // reenvía en el PUT si el usuario no toca la fecha. Campo nuevo, ignorado por apps viejas.
            renewalAnchor = renewalDate,
            active = active, notes = notes,
            categoryId = cat?.id ?: 0L,
            categoryName = cat?.name ?: "Sin categoría",
            categoryColor = cat?.color ?: "#64748b",
            categoryIcon = cat?.icon ?: "📦",
            isTrial = isTrial, trialEndsAt = trialEndsAt?.toString()
        )
    }
}
