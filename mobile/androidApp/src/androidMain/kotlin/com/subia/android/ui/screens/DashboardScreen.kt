package com.subia.android.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subia.android.R
import com.subia.android.ui.BannerAdView
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.components.CobrosPorMesCard
import com.subia.android.ui.components.EaseOutFuerte
import com.subia.android.ui.components.EntradaEscalonada
import com.subia.android.ui.components.MovimientoUnaVez
import com.subia.android.ui.components.animacionesActivas
import com.subia.android.ui.components.escalaDiferida
import com.subia.android.ui.components.nombreMesEnFrase
import com.subia.android.ui.components.rememberEscalaPulsacion
import com.subia.android.ui.components.ErrorState
import com.subia.android.ui.components.textoErrorRemoto
import com.subia.android.ui.components.GastosPorCategoriaCard
import com.subia.android.ui.components.CalendarioCobros
import com.subia.android.ui.components.TiraCobros
import com.subia.android.ui.components.formatearFechaMedia
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.components.nombreMesLargo
import com.subia.android.ui.components.tabular
import com.subia.android.ui.components.textoDiasRelativo
import com.subia.android.ui.theme.GradientBrandDeepEnd
import com.subia.android.ui.theme.GradientBrandDeepMid
import com.subia.android.ui.theme.GradientBrandDeepStart
import com.subia.android.ui.theme.Warning
import com.subia.android.ui.theme.urgent
import com.subia.android.util.InicioPrefs
import com.subia.android.widget.actualizarWidgetResumen
import com.subia.shared.model.DashboardSummary
import com.subia.shared.model.GastoCategoria
import com.subia.shared.model.ProximaRenovacion
import com.subia.shared.model.ProyeccionCobros
import com.subia.shared.model.totalDeDias
import com.subia.shared.viewmodel.DashboardUiState
import com.subia.shared.viewmodel.DashboardViewModel
import com.subia.shared.viewmodel.resumenDisponible
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.koin.compose.viewmodel.koinViewModel

