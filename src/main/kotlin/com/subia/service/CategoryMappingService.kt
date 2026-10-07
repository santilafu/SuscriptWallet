package com.subia.service

import org.springframework.stereotype.Service

/**
 * Único punto de verdad para traducir el nombre (en castellano) de una categoría de la base de
 * datos a la clave interna que usa el catálogo ([com.subia.model.CatalogItem.categoryKey]).
 *
 * Antes este mapeo estaba triplicado en `GmailController`, `ApiGmailController` y
 * `CatalogBrowserController` (hallazgo B-01 de la auditoría). Ahora todos consultan aquí.
 *
 * La parte pura (nombre → clave) vive en el `companion object` para poder usarla sin Spring
 * (p. ej. desde [CatalogService] o en tests), y la parte que necesita la base de datos
 * (clave → id de la entidad) es el método de instancia [categoryKeyToId].
 */
@Service
class CategoryMappingService(private val categoryService: CategoryService) {

    /**
     * Devuelve el mapa clave de catálogo → id de la categoría JPA correspondiente.
     * Las categorías cuyo nombre no esté en [NAME_TO_KEY] (creadas a mano por el usuario)
     * se ignoran: el catálogo no tiene items para ellas.
     */
    fun categoryKeyToId(): Map<String, Long> =
        categoryService.findAll()
            .mapNotNull { cat -> keyFor(cat.name)?.let { key -> key to cat.id } }
            .toMap()

    companion object {
        /**
         * Nombre de la categoría tal y como está sembrado en Flyway (V2, V4, V5, V13) → clave.
         * Si se añade una categoría nueva al seed, hay que añadirla también aquí.
         */
        val NAME_TO_KEY: Map<String, String> = linkedMapOf(
            "IA"                  to "ia",
            "Streaming"           to "streaming",
            "Música"              to "musica",
            "Software"            to "software",
            "Cloud"               to "cloud",
            "Gaming"              to "gaming",
            "Seguridad"           to "seguridad",
            "Noticias y Lectura"  to "noticias",
            "Salud y Deporte"     to "salud",
            "Desarrollo"          to "desarrollo",
            "Prueba gratuita"     to "prueba",
            "Finanzas"            to "finanzas",
            "Educación"           to "educacion",
            "Creatividad y foto"  to "creatividad",
            "Citas y social"      to "citas",
            // Recibos recurrentes (V13): suministros, seguros, telecos y transporte.
            "Hogar y suministros" to "hogar",
            "Seguros"             to "seguros",
            "Telecomunicaciones"  to "telecos",
            "Transporte"          to "transporte"
        )

        /** Clave → nombre sembrado (inverso de [NAME_TO_KEY]). */
        val KEY_TO_NAME: Map<String, String> = NAME_TO_KEY.entries.associate { (name, key) -> key to name }

        /**
         * Clave de catálogo para el nombre de una categoría, o null si no es una de las sembradas.
         * Ignora espacios sobrantes alrededor del nombre.
         */
        fun keyFor(categoryName: String): String? = NAME_TO_KEY[categoryName.trim()]
    }
}
