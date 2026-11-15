package com.subia.android.util

import com.subia.shared.model.Subscription
import com.subia.shared.model.mesesPorCobro

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
    "fecha_fin_prueba",
    // Al final para no mover las columnas de quien ya lea el CSV por posición.
    "intervalo_meses"
)

/**
 * CSV en UTF-8 con BOM (sin él Excel lo abre como ANSI y "Música" sale como "MÃºsica").
 * Los textos libres se neutralizan contra inyección de fórmulas (=, +, -, @ al inicio).
 */
fun List<Subscription>.toCsv(): String {
    val sb = StringBuilder()
    sb.append('\uFEFF')
    sb.append(CSV_HEADER.joinToString(","))
    sb.append('\n')
    for (sub in this) {
        val row = listOf(
            sub.id.toString(),
            sinFormula(sub.nombre),
            sinFormula(sub.descripcion),
            sub.precio.toString(),
            sub.moneda,
            sub.periodoFacturacion,
            sub.fechaRenovacion,
            sub.categoriaId?.toString().orEmpty(),
            sub.activa.toString(),
            sinFormula(sub.notas),
            sub.esPrueba.toString(),
            sub.fechaFinPrueba.orEmpty(),
            // Meses entre cobros (1, 2, 3, 6 o 12); vacío en semanales, que no van por meses.
            sub.mesesPorCobro()?.toString().orEmpty()
        )
        sb.append(row.joinToString(",") { escapeCsvField(it) })
        sb.append('\n')
    }
    return sb.toString()
}

/** Antepone un apóstrofo si el texto empieza como una fórmula de hoja de cálculo. */
private fun sinFormula(texto: String): String =
    if (texto.isNotEmpty() && texto[0] in "=+-@\t\r") "'$texto" else texto

private fun escapeCsvField(value: String): String {
    val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
    if (!needsQuotes) return value
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}
