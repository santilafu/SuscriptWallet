package com.subia.controller.api

import com.subia.dto.DashboardMobileStatsDto
import com.subia.dto.api.*
import com.subia.repository.UserRepository
import com.subia.service.DashboardService
import com.subia.service.SubscriptionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/dashboard")
class ApiDashboardController(
    private val dashboardService: DashboardService,
    private val subscriptionService: SubscriptionService,
    private val userRepository: UserRepository
) {

    private fun resolveUserId(jwt: Jwt): Long {
        val email = jwt.subject
        return userRepository.findByEmail(email)?.id
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")
    }

    @GetMapping
    fun getStats(@AuthenticationPrincipal jwt: Jwt): ApiResponse<DashboardStatsDto> {
        val userId = resolveUserId(jwt)
        val dash = dashboardService.getDashboard(userId)
        val activeCount = subscriptionService.findActive(userId).size

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
    fun getMobileStats(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<ApiResponse<DashboardMobileStatsDto>> {
        val userId = resolveUserId(jwt)
        return ResponseEntity.ok(ApiResponse(data = dashboardService.getDashboardStats(userId)))
    }
}
