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
import java.time.LocalDate

@RestController
@RequestMapping("/api/subscriptions")
class ApiSubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val categoryService: CategoryService,
    private val userRepository: UserRepository
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
            trialEndsAt = req.trialEndsAt?.let { LocalDate.parse(it) }
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
        subscriptionService.findById(id, userId)
        val category = categoryService.findById(req.categoryId)
        val sub = Subscription(
            id = id, name = req.name, description = req.description, price = req.price,
            currency = req.currency, billingCycle = req.billingCycle,
            renewalDate = req.renewalDate, active = req.active, notes = req.notes,
            category = category,
            isTrial = req.isTrial,
            trialEndsAt = req.trialEndsAt?.let { LocalDate.parse(it) },
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

    private fun Subscription.toDto() = SubscriptionDto(
        id = id, name = name, description = description, price = price,
        currency = currency, billingCycle = billingCycle.name, renewalDate = renewalDate,
        active = active, notes = notes, categoryId = category.id, categoryName = category.name,
        categoryColor = category.color, categoryIcon = category.icon,
        isTrial = isTrial, trialEndsAt = trialEndsAt?.toString()
    )
}
