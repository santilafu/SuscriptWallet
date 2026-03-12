package com.subia.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Ciclo de facturación de una suscripción.
 * Se usa en [Subscription.billingCycle] y en el cálculo de gastos del dashboard.
 */
enum class BillingCycle {
    /** Cobro mensual (el más habitual). */
    MONTHLY,

    /** Cobro anual (precio por año). */
    YEARLY,

    /** Cobro semanal. */
    WEEKLY
}

/**
 * Entidad JPA que representa una suscripción de pago.
 *
 * Almacena todos los datos necesarios para hacer seguimiento de una suscripción:
 * nombre, precio, ciclo de facturación, fecha de renovación, categoría y estado.
 *
 * Relación: pertenece a una [Category] (Many-to-One).
 * La tabla en base de datos se llama "subscriptions".
 */
@Entity
@Table(name = "subscriptions")
data class Subscription(

    /** Identificador único generado automáticamente por la base de datos. */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** Nombre del servicio (p. ej. "Netflix Estándar"). Máximo 100 caracteres. */
    val name: String,

    /** Descripción opcional del plan o tier contratado. */
    val description: String = "",

    /** Precio del ciclo de facturación (no necesariamente mensual). */
    val price: BigDecimal,

    /** Código ISO 4217 de la moneda (p. ej. "EUR", "USD"). */
    val currency: String = "EUR",

    /**
     * Periodicidad con que se cobra la suscripción.
     * Persiste como String en la columna para mayor legibilidad.
     */
    @Enumerated(EnumType.STRING)
    val billingCycle: BillingCycle,

    /** Fecha en que se renueva (cobra) la próxima vez. Se usa para alertas en el dashboard. */
    val renewalDate: LocalDate,

    /**
     * Categoría a la que pertenece la suscripción.
     * Se carga de forma EAGER porque siempre se necesita al mostrar listas y el dashboard.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    val category: Category,

    /** Indica si la suscripción está activa. Las inactivas se excluyen del cálculo del dashboard. */
    val active: Boolean = true,

    /** Notas personales libres sobre la suscripción. */
    val notes: String = ""
)