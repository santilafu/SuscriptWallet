package com.subia.dto.api

import java.math.BigDecimal
import java.time.LocalDate

data class SubscriptionDto(
    val id: Long,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val currency: String,
    val billingCycle: String,
    val renewalDate: LocalDate,
    val active: Boolean,
    val notes: String,
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String
)