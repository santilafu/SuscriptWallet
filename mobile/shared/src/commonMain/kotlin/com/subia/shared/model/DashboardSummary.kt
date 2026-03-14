package com.subia.shared.model

import kotlinx.serialization.Serializable

/** Resumen del panel principal: gasto total y renovaciones próximas. */
@Serializable
data class DashboardSummary(
    val gastoMensual: Double,
    val gastoAnual: Double,
    val totalSuscripciones: Int,
    val renovacionesProximas: List<ProximaRenovacion>
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
