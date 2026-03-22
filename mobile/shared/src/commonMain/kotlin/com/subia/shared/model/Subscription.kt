package com.subia.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Suscripción del usuario a un servicio. */
@Serializable
data class Subscription(
    val id: Long = 0,
    @SerialName("name") val nombre: String,
    val descripcion: String = "",
    @SerialName("price") val precio: Double,
    val moneda: String = "EUR",
    @SerialName("billingCycle") val periodoFacturacion: String,   // MONTHLY | YEARLY | WEEKLY
    @SerialName("renewalDate") val fechaRenovacion: String,      // yyyy-MM-dd
    @SerialName("categoryId") val categoriaId: Long? = null,
    @SerialName("active") val activa: Boolean = true,
    @SerialName("notes") val notas: String = "",
    @SerialName("isTrial")     val esPrueba: Boolean = false,
    @SerialName("trialEndsAt") val fechaFinPrueba: String? = null
)

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
    @SerialName("notes") val notas: String = ""
)
