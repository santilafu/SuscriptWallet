package com.subia.android.util

import com.subia.shared.model.Subscription

private val CSV_HEADER = listOf(
    "id",
    "nombre",
    "descripcion",
    "precio",
    "moneda",
    "periodo_facturacion",
    "fecha_renovacion",
    "categoria_id",
    "activa",
    "notas",
    "es_prueba",
    "fecha_fin_prueba"
)

fun List<Subscription>.toCsv(): String {
    val sb = StringBuilder()
    sb.append(CSV_HEADER.joinToString(","))
    sb.append('\n')
    for (sub in this) {
        val row = listOf(
            sub.id.toString(),
            sub.nombre,
            sub.descripcion,
            sub.precio.toString(),
            sub.moneda,
            sub.periodoFacturacion,
            sub.fechaRenovacion,
            sub.categoriaId?.toString().orEmpty(),
            sub.activa.toString(),
            sub.notas,
            sub.esPrueba.toString(),
            sub.fechaFinPrueba.orEmpty()
        )
        sb.append(row.joinToString(",") { escapeCsvField(it) })
        sb.append('\n')
    }
    return sb.toString()
}

private fun escapeCsvField(value: String): String {
    val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
    if (!needsQuotes) return value
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}
