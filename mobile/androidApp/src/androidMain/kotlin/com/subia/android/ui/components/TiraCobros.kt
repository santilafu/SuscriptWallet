package com.subia.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.R
import com.subia.android.ui.ServiceLogo
import com.subia.shared.model.CobroPrevisto
import com.subia.shared.model.DiaCobros
import kotlinx.datetime.LocalDate

private val AnchoDia = 48.dp
private val AltoZonaLogos = 64.dp

/** Opacidad de los días ya pasados en la vista mensual: se leen como "ya cobrado". */
private const val ALFA_DIA_PASADO = 0.45f

/**
 * "Tira de cobros": calendario horizontal de días. En la vista de 14 días va de hoy a +13; en
 * la de "Este mes", del 1 al último día del mes, con los días anteriores a [hoy] atenuados (ya
 * cobrados). Cada día es una columna estrecha con el día de la semana, el número y los logos de
 * los servicios que cobran ese día posados encima, con el importe del día debajo. Hoy va en
 * `primary`; el día del próximo cobro lleva un anillo `primary`. Tocar un logo abre la suscripción.
 *
 * Semántica: cada columna se lee como "Jueves 9: Netflix 12,99 €, Spotify 9,99 €".
 *
 * @param estadoLista posición de la tira; la vista mensual la crea ya desplazada hasta hoy.
 */
@Composable
fun TiraCobros(
    dias: List<DiaCobros>,
    hoy: LocalDate,
    proximoCobro: CobroPrevisto?,
    onOpenSuscripcion: (Long) -> Unit,
    modifier: Modifier = Modifier,
    estadoLista: LazyListState = rememberLazyListState()
) {
    val fechaProximo: LocalDate? = proximoCobro?.fecha
    // Sin ningún cobro en los días mostrados la tira se queda en calendario (día + número): 80 dp
    // de puntos vacíos se leían como contenido que no ha cargado.
    val compacta = dias.none { it.tieneCobros }
    LazyRow(
        modifier = modifier,
        state = estadoLista,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(horizontal = 10.dp)
    ) {
        items(dias, key = { dia -> dia.fecha.toString() }) { dia ->
            DiaColumna(
                dia = dia,
                esHoy = dia.fecha == hoy,
                esPasado = dia.fecha < hoy,
                esProximo = dia.tieneCobros && dia.fecha == fechaProximo,
                compacta = compacta,
                onOpenSuscripcion = onOpenSuscripcion
            )
        }
    }
}

