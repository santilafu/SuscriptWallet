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
 * @property categoryKey Clave interna que agrupa los servicios por tipo (ver
 *                       [com.subia.service.CategoryMappingService.NAME_TO_KEY]): "ia", "streaming",
 *                       "hogar", "seguros", "telecos", "transporte"…
 *                       Se usa para filtrar el catálogo cuando el usuario selecciona una categoría.
 * @property priceAnnual Precio anual del servicio (si ofrece plan anual). null si no aplica.
 * @property iconUrl     URL directa al icono/logo del servicio. null si se usa el fallback por dominio.
 * @property variablePrice true cuando el importe depende del consumo o de la póliza (luz, gas, agua,
 *                       telecos con consumo, seguros…). En ese caso [price] es solo un importe
 *                       orientativo y la UI debe indicarlo. false para suscripciones de precio fijo.
 */
data class CatalogItem(
    val name: String,
    val price: BigDecimal,
    val currency: String,
    val billingCycle: BillingCycle,
    val description: String,
    val categoryKey: String,
    val trialDays: Int? = null,
    val domain: String? = null,
    val cancelUrl: String? = null,
    val priceAnnual: BigDecimal? = null,
    val iconUrl: String? = null,
    val variablePrice: Boolean = false
) : java.io.Serializable