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

/** Petición para crear una categoría. */
@Serializable
data class NuevaCategoriaRequest(
    val nombre: String,
    val color: String = "#6c757d",
    val icon: String = ""
)
