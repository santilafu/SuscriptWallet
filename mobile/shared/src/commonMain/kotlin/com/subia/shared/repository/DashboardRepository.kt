package com.subia.shared.repository

import com.subia.shared.model.DashboardSummary
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiRoutes

/** Repositorio para el panel de estadísticas de gasto. */
class DashboardRepository(private val apiClient: ApiClient) {
    /** Obtiene el resumen de gasto mensual, anual y renovaciones próximas. */
    suspend fun getStats(): Result<DashboardSummary> =
        apiClient.get(ApiRoutes.DASHBOARD_STATS)
}
