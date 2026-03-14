package com.subia.shared.repository

import com.subia.shared.model.NuevaSuscripcionRequest
import com.subia.shared.model.Subscription
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiRoutes

/** Repositorio CRUD de suscripciones. */
class SubscriptionRepository(private val apiClient: ApiClient) {

    suspend fun getAll(): Result<List<Subscription>> =
        apiClient.get(ApiRoutes.SUBSCRIPTIONS)

    suspend fun getById(id: Long): Result<Subscription> =
        apiClient.get(ApiRoutes.subscription(id))

    suspend fun create(request: NuevaSuscripcionRequest): Result<Subscription> =
        apiClient.post(ApiRoutes.SUBSCRIPTIONS, request)

    suspend fun update(id: Long, request: NuevaSuscripcionRequest): Result<Subscription> =
        apiClient.put(ApiRoutes.subscription(id), request)

    suspend fun delete(id: Long): Result<Unit> =
        apiClient.delete(ApiRoutes.subscription(id))
}
