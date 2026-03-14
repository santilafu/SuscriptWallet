package com.subia.shared.model

import kotlinx.serialization.Serializable

/** Suscripción del usuario a un servicio. */
@Serializable
data class Subscription(
    val id: Long = 0,
    val nombre: String,
    val descripcion: String = "",
    val precio: Double,
    val moneda: String = "EUR",
    val periodoFacturacion: String,   // MONTHLY | YEARLY | WEEKLY
    val fechaRenovacion: String,      // yyyy-MM-dd
    val categoriaId: Long? = null,
    val activa: Boolean = true,
    val notas: String = ""
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
