package com.subia.dto

/**
 * DTO de estadísticas del dashboard para la app móvil.
 * Devuelto por GET /api/dashboard/stats.
 */
data class DashboardMobileStatsDto(
    val gastoMensual: Double,
    val gastoAnual: Double,
    val totalSuscripciones: Int,
    val renovacionesProximas: List<ProximaRenovacionMobileDto>
)

/** Renovación próxima para el panel de la app móvil. */
data class ProximaRenovacionMobileDto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val fechaRenovacion: String,   // yyyy-MM-dd
    val diasRestantes: Int
)
