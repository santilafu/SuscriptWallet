package com.subia.controller.api

import com.subia.dto.DashboardMobileStatsDto
import com.subia.dto.api.*
import com.subia.service.DashboardService
import com.subia.service.SubscriptionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dashboard")
class ApiDashboardController(
    private val dashboardService: DashboardService,
    private val subscriptionService: SubscriptionService
) {

    @GetMapping
    fun getStats(): ApiResponse<DashboardStatsDto> {
        val dash = dashboardService.getDashboard()
        val activeCount = subscriptionService.findActive().size

        val spendByCategory = dash.spendByCategory.map { (cat, amount) ->
            CategorySpendDto(
                categoryId = cat.id,
                categoryName = cat.name,
                categoryColor = cat.color,
                monthlyAmount = amount
            )
        }

        val upcomingRenewals = dash.upcomingRenewals.map { sub ->
            RenewalDto(
                subscriptionId = sub.id,
                name = sub.name,
                renewalDate = sub.renewalDate,
                price = sub.price,
                currency = sub.currency,
                billingCycle = sub.billingCycle.name
            )
        }

        return ApiResponse(data = DashboardStatsDto(
            totalMonthly = dash.totalMonthly,
            totalYearly = dash.totalYearly,
            activeCount = activeCount,
            alertCount = dash.alertRenewals.size,
            spendByCategory = spendByCategory,
            upcomingRenewals = upcomingRenewals
        ))
    }

    /**
     * Estadísticas del dashboard en formato para la app móvil KMM.
     * Devuelve gastoMensual, gastoAnual, totalSuscripciones y renovacionesProximas.
     */
    @GetMapping("/stats")
    fun getMobileStats(): ResponseEntity<DashboardMobileStatsDto> =
        ResponseEntity.ok(dashboardService.getDashboardStats())
}