@Composable
private fun DiaColumna(
    dia: DiaCobros,
    esHoy: Boolean,
    esPasado: Boolean,
    esProximo: Boolean,
    compacta: Boolean,
    onOpenSuscripcion: (Long) -> Unit
) {
    val colores = MaterialTheme.colorScheme
    val sinCobros = stringResource(R.string.tira_no_charge)
    val hoyTexto = stringResource(R.string.day_today)
    val yaCobrado = stringResource(R.string.tira_already_charged)
    val descripcion = remember(dia, esHoy, esPasado, sinCobros, hoyTexto, yaCobrado) {
        val cabecera = buildString {
            if (esHoy) append(hoyTexto).append(", ")
            append(nombreDiaSemanaLargo(dia.fecha)).append(' ').append(dia.fecha.dayOfMonth)
        }
        val detalle = if (dia.tieneCobros) {
            dia.cobros.joinToString(", ") { "${it.nombre} ${formatearImporte(it.importe, it.moneda)}" } +
                if (esPasado) ", $yaCobrado" else ""
        } else sinCobros
        "$cabecera: $detalle"
    }

    Column(
        modifier = Modifier
            .width(AnchoDia)
            .then(if (esPasado) Modifier.alpha(ALFA_DIA_PASADO) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = descripcion },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Día de la semana
        Text(
            text = nombreDiaSemanaCorto(dia.fecha),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = if (esHoy) FontWeight.SemiBold else FontWeight.Medium,
            color = if (esHoy) colores.primary else colores.onSurfaceVariant,
            maxLines = 1
        )

        // Número del día: relleno primary si es hoy, anillo primary si es el próximo cobro.
        val fondoNumero = when {
            esHoy -> Modifier.background(colores.primary, CircleShape)
            esProximo -> Modifier.border(1.5.dp, colores.primary, CircleShape)
            else -> Modifier
        }
        Box(
            modifier = Modifier.size(32.dp).then(fondoNumero),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dia.fecha.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.tabular,
                fontSize = 15.sp,
                fontWeight = if (esHoy || esProximo) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    esHoy -> colores.onPrimary
                    esProximo -> colores.primary
                    else -> colores.onSurface
                }
            )
        }

        if (compacta) return@Column

        // Logos posados sobre el día
        Column(
            modifier = Modifier.height(AltoZonaLogos),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (dia.cobros.size) {
                0 -> Box(Modifier.size(4.dp).background(colores.surfaceVariant, CircleShape))
                1 -> LogoPulsable(dia.cobros[0], 34.dp, onOpenSuscripcion)
                2 -> {
                    LogoPulsable(dia.cobros[0], 28.dp, onOpenSuscripcion, ZonaTactilApilada)
                    Box(Modifier.height(2.dp))
                    LogoPulsable(dia.cobros[1], 28.dp, onOpenSuscripcion, ZonaTactilApilada)
                }
                else -> {
                    LogoPulsable(dia.cobros[0], 28.dp, onOpenSuscripcion, ZonaTactilApilada)
                    Box(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colores.surfaceVariant)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = dia.cobros[1].nombre
                            ) { onOpenSuscripcion(dia.cobros[1].suscripcionId) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${dia.cobros.size - 1}",
                            style = MaterialTheme.typography.labelSmall.tabular,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colores.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Importe del día (primera divisa; si hay varias, "+" indica que hay más)
        val totales = dia.totalPorMoneda.entries.toList()
        val importeTexto = when {
            totales.isEmpty() -> ""
            else -> {
                val (moneda, total) = totales.first()
                (if (total >= 100) formatearImporteCompacto(total, moneda) else formatearImporte(total, moneda)) +
                    if (totales.size > 1) " +" else ""
            }
        }
        Text(
            text = importeTexto,
            style = MaterialTheme.typography.labelSmall.tabular,
            fontSize = tamEtiquetaGrafico(11f),
            fontWeight = if (esProximo) FontWeight.SemiBold else FontWeight.Medium,
            color = if (esProximo) colores.primary else colores.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
            // Alto mínimo, no fijo: con fuente grande la línea mide más de 16 dp y se cortaba por abajo.
            modifier = Modifier.heightIn(min = 16.dp)
        )
    }
}

/**
 * Con dos logos apilados, dos cajas de 44 dp no caben en los 64 dp de la zona y el segundo logo
 * salía recortado (visto en la vista mensual, con dos cobros el mismo día): cada caja mide 31 dp
 * y Compose sigue ampliando la zona táctil efectiva hasta 48 dp.
 */
private val ZonaTactilApilada = 31.dp

/** Logo con zona táctil de 44 dp (se superpone 2 dp a cada lado de la columna) y feedback de presión. */
@Composable
private fun LogoPulsable(
    cobro: CobroPrevisto,
    tamano: androidx.compose.ui.unit.Dp,
    onOpenSuscripcion: (Long) -> Unit,
    zonaTactil: androidx.compose.ui.unit.Dp = 44.dp
) {
    val interaccion = remember { MutableInteractionSource() }
    val pulsado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (pulsado) 0.94f else 1f, tween(120), label = "logoPress")
    val abrir = stringResource(R.string.tira_open_sub, cobro.nombre)
    Box(
        modifier = Modifier
            .size(zonaTactil)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaccion, indication = null, role = Role.Button) {
                onOpenSuscripcion(cobro.suscripcionId)
            }
            .semantics { contentDescription = abrir },
        contentAlignment = Alignment.Center
    ) {
        ServiceLogo(
            nombre = cobro.nombre,
            size = tamano,
            contentDescription = null,
            modifier = Modifier.graphicsLayer { scaleX = escala; scaleY = escala }
        )
    }
}
