package com.subia.shared.network

/** Rutas de la API REST de SubIA. Deben coincidir con los endpoints del servidor Spring Boot. */
object ApiRoutes {
    private const val BASE = "/api"

    // Auth
    const val LOGIN = "$BASE/auth/login"
    const val REFRESH = "$BASE/auth/refresh"
    const val LOGOUT = "$BASE/auth/logout"

    // Dashboard
    const val DASHBOARD_STATS = "$BASE/dashboard/stats"

    // Suscripciones
    const val SUBSCRIPTIONS = "$BASE/subscriptions"
    fun subscription(id: Long) = "$SUBSCRIPTIONS/$id"

    // Categorías
    const val CATEGORIES = "$BASE/categories"
    fun category(id: Long) = "$CATEGORIES/$id"

    // Catálogo
    const val CATALOG = "$BASE/catalog"
    fun catalogByCategory(categoryId: Long) = "$CATALOG?categoryId=$categoryId"
}
