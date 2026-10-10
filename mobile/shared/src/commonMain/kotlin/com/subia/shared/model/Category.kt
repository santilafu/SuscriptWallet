package com.subia.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Categoría para clasificar suscripciones. */
@Serializable
data class Category(
    val id: Long = 0,
    @SerialName("name") val nombre: String,
    val color: String = "#6c757d",
    val icon: String = ""
)

/**
 * Petición para crear una categoría (`CategoryRequestDto` del backend: name, color e icon,
 * los tres `@NotBlank`). Antes se enviaba "nombre" y un icono vacío: el servidor respondía
 * 400 siempre. `encodeDefaults` está desactivado, por eso color e icono no llevan default.
 */
@Serializable
data class NuevaCategoriaRequest(
    @SerialName("name") val nombre: String,
    val color: String,
    val icon: String
)
