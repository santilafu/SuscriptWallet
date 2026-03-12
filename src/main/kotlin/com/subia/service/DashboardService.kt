package com.subia.service

import com.subia.dto.DashboardDto
import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Subscription
import com.subia.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Servicio de negocio que calcula los datos agregados del dashboard.
 *
 * Normaliza todos los precios a equivalentes mensuales y anuales para poder
 * comparar suscripciones con distintos ciclos de facturación.
 *
 * Se marca como [readOnly = true] porque solo lee datos; esto permite que el ORM
 * aplique optimizaciones de rendimiento (sin seguimiento de cambios en las entidades).
 */
@Service
@Transactional(readOnly = true)
class DashboardService(private val repo: SubscriptionRepository) {

    // Factores de conversión para normalizar ciclos de facturación
    private val WEEKLY_FACTOR = BigDecimal("4.33")   // semanas promedio por mes
    private val MONTHS_IN_YEAR = BigDecimal("12")
    private val WEEKS_IN_YEAR = BigDecimal("52")

    /**
     * Construye y devuelve el [DashboardDto] con todos los datos necesarios para la pantalla principal.
     *
     * Pasos:
     * 1. Carga todas las suscripciones activas.
     * 2. Calcula el gasto mensual y anual equivalente total.
     * 3. Agrupa el gasto mensual por categoría (ordenado de mayor a menor).
     * 4. Filtra las renovaciones próximas a 30 días y a 7 días.
     */
    fun getDashboard(): DashboardDto {
        val active = repo.findByActiveTrue()
        val today = LocalDate.now()

        // Suma de todos los gastos normalizados a mensual y anual
        val totalMonthly = active.sumOf { toMonthly(it) }.setScale(2, RoundingMode.HALF_UP)
        val totalYearly  = active.sumOf { toYearly(it) }.setScale(2, RoundingMode.HALF_UP)

        // Agrupa suscripciones activas por categoría y suma su gasto mensual equivalente
        // El mapa resultante está ordenado de mayor a menor gasto
        val spendByCategory: Map<Category, BigDecimal> = active
            .groupBy { it.category }
            .mapValues { (_, subs) -> subs.sumOf { toMonthly(it) }.setScale(2, RoundingMode.HALF_UP) }
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }

        // Renovaciones en los próximos 30 días (para la tabla del dashboard)
        val upcomingRenewals = repo.findActiveRenewingBetween(today, today.plusDays(30))
            .sortedBy { it.renewalDate }

        // Renovaciones en los próximos 7 días (para la alerta roja en la cabecera)
        val alertRenewals = repo.findActiveRenewingBetween(today, today.plusDays(7))
            .sortedBy { it.renewalDate }

        return DashboardDto(totalMonthly, totalYearly, spendByCategory, upcomingRenewals, alertRenewals)
    }

    /**
     * Convierte el precio de una suscripción a su equivalente mensual.
     *
     * - MONTHLY: el precio ya es mensual, se devuelve tal cual.
     * - YEARLY:  se divide entre 12 meses.
     * - WEEKLY:  se multiplica por 4,33 (promedio de semanas por mes).
     */
    private fun toMonthly(s: Subscription): BigDecimal = when (s.billingCycle) {
        BillingCycle.MONTHLY -> s.price
        BillingCycle.YEARLY  -> s.price.divide(MONTHS_IN_YEAR, 4, RoundingMode.HALF_UP)
        BillingCycle.WEEKLY  -> s.price.multiply(WEEKLY_FACTOR)
    }

    /**
     * Convierte el precio de una suscripción a su equivalente anual.
     *
     * - MONTHLY: se multiplica por 12.
     * - YEARLY:  el precio ya es anual, se devuelve tal cual.
     * - WEEKLY:  se multiplica por 52 semanas.
     */
    private fun toYearly(s: Subscription): BigDecimal = when (s.billingCycle) {
        BillingCycle.MONTHLY -> s.price.multiply(MONTHS_IN_YEAR)
        BillingCycle.YEARLY  -> s.price
        BillingCycle.WEEKLY  -> s.price.multiply(WEEKS_IN_YEAR)
    }
}
