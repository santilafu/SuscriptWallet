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
    @SerialName("notes") val notas: String = ""
)

/** Petición para crear o actualizar una suscripción. */
@Serializable
data class NuevaSuscripcionRequest(
    val nombre: String,
    val descripcion: String = "",
    val precio: Double,
    val moneda: String = "EUR",
    val periodoFacturacion: String,
    val fechaRenovacion: String,
    val categoriaId: Long? = null,
    val notas: String = ""
)
