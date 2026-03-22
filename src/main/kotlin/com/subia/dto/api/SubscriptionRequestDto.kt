package com.subia.dto.api

import com.subia.model.BillingCycle
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class SubscriptionRequestDto(
    @field:NotBlank val name: String,
    val description: String = "",
    @field:NotNull @field:Positive val price: BigDecimal,
    val currency: String = "EUR",
    @field:NotNull val billingCycle: BillingCycle,
    @field:NotNull val renewalDate: LocalDate,
    val active: Boolean = true,
    val notes: String = "",
    @field:NotNull val categoryId: Long,
    val isTrial: Boolean = false,
    val trialEndsAt: String? = null
)