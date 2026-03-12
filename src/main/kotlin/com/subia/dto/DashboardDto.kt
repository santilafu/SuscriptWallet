package com.subia.dto

import com.subia.model.Category
import com.subia.model.Subscription
import java.math.BigDecimal

/**
 * Objeto de transferencia de datos (DTO) que agrupa toda la información del dashboard.
 *
 * [DashboardService] construye este objeto con los cálculos ya hechos y lo pasa
 * directamente al template Thymeleaf, que solo tiene que mostrar los valores.
 *
 * @property totalMonthly     Suma del gasto mensual equivalente de todas las suscripciones activas.
 *                            Las anuales se dividen entre 12 y las semanales se multiplican por 4,33.
 * @property totalYearly      Suma del gasto anual equivalente de todas las suscripciones activas.
 * @property spendByCategory  Gasto mensual agrupado por categoría, ordenado de mayor a menor.
 *                            Clave: objeto [Category]; valor: gasto mensual equivalente.
 * @property upcomingRenewals Suscripciones activas que renuevan en los próximos 30 días, ordenadas por fecha.
 * @property alertRenewals    Subconjunto de [upcomingRenewals] que renueva en los próximos 7 días.
 *                            Se usa para mostrar la alerta roja en la cabecera del dashboard.
 */
data class DashboardDto(
    val totalMonthly: BigDecimal,
    val totalYearly: BigDecimal,
    val spendByCategory: Map<Category, BigDecimal>,
    val upcomingRenewals: List<Subscription>,
    val alertRenewals: List<Subscription>
)