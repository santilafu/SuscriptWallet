package com.subia.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Servicio del catálogo predefinido de SubIA (~80 servicios). */
@Serializable
data class CatalogItem(
    val id: Long = 0,
    @SerialName("name") val nombre: String = "",
    @SerialName("price") val precioMensual: Double? = null,
    @SerialName("priceAnnual") val precioAnual: Double? = null,
    @SerialName("currency") val moneda: String = "EUR",
    @SerialName("billingCycle") val periodoFacturacion: String = "",
    @SerialName("description") val descripcion: String = "",
    @SerialName("categoryKey") val categoriaKey: String = "",
    @SerialName("trialDays") val diasPrueba: Int? = null,
    @SerialName("cancelUrl") val cancelUrl: String? = null,
    @SerialName("domain") val domain: String? = null,
    @SerialName("iconUrl") val iconUrl: String? = null
) {
    fun hasBothCycles(): Boolean = precioMensual != null && precioAnual != null

    fun monthlyEquivalent(): Double? = precioAnual?.div(12.0) ?: precioMensual

    fun annualSavingsPercent(): Int? {
        val m = precioMensual ?: return null
        val a = precioAnual ?: return null
        if (m <= 0.0) return null
        return (((m * 12.0 - a) / (m * 12.0)) * 100.0).toInt().coerceAtLeast(0)
    }
}
