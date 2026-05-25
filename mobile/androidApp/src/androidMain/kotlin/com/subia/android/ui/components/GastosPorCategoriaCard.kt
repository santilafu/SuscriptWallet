package com.subia.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.subia.android.R

// Paleta de colores fija para los segmentos del gráfico
private val DonutColors = listOf(
    Color(0xFF6366F1), // indigo-500
    Color(0xFF0D9488), // teal-600
    Color(0xFFF59E0B), // amber-500
    Color(0xFF22C55E), // green-500
    Color(0xFFEF4444), // red-500
    Color(0xFF8B5CF6), // violet-500
)

/**
 * Tarjeta del Dashboard que muestra el gasto mensual normalizado por categoría
 * mediante un gráfico de donut implementado con Canvas.
 *
 * Se muestran hasta 6 categorías; el resto se agrupan como "Otros".
 * La etiqueta de cada categoría elimina el sufijo " (EUR)" / " (USD)" para mejorar
 * la legibilidad, manteniendo el valor numérico original.
 *
 * @param gastosPorCategoria Mapa de "NombreCategoria (moneda)" → gasto mensual.
 * @param modifier           Modificador opcional para la tarjeta raíz.
 */
@Composable
fun GastosPorCategoriaCard(
    gastosPorCategoria: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.spending_by_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (gastosPorCategoria.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.no_category_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                // Agrupar: máximo 5 entradas visibles + "Otros" si hay más de 6
                val entradasOrdenadas = gastosPorCategoria.entries.sortedByDescending { it.value }
                val segmentos: List<Pair<String, Double>> = if (entradasOrdenadas.size <= 6) {
                    entradasOrdenadas.map { (k, v) -> etiquetaSinMoneda(k) to v }
                } else {
                    val visibles = entradasOrdenadas.take(5).map { (k, v) -> etiquetaSinMoneda(k) to v }
                    val otros = entradasOrdenadas.drop(5).sumOf { it.value }
                    visibles + listOf(stringResource(R.string.others) to otros)
                }

                val total = segmentos.sumOf { it.second }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Gráfico donut ──────────────────────────────────────
                    DonutChart(
                        segmentos = segmentos,
                        total = total,
                        centerLabel = stringResource(R.string.monthly_label),
                        modifier = Modifier.size(140.dp)
                    )

                    // ── Leyenda ────────────────────────────────────────────
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        segmentos.forEachIndexed { index, (etiqueta, gasto) ->
                            LeyendaFila(
                                color = DonutColors[index % DonutColors.size],
                                etiqueta = etiqueta,
                                porcentaje = if (total > 0) (gasto / total * 100).toInt() else 0
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Gráfico de donut dibujado con Canvas.
 *
 * @param segmentos Lista de (etiqueta, valor) para cada sector.
 * @param total     Suma de todos los valores; se muestra en el centro.
 * @param modifier  Modificador de tamaño para el Canvas.
 */
@Composable
private fun DonutChart(
    segmentos: List<Pair<String, Double>>,
    total: Double,
    centerLabel: String = "mensual",
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = labelColor
    )
    val sublabelStyle = TextStyle(
        fontSize = 9.sp,
        color = labelColor.copy(alpha = 0.6f)
    )

    Canvas(modifier = modifier) {
        val canvasSize = minOf(size.width, size.height)
        val outerRadius = canvasSize / 2f
        val innerRadius = outerRadius * 0.55f
        val strokeWidth = outerRadius - innerRadius
        val arcRadius = innerRadius + strokeWidth / 2f
        val arcDiameter = arcRadius * 2f
        val topLeft = Offset(
            x = (size.width - arcDiameter) / 2f,
            y = (size.height - arcDiameter) / 2f
        )
        val arcSize = Size(arcDiameter, arcDiameter)
        val gapDegrees = 2f

        var startAngle = -90f
        segmentos.forEachIndexed { index, (_, valor) ->
            val sweep = if (total > 0) ((valor / total) * 360f).toFloat() else 0f
            val adjustedSweep = (sweep - gapDegrees).coerceAtLeast(0f)
            drawArc(
                color = DonutColors[index % DonutColors.size],
                startAngle = startAngle + gapDegrees / 2f,
                sweepAngle = adjustedSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweep
        }

        // Texto central: total formateado
        val totalFormateado = "%.2f".format(total)
        val measured = textMeasurer.measure(totalFormateado, labelStyle)
        val sublabelMeasured = textMeasurer.measure(centerLabel, sublabelStyle)
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = centerX - measured.size.width / 2f,
                y = centerY - measured.size.height / 2f - sublabelMeasured.size.height / 2f
            )
        )
        drawText(
            textLayoutResult = sublabelMeasured,
            topLeft = Offset(
                x = centerX - sublabelMeasured.size.width / 2f,
                y = centerY + measured.size.height / 2f - sublabelMeasured.size.height / 2f
            )
        )
    }
}

/**
 * Fila de leyenda: punto de color + nombre de categoría + porcentaje.
 *
 * @param color      Color del segmento correspondiente.
 * @param etiqueta   Nombre de la categoría (sin sufijo de moneda).
 * @param porcentaje Porcentaje redondeado al entero más cercano.
 */
@Composable
private fun LeyendaFila(
    color: Color,
    etiqueta: String,
    porcentaje: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$porcentaje%",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

/**
 * Elimina el sufijo de moneda entre paréntesis de una clave de categoría.
 * Ejemplo: "Streaming (EUR)" → "Streaming".
 *
 * @param clave Clave original del mapa de gastos por categoría.
 * @return Etiqueta de visualización sin sufijo de moneda.
 */
private fun etiquetaSinMoneda(clave: String): String =
    clave.replace(Regex("""\s*\([A-Z]{3}\)$"""), "").trim()
