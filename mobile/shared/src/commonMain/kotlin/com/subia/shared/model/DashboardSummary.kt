package com.subia.shared.model

import kotlinx.serialization.Serializable

/** Resumen del panel principal: gasto total y renovaciones próximas. */
@Serializable
data class DashboardSummary(
    // Defaults para tolerar respuestas del backend con campos ausentes o null
    // (con coerceInputValues = true esto evita SerializationException → crash/pantalla de error).
    val gastoMensual: Double = 0.0,
    val gastoAnual: Double = 0.0,
    val totalSuscripciones: Int = 0,
    val renovacionesProximas: List<ProximaRenovacion> = emptyList()
)

/** Suscripción próxima a renovar, para el panel principal. */
@Serializable
data class ProximaRenovacion(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val fechaRenovacion: String,   // yyyy-MM-dd
    val diasRestantes: Int
)
