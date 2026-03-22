package com.subia.model

import java.math.BigDecimal

/**
 * Representa un servicio de suscripción conocido del catálogo integrado.
 *
 * No es una entidad JPA. Los datos se mantienen en memoria dentro de [com.subia.service.CatalogService]
 * y se usan para rellenar el formulario automáticamente cuando el usuario elige un servicio conocido.
 *
 * @property name       Nombre comercial del servicio (p. ej. "Netflix Estándar").
 * @property price      Precio publicado por el proveedor en la fecha indicada en CatalogService.
 * @property currency   Código ISO 4217 de la moneda (EUR, USD…).
 * @property billingCycle Periodicidad de cobro: MONTHLY, YEARLY o WEEKLY.
 * @property description Descripción breve del plan o tier.
 * @property categoryKey Clave interna que agrupa los servicios por tipo.
 *                       Valores posibles: "ia", "streaming", "software", "cloud".
 *                       Se usa para filtrar el catálogo cuando el usuario selecciona una categoría.
 */
data class CatalogItem(
    val name: String,
    val price: BigDecimal,
    val currency: String,
    val billingCycle: BillingCycle,
    val description: String,
    val categoryKey: String,
    val trialDays: Int? = null
)