package com.subia.android.navigation

import kotlinx.serialization.Serializable

// Rutas de navegación con type-safety (Navigation Compose 2.8+)
@Serializable object LoginRoute
@Serializable object DashboardRoute
@Serializable object SuscripcionesRoute
@Serializable data class SuscripcionDetalleRoute(val id: Long)
@Serializable data class SuscripcionFormRoute(val id: Long? = null)
@Serializable object CategoriasRoute
@Serializable object CatalogoRoute
@Serializable object SettingsRoute
