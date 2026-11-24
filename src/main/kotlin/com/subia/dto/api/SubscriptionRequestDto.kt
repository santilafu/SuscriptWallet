package com.subia.dto.api

import com.subia.model.BillingCycle
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Cuerpo de alta/edición de suscripción en la API. Los límites replican los de la tabla
 * `subscriptions` (V1): sin ellos, un nombre de 101 caracteres o un precio de 9 cifras
 * llegaban a la base de datos y devolvían un 500. El precio admite 0, igual que la web y
 * SubscriptionService (antes la API exigía > 0 y la web no).
 */
data class SubscriptionRequestDto(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Size(max = 500) val description: String = "",
    @field:NotNull @field:PositiveOrZero @field:DecimalMax("99999999.99") val price: BigDecimal,
    @field:Pattern(regexp = "^[A-Za-z]{3}$", message = "debe ser un código ISO 4217 de 3 letras")
    val currency: String = "EUR",
    @field:NotNull val billingCycle: BillingCycle,
    @field:NotNull val renewalDate: LocalDate,
    val active: Boolean = true,
    @field:Size(max = 1000) val notes: String = "",
    @field:NotNull val categoryId: Long,
    val isTrial: Boolean = false,
    /** yyyy-MM-dd; vacío o null = sin fecha. Se parsea en el controlador (formato malo → 400). */
    val trialEndsAt: String? = null,
    /**
     * Cada cuántos meses se cobra: 1, 2, 3, 6 o 12 (otro valor → 400). OPCIONAL porque las apps
     * publicadas (≤ 2.17.8) no lo envían; si falta se deduce del ciclo o se conserva el guardado
     * (ver `Periodicidad.resolver`). Si llega, manda él y `billingCycle` se deriva.
     */
    val intervaloMeses: Int? = null,
    /**
     * Plazo del gasto (préstamos y financiaciones), yyyy-MM-dd. OPCIONALES porque las apps
     * ≤ 2.18.2 no los envían y el PUT reconstruye la entidad entera:
     * ausente/null → se conserva lo guardado (en un alta, sin fecha); "" → se borra; fecha → se
     * guarda. Formato inválido o inicio posterior al fin → 400 VALIDATION_ERROR.
     */
    val fechaInicio: String? = null,
    val fechaFin: String? = null
)
