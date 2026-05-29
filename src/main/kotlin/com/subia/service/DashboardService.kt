package com.subia.service

import com.subia.dto.DashboardDto
import com.subia.dto.DashboardMobileStatsDto
import com.subia.dto.ProximaRenovacionMobileDto
import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Subscription
import com.subia.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
     * Construye y devuelve el [DashboardDto] filtrado por userId.
     *
     * Los totales y gráficos se calculan sobre UNA sola divisa para no mezclar importes
     * de monedas distintas (no se aplica conversión de tasas). La divisa se elige así:
     * la pedida en [selectedCurrency] si existe entre las del usuario; si no, la de mayor
     * gasto mensual; y como último recurso, EUR.
     */
    fun getDashboard(userId: Long, selectedCurrency: String? = null): DashboardDto {
        val active = repo.findByUserIdAndActiveTrue(userId)
        val today = LocalDate.now()

        val paidActive = active.filter { !it.isTrial }

        // Divisas presentes, ordenadas según el catálogo soportado (resto al final)
        val present = paidActive.map { it.currency }.toSet()
        val availableCurrencies = com.subia.model.Currencies.SUPPORTED.keys
            .filter { it in present }
            .plus(present.filter { it !in com.subia.model.Currencies.SUPPORTED.keys }.sorted())

        // Gasto mensual por divisa (para elegir la predeterminada y mantener compatibilidad)
        val monthlyByCurrency = paidActive
            .groupBy { it.currency }
            .mapValues { (_, subs) -> subs.sumOf { toMonthly(it) }.setScale(2, RoundingMode.HALF_UP) }
        val yearlyByCurrency = paidActive
            .groupBy { it.currency }
            .mapValues { (_, subs) -> subs.sumOf { toYearly(it) }.setScale(2, RoundingMode.HALF_UP) }

        val chosen = selectedCurrency?.takeIf { it in present }
            ?: monthlyByCurrency.maxByOrNull { it.value }?.key
            ?: "EUR"

        val inCurrency = paidActive.filter { it.currency == chosen }

        val totalMonthly = inCurrency.sumOf { toMonthly(it) }.setScale(2, RoundingMode.HALF_UP)
        val totalYearly  = inCurrency.sumOf { toYearly(it) }.setScale(2, RoundingMode.HALF_UP)

        val spendByCategory: Map<Category, BigDecimal> = inCurrency
            .groupBy { it.category }
            .mapValues { (_, subs) -> subs.sumOf { toMonthly(it) }.setScale(2, RoundingMode.HALF_UP) }
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }

        val topSubscriptions = inCurrency
            .sortedByDescending { toMonthly(it) }
            .take(5)

        val upcomingRenewals = repo.findActiveRenewingBetweenForUser(userId, today, today.plusDays(30))
            .sortedBy { it.renewalDate }

        val alertRenewals = repo.findActiveRenewingBetweenForUser(userId, today, today.plusDays(7))
            .sortedBy { it.renewalDate }

        val alertTrials = repo.findActiveTrialsExpiringBetweenForUser(userId, today, today.plusDays(7))
            .sortedBy { it.trialEndsAt }

        return DashboardDto(
            totalMonthly, totalYearly, spendByCategory,
            upcomingRenewals, alertRenewals, alertTrials,
            monthlyByCurrency, yearlyByCurrency, topSubscriptions,
            selectedCurrency = chosen,
            currencySymbol = com.subia.model.Currencies.symbol(chosen),
            availableCurrencies = availableCurrencies
        )
    }

    /**
     * Construye y devuelve el [DashboardDto] sin filtro por usuario (legado).
     */
    fun getDashboard(): DashboardDto {
        val active = repo.findByActiveTrue()
        val today = LocalDate.now()

        val paidActive = active.filter { !it.isTrial }

        val totalMonthly = paidActive.sumOf { toMonthly(it) }.setScale(2, RoundingMode.HALF_UP)
        val totalYearly  = paidActive.sumOf { toYearly(it) }.setScale(2, RoundingMode.HALF_UP)

        val spendByCategory: Map<Category, BigDecimal> = paidActive
            .groupBy { it.category }
            .mapValues { (_, subs) -> subs.sumOf { toMonthly(it) }.setScale(2, RoundingMode.HALF_UP) }
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }

        val upcomingRenewals = repo.findActiveRenewingBetween(today, today.plusDays(30))
            .sortedBy { it.renewalDate }

        val alertRenewals = repo.findActiveRenewingBetween(today, today.plusDays(7))
            .sortedBy { it.renewalDate }

        val alertTrials = repo.findActiveTrialsExpiringBetween(today, today.plusDays(7))
            .sortedBy { it.trialEndsAt }

        return DashboardDto(totalMonthly, totalYearly, spendByCategory, upcomingRenewals, alertRenewals, alertTrials)
    }

    /**
     * Devuelve las estadísticas del dashboard en el formato que consume la app móvil KMM.
     * Filtrado por userId.
     */
    fun getDashboardStats(userId: Long): DashboardMobileStatsDto {
        val active = repo.findByUserIdAndActiveTrue(userId)
        val today = LocalDate.now()

        val gastoMensual = active.sumOf { toMonthly(it) }
            .setScale(2, RoundingMode.HALF_UP).toDouble()
        val gastoAnual = active.sumOf { toYearly(it) }
            .setScale(2, RoundingMode.HALF_UP).toDouble()

        val renovaciones = repo.findActiveRenewingBetweenForUser(userId, today, today.plusDays(30))
            .sortedBy { it.renewalDate }
            .take(10)
            .map { sub ->
                ProximaRenovacionMobileDto(
                    id = sub.id,
                    nombre = sub.name,
                    precio = sub.price.toDouble(),
                    fechaRenovacion = sub.renewalDate.toString(),
                    diasRestantes = ChronoUnit.DAYS.between(today, sub.renewalDate).toInt()
                )
            }

        return DashboardMobileStatsDto(
            gastoMensual = gastoMensual,
            gastoAnual = gastoAnual,
            totalSuscripciones = active.size,
            renovacionesProximas = renovaciones
        )
    }

    /**
     * Devuelve las estadísticas del dashboard sin filtro por usuario (legado).
     */
    fun getDashboardStats(): DashboardMobileStatsDto {
        val active = repo.findByActiveTrue()
        val today = LocalDate.now()

        val gastoMensual = active.sumOf { toMonthly(it) }
            .setScale(2, RoundingMode.HALF_UP).toDouble()
        val gastoAnual = active.sumOf { toYearly(it) }
            .setScale(2, RoundingMode.HALF_UP).toDouble()

        val renovaciones = repo.findActiveRenewingBetween(today, today.plusDays(30))
            .sortedBy { it.renewalDate }
            .take(10)
            .map { sub ->
                ProximaRenovacionMobileDto(
                    id = sub.id,
                    nombre = sub.name,
                    precio = sub.price.toDouble(),
                    fechaRenovacion = sub.renewalDate.toString(),
                    diasRestantes = ChronoUnit.DAYS.between(today, sub.renewalDate).toInt()
                )
            }

        return DashboardMobileStatsDto(
            gastoMensual = gastoMensual,
            gastoAnual = gastoAnual,
            totalSuscripciones = active.size,
            renovacionesProximas = renovaciones
        )
    }

    /**
     * Convierte el precio de una suscripción a su equivalente mensual.
     */
    private fun toMonthly(s: Subscription): BigDecimal = when (s.billingCycle) {
        BillingCycle.MONTHLY -> s.price
        BillingCycle.YEARLY  -> s.price.divide(MONTHS_IN_YEAR, 4, RoundingMode.HALF_UP)
        BillingCycle.WEEKLY  -> s.price.multiply(WEEKLY_FACTOR)
    }

    /**
     * Convierte el precio de una suscripción a su equivalente anual.
     */
    private fun toYearly(s: Subscription): BigDecimal = when (s.billingCycle) {
        BillingCycle.MONTHLY -> s.price.multiply(MONTHS_IN_YEAR)
        BillingCycle.YEARLY  -> s.price
        BillingCycle.WEEKLY  -> s.price.multiply(WEEKS_IN_YEAR)
    }
}
