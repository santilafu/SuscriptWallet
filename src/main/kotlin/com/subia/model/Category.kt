package com.subia.model

import jakarta.persistence.*

/**
 * Entidad JPA que representa una categoría de suscripciones.
 *
 * Las categorías agrupan las suscripciones visualmente (p. ej. "IA", "Streaming", "Cloud").
 * Cada categoría tiene un color hexadecimal y un icono emoji que se muestran como etiqueta
 * en el dashboard y en las listas.
 *
 * Relación: una categoría puede tener muchas suscripciones (One-to-Many).
 * La tabla en base de datos se llama "categories".
 */
@Entity
@Table(name = "categories")
data class Category(

    /** Identificador único generado automáticamente por la base de datos. */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** Nombre visible de la categoría (p. ej. "Streaming"). Máximo 100 caracteres. */
    val name: String,

    /** Color de fondo de la etiqueta en formato hexadecimal (p. ej. "#ef4444"). */
    val color: String = "#6c757d",

    /** Icono emoji que acompaña al nombre (p. ej. "🎬"). Puede estar vacío. */
    val icon: String = ""
)