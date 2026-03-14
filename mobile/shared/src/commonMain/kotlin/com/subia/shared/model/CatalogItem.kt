package com.subia.shared.model

import kotlinx.serialization.Serializable

/** Servicio del catálogo predefinido de SubIA (~80 servicios). */
@Serializable
data class CatalogItem(
    val id: Long,
    val nombre: String,
    val categoria: String? = null,
    val precioMensual: Double? = null,
    val precioAnual: Double? = null,
    val moneda: String = "EUR",
    val logoUrl: String? = null,
    val categoriaId: Long? = null
)
