package com.subia.android.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.R
import com.subia.android.ui.ServiceLogo
import com.subia.android.util.InicioPrefs
import com.subia.shared.model.GastoCategoria
import com.subia.shared.model.GastoDeCategoria
import com.subia.shared.model.VistaCategorias

private const val MAX_FILAS_POR_DIVISA = 8

/** Crecimiento de las barras la primera vez que se ve la tarjeta. */
private const val CRECER_BARRAS_MS = 700

/** Opacidad de las categorías no elegidas mientras otra está desplegada. */
private const val ALFA_ATENUADA = 0.45f

/** Resaltar/atenuar y desplegar: duraciones de interacción (150-300 ms). */
private const val RESALTE_MS = 200
private const val DESPLEGAR_MS = 240
private const val PLEGAR_MS = 180

/** Cambio de vista: segmento elegido y fundido con desplazamiento (como la tira de cobros). */
private const val SEGMENTO_MS = 180
private const val CAMBIO_VISTA_MS = 240

private const val CLAVE_BARRAS = "categorias_barras"

/**
 * Gasto por categoría como barras horizontales ordenadas de mayor a menor, con dos vistas que se
 * eligen con el selector de la cabecera ("Este mes" por defecto; la elección se recuerda con
 * rememberSaveable + [InicioPrefs]):
 * - "Este mes" ([gastosMes]): lo que se cobra de verdad en el mes natural (del 1 al último, también
 *   lo ya cobrado), igual que el calendario y "Este mes pagas". Las categorías sin cobros este mes
 *   no aparecen; si no hay ningún cobro, un estado vacío corto.
 * - "Al año" ([gastosAnio]): total anual por categoría (importe × 12 / intervalo; semanal × 52).
 * Porcentajes y barras son relativos al total de la vista elegida en cada divisa.
 *
 * Es una sola serie nominal, así que todas las barras llevan el mismo acento (regla dataviz:
 * nunca colorear barras nominales por su valor ni por su posición). Hasta 8 categorías por divisa
 * y el resto en "Otros".
 *
 * Interactiva: tocar una categoría la resalta (las demás se atenúan) y despliega debajo sus
 * gastos con el importe de la vista; volver a tocarla la cierra. La primera vez que se ve la
 * tarjeta las barras crecen desde la izquierda (700 ms, ease-out fuerte), una sola vez.
 *
 * Cada fila se lee en TalkBack como "Streaming: 23,97 €, 45 %, 3 suscripciones", con su estado
 * (desplegada / plegada) y las acciones de desplegar o plegar. El selector se anuncia como un
 * grupo de opciones (radio) con la elegida marcada.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GastosPorCategoriaCard(
    gastosMes: List<GastoCategoria>,
    gastosAnio: List<GastoCategoria>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var enAnio by rememberSaveable { mutableStateOf(InicioPrefs.categoriasEnAnio(context)) }
    var seleccion by rememberSaveable { mutableStateOf<String?>(null) }
    val vista = if (enAnio) VistaCategorias.AL_ANIO else VistaCategorias.ESTE_MES
    // Sin suscripciones activas no hay nada que comparar: ni selector ni barras.
    val sinDatos = gastosAnio.isEmpty() && gastosMes.isEmpty()
    val desplazamiento = with(LocalDensity.current) { 16.dp.roundToPx() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // 8 dp a los lados (y 8 más en los textos): el resalte de la fila desplegada llega casi al
        // borde de la tarjeta mientras el texto queda alineado con el resto de tarjetas (16 dp).
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
            // Título y selector en una fila; si no caben (letra grande, francés) el selector baja.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.spending_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 12.dp)
                )
                if (!sinDatos) {
                    SelectorVistaCategorias(
                        vista = vista,
                        onCambiar = { nueva ->
                            enAnio = nueva == VistaCategorias.AL_ANIO
                            seleccion = null
                            InicioPrefs.guardarCategoriasEnAnio(context, enAnio)
                        },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            if (sinDatos) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.no_category_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                return@Column
            }

            // Crecimiento de las barras: solo la primera vez que aparece la tarjeta CON datos en
            // este proceso y con las animaciones del sistema activas. Se lee al dibujar.
            val activas = animacionesActivas()
            val animarBarras = remember { activas && MovimientoUnaVez.reclamar(CLAVE_BARRAS) }
            val crecimiento = remember { Animatable(if (animarBarras) 0f else 1f) }
            if (animarBarras) {
                LaunchedEffect(Unit) { crecimiento.animateTo(1f, tween(CRECER_BARRAS_MS, easing = EaseOutFuerte)) }
            }

            AnimatedContent(
                targetState = vista,
                transitionSpec = {
                    // Hacia "Al año" (segmento derecho) lo nuevo llega desde la derecha, y al revés.
                    // Con las animaciones del sistema desactivadas, Compose ya lo hace instantáneo.
                    val lado = if (targetState == VistaCategorias.AL_ANIO) 1 else -1
                    (fadeIn(tween(200, delayMillis = 50, easing = EaseOutFuerte)) +
                        slideInHorizontally(tween(CAMBIO_VISTA_MS, easing = EaseOutFuerte)) { lado * desplazamiento })
                        .togetherWith(fadeOut(tween(110)))
                        .using(SizeTransform(clip = false) { _, _ -> tween(CAMBIO_VISTA_MS, easing = EaseOutFuerte) })
                },
                label = "vistaCategorias"
            ) { vistaMostrada ->
                val gastos = if (vistaMostrada == VistaCategorias.AL_ANIO) gastosAnio else gastosMes
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(
                            if (vistaMostrada == VistaCategorias.AL_ANIO) R.string.category_subtitle_year
                            else R.string.category_subtitle_month
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    if (gastos.isEmpty()) {
                        // Hay suscripciones, pero ninguna cobra este mes.
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.category_month_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    } else {
                        Spacer(Modifier.height(10.dp))
                        ListaCategorias(
                            gastos = gastos,
                            vista = vistaMostrada,
                            crecimiento = { crecimiento.value },
                            seleccion = seleccion,
                            onToggle = { clave -> seleccion = if (seleccion == clave) null else clave }
                        )
                    }
                }
            }
        }
    }
}

/** Las barras de una vista, agrupadas por divisa (con la divisa como rótulo si hay varias). */
@Composable
private fun ListaCategorias(
    gastos: List<GastoCategoria>,
    vista: VistaCategorias,
    crecimiento: () -> Float,
    seleccion: String?,
    onToggle: (String) -> Unit
) {
    val sinCategoria = stringResource(R.string.no_category)
    val otros = stringResource(R.string.others)
    val porDivisa = remember(gastos) { gastos.groupBy { it.moneda } }
    val variasDivisas = porDivisa.size > 1

    Column {
        porDivisa.entries.forEachIndexed { indiceDivisa, (moneda, lista) ->
            if (indiceDivisa > 0) Spacer(Modifier.height(12.dp))
            if (variasDivisas) {
                Text(
                    text = moneda,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(2.dp))
            }
            val filas = plegarEnOtros(lista, otros)
            val maximo = filas.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
            filas.forEach { fila ->
                val clave = claveFila(fila)
                FilaCategoria(
                    nombre = fila.nombre.ifBlank { sinCategoria },
                    gasto = fila,
                    vista = vista,
                    fraccion = (fila.total / maximo).toFloat(),
                    crecimiento = crecimiento,
                    desplegada = seleccion == clave,
                    atenuada = seleccion != null && seleccion != clave,
                    onToggle = { onToggle(clave) }
                )
            }
        }
    }
}

