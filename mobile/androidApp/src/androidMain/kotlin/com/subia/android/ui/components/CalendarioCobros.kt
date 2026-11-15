package com.subia.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.subia.android.R
import com.subia.android.ui.ServiceLogo
import com.subia.shared.model.CuadriculaMes
import com.subia.shared.model.DiaCobros
import com.subia.shared.model.diaPorDefectoCalendario
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import java.time.DayOfWeek
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * Ancho máximo del calendario: en horizontal y en tablet las celdas no se estiran (con 7
 * columnas, unos 60 dp por celda como mucho).
 */
private val AnchoMaximoCalendario = 420.dp

private val TamCirculoDia = 30.dp
private val AltoIndicador = 18.dp

/** Tope del número del día y de su círculo, el mismo que en la tira. */
private const val ESCALA_MAXIMA_NUMERO_DIA = 1.3f

/** Opacidad de los días ya pasados: se leen como "ya cobrado" (igual que en la tira). */
private const val ALFA_DIA_PASADO = 0.45f

/**
 * Calendario de "Este mes": el mes entero a la vista, en semanas de lunes a domingo (4 a 6
 * filas, ver [CuadriculaMes]), sin desplazamiento horizontal. Cada día con cobros lleva el mini
 * logo del servicio (o el primero y "+N" si hay varios) y es un botón: al tocarlo, su lista de
 * cobros aparece debajo. Sin día elegido se despliega el próximo día con cobros desde hoy.
 * Hoy va con el círculo `primary` de la tira y los días pasados atenuados.
 *
 * Accesibilidad: cada día con cobros se lee "Martes 17, 2 cobros, 22,98 €"; los días sin
 * cobros y la cabecera de la semana no se enfocan.
 *
 * @param dias días del mes del 1 al último ([com.subia.shared.model.ProyeccionCobros.diasMes]).
 */
