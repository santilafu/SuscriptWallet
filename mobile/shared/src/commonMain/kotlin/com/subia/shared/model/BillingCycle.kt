package com.subia.shared.model

/**
 * Ciclo de facturación interno al flujo del formulario de catálogo.
 *
 * No se serializa en JSON — el modelo [CatalogItem] sigue usando
 * [CatalogItem.periodoFacturacion] como [String] para no romper el wire format.
 * Este enum solo representa la elección del usuario en el modal
 * "¿Mensual o anual?" y el pre-rellenado del formulario.
 */
enum class BillingCycle(val wire: String) {
    MONTHLY("MONTHLY"),
    YEARLY("YEARLY");

    companion object {
        fun fromWire(s: String?): BillingCycle =
            if (s.equals("YEARLY", ignoreCase = true)) YEARLY else MONTHLY
    }
}
