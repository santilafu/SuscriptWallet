package com.subia.dto.api

import java.math.BigDecimal
import java.time.LocalDate

data class DashboardStatsDto(
    val totalMonthly: BigDecimal,
    val totalYearly: BigDecimal,
    val activeCount: Int,
    val alertCount: Int,
    val spendByCategory: List<CategorySpendDto>,
    val upcomingRenewals: List<RenewalDto>
)

data class CategorySpendDto(
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String,
    val monthlyAmount: BigDecimal
)

data class RenewalDto(
    val subscriptionId: Long,
    val name: String,
    val renewalDate: LocalDate,
    val price: BigDecimal,
    val currency: String,
    val billingCycle: String
)