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
    @SerialName("iconUrl") val iconUrl: String? = null,
    /**
     * `true` en recibos de importe variable (suministros, telecos, seguros): el
     * precio es orientativo. Opcional con default `false` porque las versiones
     * del backend anteriores a los recibos recurrentes no lo envían.
     */
    @SerialName("variablePrice") val variablePrice: Boolean = false
) {
    fun hasBothCycles(): Boolean = precioMensual != null && precioAnual != null

    fun monthlyEquivalent(): Double? = precioAnual?.div(12.0) ?: precioMensual

    /**
     * Precio para el [ciclo] que elige el usuario. Ojo: `price` ([precioMensual]) es el precio
     * del ciclo propio del servicio ([periodoFacturacion]); en un seguro anual son 450 €/año,
     * no al mes. Si el ciclo pedido no es el del servicio se convierte (×12 o ÷12).
     */
    fun precioPara(ciclo: BillingCycle): Double? {
        val precioCiclo = precioMensual
        val cicloPropio = BillingCycle.fromWire(periodoFacturacion)
        return when (ciclo) {
            BillingCycle.YEARLY -> precioAnual
                ?: precioCiclo?.let { if (cicloPropio == BillingCycle.YEARLY) it else it * 12.0 }
            BillingCycle.MONTHLY -> precioCiclo?.let { if (cicloPropio == BillingCycle.YEARLY) it / 12.0 else it }
                ?: precioAnual?.div(12.0)
        }
    }

    fun annualSavingsPercent(): Int? {
        val m = precioMensual ?: return null
        val a = precioAnual ?: return null
        if (m <= 0.0) return null
        return (((m * 12.0 - a) / (m * 12.0)) * 100.0).toInt().coerceAtLeast(0)
    }
}
