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
    @SerialName("renewalAnchor") val fechaAncla: String? = null,
    /**
     * Cada cuántos meses se cobra (1, 2, 3, 6 o 12). Default 1 para leer cachés y backends
     * anteriores al campo; no se usa tal cual sino a través de [mesesPorCobro], que trata un
     * YEARLY con 1 como anual (12) y deja los semanales con su cálculo propio.
     */
    @SerialName("intervaloMeses") val intervaloMeses: Int = 1,
    /**
     * Equivalente mensual calculado por el backend (2 decimales). Solo informativo: la app
     * calcula con [equivalenteMensualInterno] para funcionar igual con la caché y sin red.
     */
    @SerialName("equivalenteMensual") val equivalenteMensual: Double? = null
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
    @SerialName("trialEndsAt") val fechaFinPrueba: String? = null,
    /**
     * Cada cuántos meses se cobra (1, 2, 3, 6 o 12). SIN valor por defecto a propósito: el Json
     * no codifica los valores por defecto, así que con `= 1` el mensual nunca viajaría y el
     * backend conservaría el intervalo guardado (no se podría volver de trimestral a mensual).
     * Además obliga a cada llamada a decidir el intervalo. En WEEKLY el backend lo ignora.
     */
    @SerialName("intervaloMeses") val intervaloMeses: Int
)

/**
 * Equivalente mensual del importe (importe / intervalo; semanal × 52 / 12…) con 4 decimales,
 * según la regla de [Periodicidad]. Único criterio para el Inicio, la lista y el resumen anual:
 * la lista trataba una semanal como si fuera mensual y su total no coincidía con el del Inicio.
 * Para SUMAR varios gastos usa [equivalenteMensualInterno] y redondea al final con
 * [Periodicidad.aCifraVisible]; para mostrar uno solo, redondea este valor a 2 decimales.
 */
fun Subscription.gastoMensualEquivalente(): Double = Periodicidad.aImporte(equivalenteMensualInterno())
