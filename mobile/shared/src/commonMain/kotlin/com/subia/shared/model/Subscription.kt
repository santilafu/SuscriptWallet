package com.subia.shared.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Suscripción del usuario a un servicio.
 *
 * Las claves JSON son las de `SubscriptionDto` del backend. `description` y `currency` no
 * tenían `@SerialName`: la app esperaba "descripcion"/"moneda", el backend envía
 * "description"/"currency", y todas las suscripciones salían en EUR y sin descripción. Al
 * editar, el PUT devolvía ese EUR al servidor y pisaba la divisa real. [JsonNames] acepta
 * además los nombres antiguos para leer las cachés guardadas por versiones anteriores.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Subscription(
    val id: Long = 0,
    @SerialName("name") val nombre: String,
    @SerialName("description") @JsonNames("descripcion") val descripcion: String = "",
    @SerialName("price") val precio: Double,
    @SerialName("currency") @JsonNames("moneda") val moneda: String = "EUR",
    @SerialName("billingCycle") val periodoFacturacion: String,   // MONTHLY | YEARLY | WEEKLY
    @SerialName("renewalDate") val fechaRenovacion: String,      // yyyy-MM-dd
    @SerialName("categoryId") val categoriaId: Long? = null,
    @SerialName("active") val activa: Boolean = true,
    @SerialName("notes") val notas: String = "",
    @SerialName("isTrial")     val esPrueba: Boolean = false,
    @SerialName("trialEndsAt") val fechaFinPrueba: String? = null,
    /**
     * Fecha guardada de la que parte la serie de cobros (R3-01/R3-02). `renewalDate` llega ya
     * proyectada con el "hoy" del SERVIDOR (Europe/Madrid): proyectar desde ella recorta un ancla
     * del 31 al 30 y, si el servidor va un día por delante del móvil, se salta el cobro de "hoy"
     * local. Los backends anteriores no lo envían (null → se usa [fechaRenovacion]).
     */
    @SerialName("renewalAnchor") val fechaAncla: String? = null
) {
    /** Ancla para proyectar: [fechaAncla] si llega, si no [fechaRenovacion]. */
    val anclaRenovacion: String
        get() = fechaAncla?.trim()?.takeIf { it.isNotEmpty() } ?: fechaRenovacion
}

/** Petición para crear o actualizar una suscripción. */
@Serializable
data class NuevaSuscripcionRequest(
    @SerialName("name") val nombre: String,
    @SerialName("description") val descripcion: String = "",
    @SerialName("price") val precio: Double,
    @SerialName("currency") val moneda: String = "EUR",
    @SerialName("billingCycle") val periodoFacturacion: String,
    @SerialName("renewalDate") val fechaRenovacion: String,
    @SerialName("categoryId") val categoriaId: Long? = null,
    /**
     * Estado de la suscripción que se está editando. Sin este campo el backend
     * (`SubscriptionRequestDto.active = true` por defecto) reactivaba al editar desde la app
     * una suscripción desactivada en la web. `false` se serializa siempre (no es el default).
     */
    @SerialName("active") val activa: Boolean = true,
    @SerialName("notes") val notas: String = "",
    @SerialName("isTrial")     val esPrueba: Boolean = false,
    @SerialName("trialEndsAt") val fechaFinPrueba: String? = null
)

/**
 * Equivalente mensual del importe según el ciclo (anual / 12, trimestral / 3, semanal × 52 / 12).
 * Único criterio para el Inicio, la lista y el resumen anual: la lista trataba una semanal
 * como si fuera mensual y su total no coincidía con el del Inicio.
 */
fun Subscription.gastoMensualEquivalente(): Double = when (periodoFacturacion.trim().uppercase()) {
    "YEARLY", "ANNUAL", "ANNUALLY" -> precio / 12.0
    "QUARTERLY" -> precio / 3.0
    "WEEKLY" -> precio * 52.0 / 12.0
    "BIWEEKLY" -> precio * 26.0 / 12.0
    "DAILY" -> precio * 365.0 / 12.0
    else -> precio
}
