package com.subia.shared.repository

import com.subia.shared.model.CatalogItem
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiRoutes

/** Repositorio del catálogo de servicios predefinidos (~80 servicios). */
class CatalogRepository(private val apiClient: ApiClient) {

    suspend fun getAll(): Result<List<CatalogItem>> =
        apiClient.get(ApiRoutes.CATALOG)

    suspend fun getByCategory(categoryId: Long): Result<List<CatalogItem>> =
        apiClient.get(ApiRoutes.catalogByCategory(categoryId))
}
