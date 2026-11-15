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
    /** Próxima renovación (>= hoy del servidor), proyectada desde [renewalAnchor]. */
    val renewalDate: LocalDate,
    /** Fecha guardada de la que parte la serie de cobros (puede ser pasada). */
    val renewalAnchor: LocalDate = renewalDate,
    val active: Boolean,
    val notes: String,
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String,
    val isTrial: Boolean = false,
    val trialEndsAt: String? = null,
    /**
     * Cada cuántos meses se cobra (1, 2, 3, 6 o 12; 1 en las semanales). [billingCycle] sigue
     * siendo solo MONTHLY/YEARLY/WEEKLY (derivado) para que las apps antiguas lo entiendan.
     */
    val intervaloMeses: Int = 1,
    /** Gasto mensual equivalente (2 decimales), igual que suma el dashboard: 90 € trimestral → 30.00. */
    val equivalenteMensual: BigDecimal = BigDecimal.ZERO
)