/**
 * Selector segmentado "Este mes | Al año", con el mismo aspecto que el de la tira de cobros
 * (pastilla de 40 dp; Compose amplía la zona táctil de cada segmento a 48 dp), pero anunciado
 * como un grupo de opciones (radio) con la elegida marcada, como el selector de periodicidad.
 * El texto sigue a la fuente del sistema solo hasta 1,2× para que la cabecera no se desborde.
 */
@Composable
private fun SelectorVistaCategorias(
    vista: VistaCategorias,
    onCambiar: (VistaCategorias) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val elegir = { nueva: VistaCategorias ->
        if (nueva != vista) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onCambiar(nueva)
        }
    }
    val etiquetaGrupo = stringResource(R.string.category_view_selector)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
            .semantics { contentDescription = etiquetaGrupo }
            .selectableGroup()
    ) {
        SegmentoVista(stringResource(R.string.category_view_month), seleccionado = vista == VistaCategorias.ESTE_MES) {
            elegir(VistaCategorias.ESTE_MES)
        }
        SegmentoVista(stringResource(R.string.category_view_year), seleccionado = vista == VistaCategorias.AL_ANIO) {
            elegir(VistaCategorias.AL_ANIO)
        }
    }
}

@Composable
private fun SegmentoVista(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val fondo by animateColorAsState(
        targetValue = if (seleccionado) colores.primaryContainer else colores.primaryContainer.copy(alpha = 0f),
        animationSpec = tween(SEGMENTO_MS, easing = EaseOutFuerte),
        label = "fondoSegmentoCategorias"
    )
    val tinta by animateColorAsState(
        targetValue = if (seleccionado) colores.onPrimaryContainer else colores.onSurfaceVariant,
        animationSpec = tween(SEGMENTO_MS, easing = EaseOutFuerte),
        label = "tintaSegmentoCategorias"
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .drawBehind { drawRect(fondo) }
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 34.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            fontSize = tamConTope(12f, ESCALA_MAXIMA_ETIQUETAS),
            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Medium,
            color = tinta,
            maxLines = 1
        )
    }
}

