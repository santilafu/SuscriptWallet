package com.subia.service

import com.subia.dto.DashboardDto
import com.subia.dto.DashboardMobileStatsDto
import com.subia.dto.ProximaRenovacionMobileDto
import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Currencies
import com.subia.model.Subscription
import com.subia.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Servicio de negocio que calcula los datos agregados del dashboard.
 *
 * Normaliza todos los precios a equivalentes mensuales y anuales para poder
 * comparar suscripciones con distintos ciclos de facturación.
 *
 * Las renovaciones próximas se calculan con la fecha PROYECTADA ([RenewalDates.nextRenewal]),
 * no con la `renewalDate` guardada, que el backend nunca avanza (N-12). Las listas devuelven
 * copias de la entidad con `renewalDate` ya proyectada (no se persisten: el servicio es de solo
 * lectura) para que las plantillas y la API muestren la fecha real sin cambios.
 *
 * "Hoy" se calcula con el [Clock] de la aplicación (zona Europe/Madrid por defecto, N-19).
 */
@Service
@Transactional(readOnly = true)
class DashboardService(
    private val repo: SubscriptionRepository,
    private val clock: Clock
) {

    companion object {
        /**
         * Equivalente mensual de una suscripción (escala interna, sin redondear a céntimos: eso
         * se hace solo en las cifras finales, ver [Periodicidad]).
         * MONTHLY/YEARLY: importe / meses por cobro (un anual con 12 da lo mismo que el antiguo /12).
         * WEEKLY: importe × 52 / 12 (el mismo factor que la app; ver [Periodicidad.equivalenteMensualSemanal]).
         *
         * Es público para que la API (`SubscriptionDto.equivalenteMensual`) y el gráfico de la web
         * usen EXACTAMENTE la misma fórmula que los totales del dashboard.
         */
        fun equivalenteMensual(s: Subscription): BigDecimal = when (s.billingCycle) {
            BillingCycle.WEEKLY -> Periodicidad.equivalenteMensualSemanal(s.price)
            else -> Periodicidad.equivalenteMensual(s.price, Periodicidad.mesesPorCobro(s.billingCycle, s.intervaloMeses))
        }
    }

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
        val today = LocalDate.now(clock)

        // Una prueba ya vencida cuenta como de pago (N-10)
        val paidActive = active.filter { !RenewalDates.isActiveTrial(it, today) }

        // Divisas presentes, ordenadas según el catálogo soportado (resto al final)
        val present = paidActive.map { it.currency }.toSet()
        val availableCurrencies = Currencies.SUPPORTED.keys
            .filter { it in present }
            .plus(present.filter { it !in Currencies.SUPPORTED.keys }.sorted())

        // Gasto mensual por divisa (para elegir la predeterminada y mantener compatibilidad)
        val monthlyByCurrency = paidActive
            .groupBy { it.currency }
            .mapValues { (_, subs) -> subs.sumOf { toMonthly(it) }.let(Periodicidad::aCifraVisible) }
        val yearlyByCurrency = paidActive
            .groupBy { it.currency }
            .mapValues { (_, subs) -> subs.sumOf { toYearly(it) }.let(Periodicidad::aCifraVisible) }

        val chosen = selectedCurrency?.takeIf { it in present }
            ?: mainCurrency(monthlyByCurrency)

        val inCurrency = paidActive.filter { it.currency == chosen }

        val totalMonthly = inCurrency.sumOf { toMonthly(it) }.let(Periodicidad::aCifraVisible)
        val totalYearly  = inCurrency.sumOf { toYearly(it) }.let(Periodicidad::aCifraVisible)

        val spendByCategory: Map<Category, BigDecimal> = inCurrency
            .groupBy { it.category }
            .mapValues { (_, subs) -> subs.sumOf { toMonthly(it) }.let(Periodicidad::aCifraVisible) }
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }

        val topSubscriptions = inCurrency
            .sortedByDescending { toMonthly(it) }
            .take(5)
        // Equivalente mensual de cada una (mismo orden): el gráfico de barras lo pinta tal cual en
        // vez de recalcularlo en JavaScript, que no conocía el intervalo de meses.
        val topMonthly = topSubscriptions.map { Periodicidad.aCifraVisible(toMonthly(it)) }

        val upcomingRenewals = renewingWithin(active, today, 30)
        val alertRenewals = upcomingRenewals.filter { !it.renewalDate.isAfter(today.plusDays(7)) }

        val alertTrials = active
            .filter { it.isTrial }
            .filter { s -> s.trialEndsAt?.let { !it.isBefore(today) && !it.isAfter(today.plusDays(7)) } == true }
            .sortedBy { it.trialEndsAt }

        return DashboardDto(
            totalMonthly, totalYearly, spendByCategory,
            upcomingRenewals, alertRenewals, alertTrials,
            monthlyByCurrency, yearlyByCurrency, topSubscriptions,
            topMonthly = topMonthly,
            selectedCurrency = chosen,
            currencySymbol = Currencies.symbol(chosen),
            availableCurrencies = availableCurrencies
        )
    }

    /**
     * Devuelve las estadísticas del dashboard en el formato que consume la app móvil KMM.
     *
     * Igual que la web: excluye las pruebas gratuitas vigentes y suma solo la divisa principal
     * (la de mayor gasto mensual); antes sumaba euros, dólares y yenes como si fueran lo mismo.
     */
    fun getDashboardStats(userId: Long): DashboardMobileStatsDto {
        val active = repo.findByUserIdAndActiveTrue(userId)
        val today = LocalDate.now(clock)

        val paidActive = active.filter { !RenewalDates.isActiveTrial(it, today) }
        val monthlyByCurrency = paidActive
            .groupBy { it.currency }
            .mapValues { (_, subs) -> subs.sumOf { toMonthly(it) } }
        val main = mainCurrency(monthlyByCurrency)
        val inCurrency = paidActive.filter { it.currency == main }

        val gastoMensual = inCurrency.sumOf { toMonthly(it) }
            .let(Periodicidad::aCifraVisible).toDouble()
        val gastoAnual = inCurrency.sumOf { toYearly(it) }
            .let(Periodicidad::aCifraVisible).toDouble()

        val renovaciones = renewingWithin(active, today, 30)
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
     * Suscripciones cuya próxima renovación proyectada cae en `[today, today + days]`,
     * como copias con `renewalDate` = fecha proyectada, ordenadas por fecha.
     */
    private fun renewingWithin(subs: List<Subscription>, today: LocalDate, days: Long): List<Subscription> {
        val limit = today.plusDays(days)
        return subs
            .map { it.copy(renewalDate = RenewalDates.nextRenewal(it, today)) }
            .filter { !it.renewalDate.isAfter(limit) }
            .sortedBy { it.renewalDate }
    }

    /** Divisa con mayor gasto mensual; EUR si no hay ninguna. */
    private fun mainCurrency(monthlyByCurrency: Map<String, BigDecimal>): String =
        monthlyByCurrency.maxByOrNull { it.value }?.key ?: "EUR"

    /** Equivalente mensual a escala interna; ver [equivalenteMensual]. */
    private fun toMonthly(s: Subscription): BigDecimal = equivalenteMensual(s)

    /**
     * Convierte el precio de una suscripción a su equivalente anual.
     * MONTHLY/YEARLY: importe × 12 / meses por cobro (exacto, porque todos dividen a 12).
     * WEEKLY: importe × 52, coherente con el mensual (importe × 52 / 12) × 12.
     */
    private fun toYearly(s: Subscription): BigDecimal = when (s.billingCycle) {
        BillingCycle.WEEKLY -> Periodicidad.totalAnualSemanal(s.price)
        else -> Periodicidad.totalAnual(s.price, Periodicidad.mesesPorCobro(s.billingCycle, s.intervaloMeses))
    }
}