/**
 * Inicio. Orden de lectura (de más a menos importante):
 * 1. Gasto mensual (hero) con el total que viene este mes y el siguiente.
 * 2. Tira de cobros: 14 días con los logos posados sobre el día en que cobran (elemento firma).
 * 3. Qué te viene cada mes: 12 barras + lista de cargos del mes tocado.
 * 4. Gasto por categoría y acceso al resumen anual.
 *
 * @param onNavigateToDetalle abre el detalle de una suscripción (toque en un logo de la tira o
 * en un cargo del mes). Por defecto cae a la lista de suscripciones hasta que se cablee la ruta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSuscripciones: () -> Unit = {},
    onNavigateToResumenAnual: () -> Unit = {},
    onSesionExpirada: () -> Unit = {},
    onNavigateToDetalle: (Long) -> Unit = { onNavigateToSuscripciones() },
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalesPorMoneda by viewModel.totalesPorMoneda.collectAsState()
    val totalesAnualesPorMoneda by viewModel.totalesAnualesPorMoneda.collectAsState()
    val gastosCategoriaMes by viewModel.gastosCategoriaMes.collectAsState()
    val gastosCategoriaAnio by viewModel.gastosCategoriaAnio.collectAsState()
    val pruebasPorVencer by viewModel.pruebasPorVencer.collectAsState()
    val proyeccion by viewModel.proyeccion.collectAsState()
    val context = LocalContext.current

    // Loading solo en la carga inicial; en Refreshing el contenido sigue en pantalla (D-05).
    val isRefreshing = uiState is DashboardUiState.Refreshing

    // El widget lee la misma caché: en cuanto hay datos frescos se le pide que se repinte (W-03).
    LaunchedEffect(uiState is DashboardUiState.Success) {
        if (uiState is DashboardUiState.Success) actualizarWidgetResumen(context)
    }

    val contenido: @Composable (DashboardSummary) -> Unit = { resumen ->
        DashboardContent(
            resumen = resumen,
            totalesPorMoneda = totalesPorMoneda,
            totalesAnualesPorMoneda = totalesAnualesPorMoneda,
            gastosCategoriaMes = gastosCategoriaMes,
            gastosCategoriaAnio = gastosCategoriaAnio,
            pruebasPorVencer = pruebasPorVencer,
            proyeccion = proyeccion,
            onNavigateToSuscripciones = onNavigateToSuscripciones,
            onNavigateToResumenAnual = onNavigateToResumenAnual,
            onNavigateToDetalle = onNavigateToDetalle
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refrescar() },
            modifier = Modifier.weight(1f)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                // Una sola rama para los dos estados con datos: con dos ramas, pasar de
                // Refreshing (caché) a Success recreaba el contenido entero (se perdía el
                // scroll y la entrada animada se cortaba a medias).
                is DashboardUiState.Success, is DashboardUiState.Refreshing ->
                    state.resumenDisponible?.let { contenido(it) }
                is DashboardUiState.Offline -> Column {
                    BannerOffline(stringResource(R.string.offline_data))
                    val cacheado = state.resumenCacheado
                    if (cacheado != null) contenido(cacheado)
                    else ErrorState(
                        mensaje = stringResource(R.string.offline_data),
                        onRetry = { viewModel.cargarEstadisticas() }
                    )
                }
                is DashboardUiState.Error -> ErrorState(
                    mensaje = textoErrorRemoto(state.error, R.string.error_load_dashboard),
                    onRetry = { viewModel.cargarEstadisticas() }
                )
                is DashboardUiState.SesionExpirada -> LaunchedEffect(Unit) { onSesionExpirada() }
            }
        }
        BannerAdView(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DashboardContent(
    resumen: DashboardSummary,
    totalesPorMoneda: Map<String, Double>,
    totalesAnualesPorMoneda: Map<String, Double>,
    gastosCategoriaMes: List<GastoCategoria>,
    gastosCategoriaAnio: List<GastoCategoria>,
    pruebasPorVencer: List<ProximaRenovacion>,
    proyeccion: ProyeccionCobros?,
    onNavigateToSuscripciones: () -> Unit,
    onNavigateToResumenAnual: () -> Unit,
    onNavigateToDetalle: (Long) -> Unit
) {
    // Divisa principal del usuario: la primera del desglose (EUR si existe) o EUR por defecto.
    val monedaPrincipal = totalesPorMoneda.keys.firstOrNull() ?: "EUR"

    // Cifra para la tarjeta "Tu año": el total anual ya calculado por el ViewModel (divisa
    // principal) o, si aún no hay desglose, el gasto anual del resumen del servidor.
    val totalAnualTexto = totalesAnualesPorMoneda.entries.firstOrNull()
        ?.let { formatearImporte(it.value, it.key, decimales = 0) }
        ?: resumen.gastoAnual.takeIf { it > 0 }?.let { formatearImporte(it, "EUR", decimales = 0) }

    // Entrada escalonada y cuenta de la cifra: solo la primera vez que se pinta el Inicio en
    // este proceso (ni al volver atrás ni al girar) y nunca con las animaciones del sistema
    // desactivadas. Las tarjetas que se componen después (al hacer scroll) ya no entran animadas:
    // la "ventana" se cierra en cuanto ha pasado la primera pintada.
    val activas = animacionesActivas()
    val animarEntrada = remember { activas && MovimientoUnaVez.reclamar(CLAVE_ENTRADA_INICIO) }
    val ventana = remember { VentanaEntrada(abierta = animarEntrada) }
    // Progreso (0..1) de la cuenta de la cifra del hero. Vive aquí y no en el hero: cuando llegan
    // los totales calculados en el cliente el hero se vuelve a crear y la cuenta no debe reiniciarse.
    val cuenta = remember { Animatable(if (animarEntrada) 0f else 1f) }
    if (animarEntrada) {
        LaunchedEffect(Unit) {
            delay(VENTANA_ENTRADA_MS)
            ventana.abierta = false
        }
        LaunchedEffect(Unit) { cuenta.animateTo(1f, tween(CUENTA_CIFRA_MS, easing = EaseOutFuerte)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        var orden = 0

        // 1. Gasto mensual: lo más importante, arriba (D-01). Una tarjeta por divisa.
        val ordenHero = orden++
        item {
            EntradaEscalonada(ordenHero, ventana.abierta) {
                if (totalesPorMoneda.isEmpty()) {
                    HeroGastoMensual(
                        moneda = "EUR",
                        mensual = resumen.gastoMensual,
                        anual = resumen.gastoAnual,
                        proyeccion = proyeccion,
                        progresoCifra = { cuenta.value }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        totalesPorMoneda.forEach { (moneda, total) ->
                            HeroGastoMensual(
                                moneda = moneda,
                                mensual = total,
                                anual = totalesAnualesPorMoneda[moneda] ?: total * 12.0,
                                proyeccion = proyeccion,
                                progresoCifra = { cuenta.value }
                            )
                        }
                    }
                }
            }
        }

        // 2. Acceso a la lista con el contador en tinta neutra (el ámbar queda solo para avisos, D-03).
        val ordenActivas = orden++
        item {
            EntradaEscalonada(ordenActivas, ventana.abierta) {
                SuscripcionesActivasRow(resumen.totalSuscripciones, onNavigateToSuscripciones)
            }
        }

        if (pruebasPorVencer.isNotEmpty()) {
            val ordenPruebas = orden++
            item { EntradaEscalonada(ordenPruebas, ventana.abierta) { PruebasPorVencerCard(pruebasPorVencer) } }
        }

        // 3. Tira de cobros (sustituye a "Próximas renovaciones").
        val ordenTira = orden++
        item {
            EntradaEscalonada(ordenTira, ventana.abierta) {
                if (proyeccion != null) {
                    TiraCobrosCard(proyeccion, onNavigateToDetalle)
                } else {
                    SinCobrosCard()
                }
            }
        }

        // 4. Qué te viene cada mes.
        if (proyeccion != null) {
            val ordenMeses = orden++
            item {
                EntradaEscalonada(ordenMeses, ventana.abierta) {
                    CobrosPorMesCard(
                        meses = proyeccion.meses,
                        moneda = monedaPrincipal,
                        onOpenSuscripcion = onNavigateToDetalle
                    )
                }
            }
        }

        val ordenCategorias = orden++
        item { EntradaEscalonada(ordenCategorias, ventana.abierta) { GastosPorCategoriaCard(gastosCategoriaMes, gastosCategoriaAnio) } }

        // Última tarjeta: acceso al resumen anual compartible (R-02).
        val ordenAnio = orden
        item {
            EntradaEscalonada(ordenAnio, ventana.abierta) {
                TuAnioCard(totalAnualTexto = totalAnualTexto, onVerResumen = onNavigateToResumenAnual)
            }
        }
    }
}

/** La entrada del Inicio solo se ve una vez por proceso (ver [MovimientoUnaVez]). */
private const val CLAVE_ENTRADA_INICIO = "inicio_entrada"