/** Identifica una fila entre recomposiciones (y para rememberSaveable): divisa + categoría. */
private fun claveFila(fila: GastoCategoria): String = "${fila.moneda}|${fila.categoriaId}|${fila.nombre}"

/** Mantiene las [MAX_FILAS_POR_DIVISA] mayores y suma el resto en una fila "Otros". */
private fun plegarEnOtros(lista: List<GastoCategoria>, etiquetaOtros: String): List<GastoCategoria> {
    val ordenada = lista.sortedByDescending { it.total }
    if (ordenada.size <= MAX_FILAS_POR_DIVISA) return ordenada
    val visibles = ordenada.take(MAX_FILAS_POR_DIVISA - 1)
    val resto = ordenada.drop(MAX_FILAS_POR_DIVISA - 1)
    return visibles + GastoCategoria(
        categoriaId = null,
        nombre = etiquetaOtros,
        moneda = resto.first().moneda,
        total = resto.sumOf { it.total },
        porcentaje = resto.sumOf { it.porcentaje },
        gastos = resto.flatMap { it.gastos }.sortedByDescending { it.importe }
    )
}

/**
 * Una categoría: cabecera tocable (nombre, importe, %, flecha y barra) de al menos 48 dp de alto
 * y, si está [desplegada], sus gastos debajo. Todo lo que se anima (escala al pulsar, opacidad,
 * fondo resaltado, giro de la flecha, ancho de la barra) se lee en la capa gráfica o al dibujar.
 */
