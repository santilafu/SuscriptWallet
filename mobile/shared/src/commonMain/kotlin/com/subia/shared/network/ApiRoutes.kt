package com.subia.shared.network

/** Rutas de la API REST de SubIA. Deben coincidir con los endpoints del servidor Spring Boot. */
object ApiRoutes {
    private const val BASE = "/api"

    // Auth
    const val LOGIN = "$BASE/auth/login"
    const val REFRESH = "$BASE/auth/refresh"
    const val LOGOUT = "$BASE/auth/logout"
    const val GOOGLE_AUTH = "$BASE/auth/google"

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

    // Escaneo de Gmail
    const val GMAIL_SCAN_TICKET = "$BASE/gmail/scan/ticket"
    const val GMAIL_SCAN_RESULTS = "$BASE/gmail/scan/results"
    const val GMAIL_SCAN_ADD = "$BASE/gmail/scan/add"
    fun gmailScanTicket(months: Int) = "$GMAIL_SCAN_TICKET?months=$months"
}
