package com.subia.shared.repository

import com.subia.shared.model.Category
import com.subia.shared.model.NuevaCategoriaRequest
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiRoutes

/** Repositorio de categorías. */
class CategoryRepository(private val apiClient: ApiClient) {

    suspend fun getAll(): Result<List<Category>> =
        apiClient.get(ApiRoutes.CATEGORIES)

    suspend fun create(request: NuevaCategoriaRequest): Result<Category> =
        apiClient.post(ApiRoutes.CATEGORIES, request)
}