@Composable
private fun FilaCategoria(
    nombre: String,
    gasto: GastoCategoria,
    vista: VistaCategorias,
    fraccion: Float,
    crecimiento: () -> Float,
    desplegada: Boolean,
    atenuada: Boolean,
    onToggle: () -> Unit
) {
    val colores = MaterialTheme.colorScheme
    val importe = formatearImporte(gasto.total, gasto.moneda)
    val subs = pluralStringResource(R.plurals.category_subs_count, gasto.numSuscripciones, gasto.numSuscripciones)
    val descripcion = "$nombre: $importe, ${gasto.porcentaje} %, $subs"
    val estado = stringResource(if (desplegada) R.string.a11y_expanded else R.string.a11y_collapsed)
    val accion = stringResource(if (desplegada) R.string.category_hide_expenses else R.string.category_show_expenses)
    val desplegable = gasto.gastos.isNotEmpty()
    val view = LocalView.current
    val alternar = {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        onToggle()
    }

    val interaccion = remember { MutableInteractionSource() }
    val escala = rememberEscalaPulsacion(interaccion)
    val alfa = animateFloatAsState(
        targetValue = if (atenuada) ALFA_ATENUADA else 1f,
        animationSpec = tween(RESALTE_MS, easing = EaseOutFuerte),
        label = "alfaCategoria"
    )
    val fondo = animateColorAsState(
        targetValue = if (desplegada) colores.primary.copy(alpha = 0.08f) else colores.primary.copy(alpha = 0f),
        animationSpec = tween(RESALTE_MS, easing = EaseOutFuerte),
        label = "fondoCategoria"
    )
    val giro = animateFloatAsState(
        targetValue = if (desplegada) 180f else 0f,
        animationSpec = tween(DESPLEGAR_MS, easing = EaseOutFuerte),
        label = "giroFlecha"
    )
    val ancho = animateFloatAsState(
        targetValue = fraccion.coerceIn(0.02f, 1f),
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "anchoCategoria"
    )
    val acento = ChartColors.acento
    val pista = ChartColors.pista

    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = alfa.value
                scaleX = escala.value
                scaleY = escala.value
            }
            .clip(RoundedCornerShape(12.dp))
            .drawBehind { drawRect(fondo.value) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaccion,
                    indication = LocalIndication.current,
                    enabled = desplegable,
                    role = Role.Button,
                    onClickLabel = accion,
                    onClick = alternar
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = descripcion
                    if (desplegable) {
                        stateDescription = estado
                        if (desplegada) collapse { alternar(); true } else expand { alternar(); true }
                    }
                }
                .heightIn(min = 48.dp)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (desplegada) FontWeight.SemiBold else FontWeight.Normal,
                    color = colores.onSurface,
                    // Dos líneas: con letra 1,5× "Hogar y suministros" no cabía en una junto al importe.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = importe,
                    style = MaterialTheme.typography.bodyMedium.tabular,
                    fontWeight = FontWeight.SemiBold,
                    color = colores.onSurface
                )
                Spacer(Modifier.width(6.dp))
                // Ancho mínimo (no fijo) para que la columna quede alineada: con la fuente del sistema
                // ampliada, "37 %" no cabía en 36 dp y se partía en dos líneas ("37" / "%").
                Text(
                    text = "${gasto.porcentaje} %",
                    style = MaterialTheme.typography.labelMedium.tabular,
                    fontSize = 12.sp,
                    color = colores.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.widthIn(min = 36.dp)
                )
                if (desplegable) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = colores.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(18.dp)
                            .graphicsLayer { rotationZ = giro.value }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            // Pista y barra pintadas en una sola capa: el ancho (dato × crecimiento) se lee al
            // dibujar, así que ni la entrada ni un cambio de datos vuelven a medir la fila.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .drawBehind {
                        val radio = CornerRadius(size.height / 2f)
                        drawRoundRect(pista, cornerRadius = radio)
                        val w = size.width * ancho.value * crecimiento()
                        if (w > 0f) drawRoundRect(acento, size = Size(w, size.height), cornerRadius = radio)
                    }
            )
        }

        AnimatedVisibility(
            visible = desplegada && desplegable,
            enter = expandVertically(tween(DESPLEGAR_MS, easing = EaseOutFuerte), expandFrom = Alignment.Top) +
                fadeIn(tween(DESPLEGAR_MS - 60, delayMillis = 60, easing = EaseOutFuerte)),
            exit = shrinkVertically(tween(PLEGAR_MS, easing = EaseOutFuerte), shrinkTowards = Alignment.Top) +
                fadeOut(tween(PLEGAR_MS - 60))
        ) {
            GastosDeLaCategoria(gasto.gastos, vista)
        }
    }
}

/**
 * Los gastos de una categoría desplegada: logo, nombre y, a la derecha, lo que suman en la vista
 * (lo cobrado este mes o el total al año). Debajo del nombre, lo que se paga en cada cobro
 * ("90,00 € cada 3 meses") cuando aporta algo: en "Este mes" se omite en los mensuales con un solo
 * cobro y, si hay varios (un semanal), se antepone cuántos ("5 cobros · 5,00 € a la semana"); en
 * "Al año" se omite cuando hay un único cobro al año (el anual), porque sería la misma cifra.
 */
@Composable
private fun GastosDeLaCategoria(gastos: List<GastoDeCategoria>, vista: VistaCategorias) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        gastos.forEach { g ->
            val cadaCobro = importeCadaPeriodo(g.precio, g.moneda, g.periodo)
            val detalle = when (vista) {
                VistaCategorias.ESTE_MES -> when {
                    g.numCobros > 1 ->
                        pluralStringResource(R.plurals.month_charges_count, g.numCobros, g.numCobros) + " · " + cadaCobro
                    g.periodo.esMensual -> null
                    else -> cadaCobro
                }
                VistaCategorias.AL_ANIO -> if (g.numCobros <= 1) null else cadaCobro
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceLogo(nombre = g.nombre, size = 28.dp, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = g.nombre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (detalle != null) {
                        Text(
                            text = detalle,
                            style = MaterialTheme.typography.bodySmall.tabular,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatearImporte(g.importe, g.moneda),
                    style = MaterialTheme.typography.bodyMedium.tabular,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
