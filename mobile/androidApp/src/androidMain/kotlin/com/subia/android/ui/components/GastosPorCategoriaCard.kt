package com.subia.android.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.R
import com.subia.shared.viewmodel.GastoCategoria

private const val MAX_FILAS_POR_DIVISA = 8

/**
 * Gasto mensual normalizado por categoría como barras horizontales ordenadas de mayor a
 * menor. Es una sola serie nominal, así que todas las barras llevan el mismo acento (regla
 * dataviz: nunca colorear barras nominales por su valor ni por su posición); la identidad la
 * da el nombre completo, no un color. Hasta 8 categorías por divisa y el resto en "Otros".
 *
 * Cada fila se lee en TalkBack como "Streaming: 23,97 €, 45 %, 3 suscripciones".
 */
@Composable
fun GastosPorCategoriaCard(
    gastos: List<GastoCategoria>,
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
            Text(
                text = stringResource(R.string.category_monthly_equiv),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (gastos.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.no_category_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Spacer(Modifier.height(14.dp))
            val sinCategoria = stringResource(R.string.no_category)
            val otros = stringResource(R.string.others)
            val porDivisa = remember(gastos) { gastos.groupBy { it.moneda } }
            val variasDivisas = porDivisa.size > 1

            porDivisa.entries.forEachIndexed { indiceDivisa, (moneda, lista) ->
                if (indiceDivisa > 0) Spacer(Modifier.height(16.dp))
                if (variasDivisas) {
                    Text(
                        text = moneda,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                }
                val filas = plegarEnOtros(lista, otros)
                val maximo = filas.maxOf { it.gastoMensual }.takeIf { it > 0 } ?: 1.0
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filas.forEach { fila ->
                        FilaCategoria(
                            nombre = fila.nombre.ifBlank { sinCategoria },
                            gasto = fila,
                            fraccion = (fila.gastoMensual / maximo).toFloat()
                        )
                    }
                }
            }
        }
    }
}

/** Mantiene las [MAX_FILAS_POR_DIVISA] mayores y suma el resto en una fila "Otros". */
private fun plegarEnOtros(lista: List<GastoCategoria>, etiquetaOtros: String): List<GastoCategoria> {
    val ordenada = lista.sortedByDescending { it.gastoMensual }
    if (ordenada.size <= MAX_FILAS_POR_DIVISA) return ordenada
    val visibles = ordenada.take(MAX_FILAS_POR_DIVISA - 1)
    val resto = ordenada.drop(MAX_FILAS_POR_DIVISA - 1)
    return visibles + GastoCategoria(
        categoriaId = null,
        nombre = etiquetaOtros,
        moneda = resto.first().moneda,
        gastoMensual = resto.sumOf { it.gastoMensual },
        numSuscripciones = resto.sumOf { it.numSuscripciones },
        porcentaje = resto.sumOf { it.porcentaje }
    )
}

@Composable
private fun FilaCategoria(nombre: String, gasto: GastoCategoria, fraccion: Float) {
    val importe = formatearImporte(gasto.gastoMensual, gasto.moneda)
    val subs = pluralStringResource(R.plurals.category_subs_count, gasto.numSuscripciones, gasto.numSuscripciones)
    val descripcion = "$nombre: $importe, ${gasto.porcentaje} %, $subs"
    val ancho by animateFloatAsState(
        targetValue = fraccion.coerceIn(0.02f, 1f),
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "anchoCategoria"
    )

    Column(Modifier.semantics(mergeDescendants = true) { contentDescription = descripcion }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = importe,
                style = MaterialTheme.typography.bodyMedium.tabular,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${gasto.porcentaje} %",
                style = MaterialTheme.typography.labelMedium.tabular,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ChartColors.pista)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ancho)
                    .fillMaxHeight()
                    .background(ChartColors.acento, RoundedCornerShape(3.dp))
            )
        }
    }
}