/** Tiempo tras la primera pintada en el que una tarjeta recién compuesta aún entra animada. */
private const val VENTANA_ENTRADA_MS = 400L

/** Si las tarjetas que se componen ahora deben entrar animadas. No es estado de Compose a propósito. */
private class VentanaEntrada(var abierta: Boolean)

/**
 * Hero del Dashboard. Dos cifras distintas que no deben confundirse:
 * - "Gasto mensual medio" (grande, `displaySmall` tabular): suma de equivalentes mensuales; un
 *   trimestral de 90 € cuenta 30 € todos los meses. Debajo, su total anual.
 * - "Este mes pagas" (abajo, con el mes siguiente al lado): lo que vence en el mes natural
 *   ENTERO, incluidos los cobros ya pasados; el trimestral suma 90 € solo en su mes. Antes era
 *   "Este mes" con lo que quedaba desde hoy (`totalMesActual`, que sigue usando el widget).
 * Único gradiente de la pantalla (tonos 600/700) con todo el texto en blanco al 100 % (D-02).
 */
@Composable
private fun HeroGastoMensual(
    moneda: String,
    mensual: Double,
    anual: Double,
    proyeccion: ProyeccionCobros?,
    progresoCifra: () -> Float = { 1f }
) {
    val etiqueta = stringResource(R.string.hero_monthly_label)
    val importe = formatearImporte(mensual, moneda)
    val anualTexto = stringResource(R.string.hero_per_year, formatearImporte(anual, moneda, decimales = 0))
    val esteMes = proyeccion?.totalMesCompleto?.get(moneda) ?: 0.0
    val mesSiguiente = proyeccion?.totalMesSiguiente?.get(moneda) ?: 0.0
    val nombreMesSiguiente = proyeccion?.meses?.getOrNull(1)?.let { nombreMesLargo(it.mes) }
    val etiquetaEsteMes = stringResource(R.string.this_month)
    val esteMesTexto = formatearImporte(esteMes, moneda)
    val mesSiguienteTexto = formatearImporte(mesSiguiente, moneda)
    val conMeses = proyeccion != null && nombreMesSiguiente != null

    // TalkBack lee el hero de una vez y con etiquetas: "Gasto mensual medio: 45,30 €. 544 € al
    // año. Este mes pagas: 30,00 €. Diciembre: 12,99 €." (la divisa la verbaliza el lector).
    val etiquetaConMoneda = if (moneda == "EUR") etiqueta else "$etiqueta $moneda"
    val descripcion = buildList {
        add(stringResource(R.string.a11y_label_value, etiquetaConMoneda, importe))
        add(anualTexto)
        if (conMeses) {
            add(stringResource(R.string.a11y_label_value, etiquetaEsteMes, esteMesTexto))
            add(stringResource(R.string.a11y_label_value, nombreMesSiguiente.orEmpty(), mesSiguienteTexto))
        }
    }.joinToString(". ", postfix = ".")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(GradientBrandDeepStart, GradientBrandDeepMid, GradientBrandDeepEnd)))
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = descripcion
            }
    ) {
        Text(
            text = if (moneda == "EUR") etiqueta else "$etiqueta · $moneda",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(2.dp))
        CifraContando(valor = mensual, moneda = moneda, progreso = progresoCifra)
        Text(
            text = anualTexto,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.tabular
        )

        if (conMeses) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
            Spacer(Modifier.height(12.dp))
            // Con fuente muy grande, dos columnas de media anchura recortaban las cifras: se
            // apilan una debajo de otra.
            if (LocalDensity.current.fontScale >= ESCALA_FUENTE_APILAR) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniCifra(etiqueta = etiquetaEsteMes, valor = esteMesTexto)
                    MiniCifra(etiqueta = nombreMesSiguiente.orEmpty(), valor = mesSiguienteTexto)
                }
            } else {
                Row(Modifier.fillMaxWidth()) {
                    MiniCifra(etiqueta = etiquetaEsteMes, valor = esteMesTexto, modifier = Modifier.weight(1f))
                    MiniCifra(etiqueta = nombreMesSiguiente.orEmpty(), valor = mesSiguienteTexto, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Escala de fuente del sistema a partir de la cual las cifras del hero se apilan. */
private const val ESCALA_FUENTE_APILAR = 1.5f

/** Duración de la cuenta de la cifra principal: más larga que una interacción porque se ve una vez. */
private const val CUENTA_CIFRA_MS = 750

/**
 * Cifra principal del hero. Mientras [progreso] (0..1) no llega a 1 muestra `valor × progreso`
 * (la cuenta desde 0 con ease-out fuerte de la primera carga); después, [valor] exacto. La lectura
 * de [progreso] ocurre aquí dentro, así que en cada frame de la cuenta solo se recompone este
 * texto, no el hero ni la pantalla. TalkBack lee siempre la cifra final (descripción del hero).
 */
@Composable
private fun CifraContando(valor: Double, moneda: String, progreso: () -> Float) {
    val p = progreso()
    Text(
        text = formatearImporte(if (p >= 1f) valor else valor * p, moneda),
        color = Color.White,
        style = MaterialTheme.typography.displaySmall.tabular,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

@Composable
private fun MiniCifra(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        // Hasta 2 líneas: "Este mes pagas" (o "Ce mois-ci, vous payez") no debe cortarse.
        Text(
            text = etiqueta,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = valor,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.tabular,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SuscripcionesActivasRow(total: Int, onClick: () -> Unit) {
    // Escala 0,97 al pulsar (además del ripple), leída en la capa gráfica.
    val interaccion = remember { MutableInteractionSource() }
    val escala = rememberEscalaPulsacion(interaccion)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .escalaDiferida(escala)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaccion, indication = LocalIndication.current, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Subscriptions,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.active_subscriptions),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.titleLarge.tabular,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.view_subscriptions),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Tarjeta de la tira de cobros con dos vistas, elegidas con el selector de la cabecera:
 * - "14 días": línea del próximo cobro con urgencia en texto e icono ("Netflix · Mañana ·
 *   12,99 €", D-04) y la tira de hoy a +13.
 * - "Este mes": el mes natural entero (del 1 al último, como "Este mes pagas"), con el número
 *   de cobros y el total del mes y un calendario de lunes a domingo ([CalendarioCobros]) con
 *   la lista de cobros del día elegido debajo.
 * La elección se recuerda (rememberSaveable + [InicioPrefs]). El cambio es un fundido corto con
 * un desplazamiento de 16 dp desde el lado del segmento elegido; la altura se anima, no salta.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TiraCobrosCard(proyeccion: ProyeccionCobros, onNavigateToDetalle: (Long) -> Unit) {
    val context = LocalContext.current
    val proximo = proyeccion.proximoCobro
    var modoMes by rememberSaveable { mutableStateOf(InicioPrefs.tiraEnModoMes(context)) }
    // Una proyección sin días del mes (no debería pasar) se queda en la vista de 14 días.
    val vistaMes = modoMes && proyeccion.diasMes.isNotEmpty()
    val desplazamiento = with(LocalDensity.current) { 16.dp.roundToPx() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            // Título y selector en una fila; si no caben (letra grande, francés) el selector baja.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Crossfade(
                    targetState = vistaMes,
                    animationSpec = tween(150),
                    label = "tituloTira",
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 12.dp)
                ) { mes ->
                    Text(
                        text = if (mes) {
                            stringResource(R.string.tira_title_month, nombreMesEnFrase(proyeccion.hoy.monthNumber))
                        } else {
                            stringResource(R.string.tira_title)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                SelectorModoTira(
                    modoMes = vistaMes,
                    onCambiar = { mes ->
                        modoMes = mes
                        InicioPrefs.guardarTiraEnModoMes(context, mes)
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            Spacer(Modifier.height(4.dp))

            AnimatedContent(
                targetState = vistaMes,
                transitionSpec = {
                    // Hacia "Este mes" (segmento derecho) lo nuevo llega desde la derecha, y al revés.
                    val lado = if (targetState) 1 else -1
                    (fadeIn(tween(200, delayMillis = 50, easing = EaseOutFuerte)) +
                        slideInHorizontally(tween(240, easing = EaseOutFuerte)) { lado * desplazamiento })
                        .togetherWith(fadeOut(tween(110)))
                        .using(SizeTransform(clip = false) { _, _ -> tween(240, easing = EaseOutFuerte) })
                },
                label = "modoTira"
            ) { mes ->
                // En "Este mes" ocupa todo el ancho para centrar el calendario (de ancho topado).
                Column(if (mes) Modifier.fillMaxWidth() else Modifier) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        if (mes) {
                            ResumenMesLinea(proyeccion)
                        } else if (proximo != null) {
                            ProximoCobroLinea(proximo.nombre, proximo.importe, proximo.moneda, proyeccion.hoy, proximo.fecha)
                        } else {
                            Text(
                                text = stringResource(R.string.tira_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (mes) {
                        // El mes entero a la vista, sin desplazarse (de 4 a 6 semanas).
                        CalendarioCobros(
                            dias = proyeccion.diasMes,
                            hoy = proyeccion.hoy,
                            onOpenSuscripcion = onNavigateToDetalle,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(horizontal = 12.dp)
                        )
                    } else {
                        TiraCobros(
                            dias = proyeccion.dias,
                            hoy = proyeccion.hoy,
                            proximoCobro = proximo,
                            onOpenSuscripcion = onNavigateToDetalle
                        )
                    }
                }
            }
        }
    }
}

/**
 * "5 cobros · 61,96 €": cobros del mes entero y su total, la misma cifra que "Este mes pagas"
 * del hero ([totalDeDias] sobre los días del mes coincide con `totalMesCompleto`).
 */
@Composable
private fun ResumenMesLinea(proyeccion: ProyeccionCobros) {
    val numCobros = remember(proyeccion) { proyeccion.diasMes.sumOf { it.cobros.size } }
    val totales = remember(proyeccion) { totalDeDias(proyeccion.diasMes) }
    val texto = if (numCobros == 0) {
        stringResource(R.string.month_no_charges)
    } else {
        pluralStringResource(R.plurals.month_charges_count, numCobros, numCobros) + " · " +
            totales.entries.joinToString(" · ") { formatearImporte(it.value, it.key) }
    }
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium.tabular,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Selector segmentado "14 días | Este mes": pastilla pequeña de 40 dp (Compose amplía la zona
 * táctil de cada segmento hasta 48 dp) con semántica de pestañas y estado seleccionado. El
 * segmento elegido cambia de color en 180 ms; el fondo se pinta en `drawBehind`.
 */
@Composable
private fun SelectorModoTira(modoMes: Boolean, onCambiar: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val elegir = { mes: Boolean ->
        if (mes != modoMes) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onCambiar(mes)
        }
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
            .selectableGroup()
    ) {
        SegmentoModo(stringResource(R.string.tira_mode_14_days), seleccionado = !modoMes) { elegir(false) }
        SegmentoModo(stringResource(R.string.tira_mode_month), seleccionado = modoMes) { elegir(true) }
    }
}

@Composable
private fun SegmentoModo(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val fondo by animateColorAsState(
        targetValue = if (seleccionado) colores.primaryContainer else colores.primaryContainer.copy(alpha = 0f),
        animationSpec = tween(180, easing = EaseOutFuerte),
        label = "fondoSegmento"
    )
    val tinta by animateColorAsState(
        targetValue = if (seleccionado) colores.onPrimaryContainer else colores.onSurfaceVariant,
        animationSpec = tween(180, easing = EaseOutFuerte),
        label = "tintaSegmento"
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .drawBehind { drawRect(fondo) }
            .selectable(selected = seleccionado, role = Role.Tab, onClick = onClick)
            .heightIn(min = 34.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Medium,
            color = tinta,
            maxLines = 1
        )
    }
}

/** "Próximo cobro · Netflix · Mañana · 12,99 €" con icono de reloj si es en ≤ 3 días. */
@Composable
private fun ProximoCobroLinea(nombre: String, importe: Double, moneda: String, hoy: LocalDate, fecha: LocalDate) {
    val dias = hoy.daysUntil(fecha)
    val urgente = dias <= 3
    val color = if (urgente) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (urgente) Icons.Outlined.Schedule else Icons.Outlined.Event,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$nombre · ${textoDiasRelativo(dias)} · ${formatearImporte(importe, moneda)}",
            style = MaterialTheme.typography.bodyMedium.tabular,
            fontWeight = if (urgente) FontWeight.SemiBold else FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Estado vacío compacto cuando aún no hay suscripciones de las que proyectar cobros. */
@Composable
private fun SinCobrosCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.EventAvailable,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.tira_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.no_upcoming_renewals),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tarjeta "Tu año en suscripciones": cifra anual (si ya está calculada) y CTA al resumen
 * compartible. Sin gradiente: el único gradiente del Dashboard es el hero.
 */
@Composable
private fun TuAnioCard(totalAnualTexto: String?, onVerResumen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.resumen_anual_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (totalAnualTexto != null) {
                Column {
                    Text(
                        stringResource(R.string.resumen_anual_total_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        totalAnualTexto,
                        style = MaterialTheme.typography.headlineSmall.tabular,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                stringResource(R.string.year_card_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onVerResumen,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.year_card_cta))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Pruebas gratuitas que vencen en ≤ 7 días: color de aviso + texto relativo e icono (no solo color). */
@Composable
private fun PruebasPorVencerCard(pruebas: List<ProximaRenovacion>) {
    val naranja = MaterialTheme.colorScheme.urgent
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, naranja, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = naranja.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = naranja, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.trials_expiring),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = naranja
                )
            }
            pruebas.forEach { prueba ->
                val fechaTexto = runCatching { formatearFechaMedia(LocalDate.parse(prueba.fechaRenovacion)) }
                    .getOrDefault(prueba.fechaRenovacion)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ServiceLogo(nombre = prueba.nombre, size = 32.dp, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(prueba.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.expires_on, fechaTexto),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = textoDiasRelativo(prueba.diasRestantes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (prueba.diasRestantes <= 3) MaterialTheme.colorScheme.error else naranja
                    )
                }
            }
        }
    }
}

@Composable
fun BannerOffline(mensaje: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Warning)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        Alignment.Center
    ) {
        // Texto oscuro sobre amber para cumplir contraste WCAG AA (blanco sobre amber falla).
        Text(mensaje, color = Color(0xFF1A1200), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
