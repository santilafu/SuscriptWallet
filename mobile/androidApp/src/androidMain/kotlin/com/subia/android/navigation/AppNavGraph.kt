package com.subia.android.navigation

import kotlinx.serialization.Serializable

// Rutas de navegación con type-safety (Navigation Compose 2.8+)
@Serializable object LoginRoute
@Serializable object DashboardRoute
@Serializable object SuscripcionesRoute
@Serializable data class SuscripcionDetalleRoute(val id: Long)
/**
 * Formulario de alta/edición. [id] edita una suscripción existente; [prefillNombre] abre el
 * alta con el nombre ya puesto (y, si está en el catálogo, precio, ciclo y categoría).
 */
@Serializable data class SuscripcionFormRoute(val id: Long? = null, val prefillNombre: String? = null)
@Serializable object CategoriasRoute
@Serializable object CatalogoRoute
@Serializable object SettingsRoute
@Serializable object ResumenAnualRoute
@Serializable object OnboardingRoute
@Serializable object GmailScanRoute
