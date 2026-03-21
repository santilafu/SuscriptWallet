package com.subia.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tarjeta del Dashboard que muestra el gasto mensual normalizado por categoría
 * mediante un gráfico de barras horizontales implementado con composables nativos.
 *
 * Cada barra representa el gasto mensual de una categoría (o categoría + divisa si hay
 * varias divisas). El ancho de cada barra es proporcional al máximo valor del conjunto.
 *
 * @param gastosPorCategoria Mapa de "NombreCategoria (moneda)" → gasto mensual.
 *                           Las claves con una única divisa pueden omitir el sufijo de moneda.
 * @param modifier           Modificador opcional para la tarjeta raíz.
 */
@Composable
fun GastosPorCategoriaCard(
    gastosPorCategoria: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gasto por categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (gastosPorCategoria.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No hay datos de categorías",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                val maxValor = gastosPorCategoria.values.maxOrNull() ?: 1.0
                val barraColores = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.error
                )

                gastosPorCategoria.entries.forEachIndexed { index, (categoria, gasto) ->
                    FilaCategoria(
                        categoria = categoria,
                        gasto = gasto,
                        proporcion = (gasto / maxValor).toFloat().coerceIn(0.05f, 1.0f),
                        colorBarra = barraColores[index % barraColores.size]
                    )
                    if (index < gastosPorCategoria.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Fila individual de la gráfica: nombre de categoría, barra proporcional e importe.
 *
 * @param categoria  Nombre de la categoría (puede incluir sufijo de moneda entre paréntesis).
 * @param gasto      Importe mensual normalizado.
 * @param proporcion Fracción [0.0–1.0] respecto al máximo del conjunto (ancho relativo de la barra).
 * @param colorBarra Color de la barra.
 */
@Composable
private fun FilaCategoria(
    categoria: String,
    gasto: Double,
    proporcion: Float,
    colorBarra: androidx.compose.ui.graphics.Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoria,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "%.2f".format(gasto),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(proporcion)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colorBarra)
        )
    }
}
