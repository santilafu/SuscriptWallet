package com.subia.android.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.R
import com.subia.android.ui.ServiceLogo
import com.subia.shared.model.MesCobros

private val AltoBarras = 112.dp
private val AnchoBarra = 18.dp
private val AltoEtiqueta = 22.dp

/**
 * "Qué te viene cada mes": 12 barras con el total previsto por mes (forma *énfasis* de la
 * skill dataviz: el mes seleccionado en el acento, el resto en gris neutro; sin eje Y, sin
 * rejilla, una sola etiqueta de importe sobre la barra seleccionada). Al tocar una barra se
 * listan debajo los cargos de ese mes para entender por qué sube (p. ej. el seguro anual).
 *
 * Accesible: la fila de barras lleva un resumen textual y cada barra es un botón con su
 * propio texto; la lista táctil es la alternativa completa al gráfico.
 */
@Composable
fun CobrosPorMesCard(
    meses: List<MesCobros>,
    moneda: String,
    onOpenSuscripcion: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var seleccionado by rememberSaveable { mutableIntStateOf(0) }
    if (meses.isEmpty()) return
    val indice = seleccionado.coerceIn(0, meses.lastIndex)

    val totales = remember(meses, moneda) { meses.map { it.total(moneda) } }
    val maximo = remember(totales) { totales.maxOrNull()?.takeIf { it > 0 } ?: 1.0 }
    val resumenGrafico = remember(meses, totales) {
        meses.indices.joinToString(", ") { i ->
            "${nombreMesCorto(meses[i].mes)} ${formatearImporteCompacto(totales[i], moneda)}"
        }
    }
    val descripcionGrafico = stringResource(R.string.months_chart_desc, resumenGrafico)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.months_chart_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))

            // Cabecera del mes seleccionado: nombre + total + nº de cobros
            Crossfade(targetState = indice, animationSpec = tween(160), label = "mesCabecera") { i ->
                val mes = meses[i]
                val hoyAnio = meses[0].anio
                val nombre = nombreMesLargo(mes.mes) + if (mes.anio != hoyAnio) " ${mes.anio}" else ""
                val totalesMes = mes.totalPorMoneda.entries.toList()
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = nombre,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (totalesMes.isEmpty()) formatearImporte(0.0, moneda)
                            else totalesMes.joinToString(" · ") { formatearImporte(it.value, it.key) },
                            style = MaterialTheme.typography.headlineSmall.tabular,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = pluralStringResource(R.plurals.month_charges_count, mes.cobros.size, mes.cobros.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Gráfico de 12 barras
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = descripcionGrafico },
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                meses.forEachIndexed { i, mes ->
                    BarraMes(
                        etiqueta = nombreMesCorto(mes.mes),
                        valor = totales[i],
                        fraccion = (totales[i] / maximo).toFloat(),
                        moneda = moneda,
                        seleccionada = i == indice,
                        esMesActual = i == 0,
                        onClick = { seleccionado = i },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Lista de cargos del mes seleccionado
            Crossfade(targetState = indice, animationSpec = tween(160), label = "mesLista") { i ->
                val cobros = meses[i].cobros
                Column {
                    if (cobros.isEmpty()) {
                        Text(
                            text = stringResource(R.string.month_no_charges),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        cobros.forEachIndexed { j, cobro ->
                            if (j > 0) HorizontalDivider(color = ChartColors.pista, thickness = 1.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenSuscripcion(cobro.suscripcionId) }
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ServiceLogo(nombre = cobro.nombre, size = 34.dp, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = cobro.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatearFechaCorta(cobro.fecha),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatearImporte(cobro.importe, cobro.moneda),
                                    style = MaterialTheme.typography.bodyMedium.tabular,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarraMes(
    etiqueta: String,
    valor: Double,
    fraccion: Float,
    moneda: String,
    seleccionada: Boolean,
    esMesActual: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    val colorBarra by animateColorAsState(
        targetValue = if (seleccionada) ChartColors.acento else ChartColors.neutro,
        animationSpec = tween(160),
        label = "colorBarra"
    )
    val altura by animateFloatAsState(
        targetValue = fraccion.coerceIn(0f, 1f),
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "alturaBarra"
    )
    val descripcion = "$etiqueta: ${formatearImporte(valor, moneda)}"

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion; selected = seleccionada; role = Role.Button }
            .padding(bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Etiqueta de importe solo en la barra seleccionada, posada justo encima de SU barra:
        // arriba del todo se leía como una etiqueta de eje ("15 €" sobre barras de 120 €).
        Box(
            modifier = Modifier.height(AltoBarras + AltoEtiqueta).fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (seleccionada) {
                    Text(
                        text = formatearImporteCompacto(valor, moneda),
                        style = MaterialTheme.typography.labelSmall.tabular,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colores.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.wrapContentWidth(unbounded = true)
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (valor > 0) {
                    Box(
                        Modifier
                            .width(AnchoBarra)
                            .height(AltoBarras * altura.coerceAtLeast(0.03f))
                            .background(colorBarra, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    )
                } else {
                    // Cero explícito: línea de 2 dp en la pista, para que "nada" se lea como 0 y no como ausencia.
                    Box(Modifier.width(AnchoBarra).height(2.dp).background(ChartColors.pista))
                }
            }
        }
        HorizontalDivider(color = ChartColors.pista, thickness = 1.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = if (seleccionada || esMesActual) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                seleccionada -> colores.onSurface
                esMesActual -> colores.primary
                else -> colores.onSurfaceVariant
            },
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentWidth(unbounded = true)
        )
    }
}