@Composable
fun CalendarioCobros(
    dias: List<DiaCobros>,
    hoy: LocalDate,
    onOpenSuscripcion: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val primerDia = dias.first().fecha
    val cuadricula = remember(primerDia) { CuadriculaMes.de(primerDia) }
    val porDia = remember(dias) { dias.associateBy { it.fecha.dayOfMonth } }
    // Día tocado (número del día). Se olvida al cambiar de mes.
    var elegido by rememberSaveable(primerDia.toString()) { mutableStateOf<Int?>(null) }
    val mostrado: DiaCobros? = elegido?.let { porDia[it] }?.takeIf { it.tieneCobros }
        ?: diaPorDefectoCalendario(dias, hoy)?.let { porDia[it.dayOfMonth] }

    Column(modifier.widthIn(max = AnchoMaximoCalendario).fillMaxWidth()) {
        CabeceraSemana()
        Spacer(Modifier.height(4.dp))
        cuadricula.semanas().forEach { semana ->
            Row(Modifier.fillMaxWidth()) {
                semana.forEach { numero ->
                    val dia = numero?.let { porDia[it] }
                    if (dia == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        CeldaDia(
                            dia = dia,
                            esHoy = dia.fecha == hoy,
                            esPasado = dia.fecha < hoy,
                            seleccionado = dia.fecha == mostrado?.fecha,
                            onElegir = { elegido = dia.fecha.dayOfMonth },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (dias.any { it.tieneCobros }) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            // Cambio de día: fundido corto con 6 dp de subida; la altura se anima, no salta.
            val subida = with(LocalDensity.current) { 6.dp.roundToPx() }
            AnimatedContent(
                targetState = mostrado,
                transitionSpec = {
                    (fadeIn(tween(160, easing = EaseOutFuerte)) +
                        slideInVertically(tween(200, easing = EaseOutFuerte)) { subida })
                        .togetherWith(fadeOut(tween(90)))
                        .using(SizeTransform(clip = true) { _, _ -> tween(200, easing = EaseOutFuerte) })
                },
                contentKey = { it?.fecha },
                label = "diaCalendario"
            ) { dia ->
                if (dia != null) {
                    DetalleDia(dia, hoy, onOpenSuscripcion)
                } else {
                    Text(
                        text = stringResource(R.string.calendar_no_more_charges),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp)
                    )
                }
            }
        }
    }
}

/** "Lun Mar Mié …" según el locale. Oculta a TalkBack: cada día ya dice su nombre completo. */
@Composable
private fun CabeceraSemana() {
    val locale = Locale.getDefault()
    val nombres = remember(locale) {
        // De lunes a domingo (el orden de java.time.DayOfWeek), como la rejilla.
        DayOfWeek.entries.map { dia ->
            dia.getDisplayName(JavaTextStyle.SHORT, locale)
                .trimEnd('.')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }
    }
    Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        nombres.forEach { nombre ->
            Text(
                text = nombre,
                style = MaterialTheme.typography.labelSmall,
                fontSize = tamEtiquetaGrafico(11f),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CeldaDia(
    dia: DiaCobros,
    esHoy: Boolean,
    esPasado: Boolean,
    seleccionado: Boolean,
    onElegir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    val fondo by animateColorAsState(
        targetValue = if (seleccionado) colores.primaryContainer else colores.primaryContainer.copy(alpha = 0f),
        animationSpec = tween(150, easing = EaseOutFuerte),
        label = "fondoDiaCalendario"
    )
    val numCobros = dia.cobros.size
    val hoyTexto = stringResource(R.string.day_today)
    val yaCobrado = stringResource(R.string.tira_already_charged)
    val cobrosTexto = if (numCobros > 0) pluralStringResource(R.plurals.month_charges_count, numCobros, numCobros) else ""
    val verCobros = stringResource(R.string.calendar_view_charges)
    val descripcion = remember(dia, esHoy, esPasado, hoyTexto, yaCobrado, cobrosTexto) {
        buildString {
            if (esHoy) append(hoyTexto).append(", ")
            append(nombreDiaSemanaLargo(dia.fecha)).append(' ').append(dia.fecha.dayOfMonth)
            append(", ").append(cobrosTexto).append(", ")
            append(dia.totalPorMoneda.entries.joinToString(", ") { formatearImporte(it.value, it.key) })
            if (esPasado) append(", ").append(yaCobrado)
        }
    }

    // Toda la celda es el objetivo táctil (unos 46 dp de ancho en un móvil, 48 dp de alto como mínimo).
    val interaccion = if (dia.tieneCobros) {
        Modifier
            .clickable(role = Role.Button, onClickLabel = verCobros, onClick = onElegir)
            // Limpia el "17" del Text de dentro: la celda se lee solo con su descripción.
            .clearAndSetSemantics {
                contentDescription = descripcion
                // Solo el elegido: con `selected = false` TalkBack diría "no seleccionado" en cada día.
                if (seleccionado) selected = true
            }
    } else {
        Modifier.clearAndSetSemantics {}
    }
    val tamCirculo = TamCirculoDia * escalaFuenteConTope(ESCALA_MAXIMA_NUMERO_DIA)

    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind { if (fondo.alpha > 0f) drawRect(fondo) }
            .then(interaccion)
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp)
            .then(if (esPasado) Modifier.alpha(ALFA_DIA_PASADO) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(tamCirculo)
                .then(if (esHoy) Modifier.background(colores.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dia.fecha.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.tabular,
                fontSize = tamConTope(14f, ESCALA_MAXIMA_NUMERO_DIA),
                maxLines = 1,
                fontWeight = if (esHoy || seleccionado || dia.tieneCobros) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    esHoy -> colores.onPrimary
                    seleccionado -> colores.onPrimaryContainer
                    else -> colores.onSurface
                }
            )
        }
        IndicadorCobros(dia, color = if (seleccionado) colores.onPrimaryContainer else colores.onSurfaceVariant)
    }
}

/** Mini logo del servicio; con varios cobros, el primero y "+N" (los demás). Sin cobros, hueco. */
@Composable
private fun IndicadorCobros(dia: DiaCobros, color: Color) {
    Row(
        modifier = Modifier.height(AltoIndicador),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (dia.cobros.size) {
            0 -> Unit
            1 -> MiniLogo(dia.cobros[0].nombre, AltoIndicador)
            else -> {
                MiniLogo(dia.cobros[0].nombre, 16.dp)
                Text(
                    text = "+${dia.cobros.size - 1}",
                    style = MaterialTheme.typography.labelSmall.tabular,
                    fontSize = tamEtiquetaGrafico(10f),
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Logo de 16-18 dp. La inicial del logo genérico se mide en sp: con letra 1,5× no cabía en el
 * círculo y salía cortada. Es un icono, no texto que leer, así que aquí no sigue a la fuente.
 */
@Composable
private fun MiniLogo(nombre: String, tamano: Dp) {
    val densidad = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(densidad.density, fontScale = 1f)) {
        ServiceLogo(nombre = nombre, size = tamano, contentDescription = null)
    }
}

/**
 * Cobros del día elegido: logo, nombre e importe con su periodo ("12,99 € al mes", "90,00 € cada
 * 3 meses"). El nombre y el importe van uno debajo del otro: con letra grande no se corta nada.
 * Tocar un cobro abre la suscripción.
 */
@Composable
private fun DetalleDia(dia: DiaCobros, hoy: LocalDate, onOpenSuscripcion: (Long) -> Unit) {
    val colores = MaterialTheme.colorScheme
    val esPasado = dia.fecha < hoy
    val cabecera = buildString {
        val distancia = hoy.daysUntil(dia.fecha)
        if (distancia == 0) append(stringResource(R.string.day_today)).append(" · ")
        if (distancia == 1) append(stringResource(R.string.day_tomorrow)).append(" · ")
        append(nombreDiaSemanaLargo(dia.fecha)).append(' ').append(dia.fecha.dayOfMonth)
        if (esPasado) append(" · ").append(stringResource(R.string.tira_already_charged))
    }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = cabecera,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (esPasado) colores.onSurfaceVariant else colores.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(4.dp))
        dia.cobros.forEach { cobro ->
            val abrir = stringResource(R.string.tira_open_sub, cobro.nombre)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClickLabel = abrir) { onOpenSuscripcion(cobro.suscripcionId) }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceLogo(
                    nombre = cobro.nombre,
                    size = 32.dp,
                    contentDescription = null,
                    modifier = if (esPasado) Modifier.alpha(ALFA_DIA_PASADO) else Modifier
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = cobro.nombre,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colores.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = importeCadaPeriodo(cobro.importe, cobro.moneda, cobro.periodo),
                        style = MaterialTheme.typography.bodySmall.tabular,
                        color = colores.onSurfaceVariant
                    )
                }
            }
        }
    }
}
