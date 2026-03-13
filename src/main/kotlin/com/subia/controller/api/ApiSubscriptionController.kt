package com.subia.controller.api

import com.subia.dto.api.ApiResponse
import com.subia.dto.api.SubscriptionDto
import com.subia.dto.api.SubscriptionRequestDto
import com.subia.model.Subscription
import com.subia.service.CategoryService
import com.subia.service.SubscriptionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/subscriptions")
class ApiSubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val categoryService: CategoryService
) {
    @GetMapping
    fun listAll(): ApiResponse<List<SubscriptionDto>> =
        ApiResponse(data = subscriptionService.findAll().map { it.toDto() })

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ApiResponse<SubscriptionDto> =
        ApiResponse(data = subscriptionService.findById(id).toDto())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: SubscriptionRequestDto): ApiResponse<SubscriptionDto> {
        val category = categoryService.findById(req.categoryId)
        val sub = Subscription(
            name = req.name, description = req.description, price = req.price,
            currency = req.currency, billingCycle = req.billingCycle,
            renewalDate = req.renewalDate, active = req.active, notes = req.notes,
            category = category
        )
        return ApiResponse(data = subscriptionService.save(sub).toDto())
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: SubscriptionRequestDto): ApiResponse<SubscriptionDto> {
        // Verify it exists before updating (throws NoSuchElementException if not found)
        subscriptionService.findById(id)
        val category = categoryService.findById(req.categoryId)
        val sub = Subscription(
            id = id, name = req.name, description = req.description, price = req.price,
            currency = req.currency, billingCycle = req.billingCycle,
            renewalDate = req.renewalDate, active = req.active, notes = req.notes,
            category = category
        )
        return ApiResponse(data = subscriptionService.save(sub).toDto())
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) { subscriptionService.delete(id) }

    private fun Subscription.toDto() = SubscriptionDto(
        id = id, name = name, description = description, price = price,
        currency = currency, billingCycle = billingCycle.name, renewalDate = renewalDate,
        active = active, notes = notes, categoryId = category.id, categoryName = category.name,
        categoryColor = category.color, categoryIcon = category.icon
    )
}