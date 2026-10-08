package com.subia.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subia.android.R
import com.subia.android.ui.BannerAdView
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.components.CobrosPorMesCard
import com.subia.android.ui.components.ErrorState
import com.subia.android.ui.components.GastosPorCategoriaCard
import com.subia.android.ui.components.TiraCobros
import com.subia.android.ui.components.formatearFechaMedia
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.components.nombreMesLargo
import com.subia.android.ui.components.tabular
import com.subia.android.ui.components.textoDiasRelativo
import com.subia.android.ui.theme.GradientIndigoDeepEnd
import com.subia.android.ui.theme.GradientIndigoDeepStart
import com.subia.android.ui.theme.Warning
import com.subia.android.ui.theme.urgent
import com.subia.android.widget.actualizarWidgetResumen
import com.subia.shared.model.DashboardSummary
import com.subia.shared.model.ProximaRenovacion
import com.subia.shared.model.ProyeccionCobros
import com.subia.shared.viewmodel.DashboardUiState
import com.subia.shared.viewmodel.DashboardViewModel
import com.subia.shared.viewmodel.GastoCategoria
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
    val gastosPorCategoria by viewModel.gastosPorCategoriaDetalle.collectAsState()
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
            gastosPorCategoria = gastosPorCategoria,
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
                is DashboardUiState.Success -> contenido(state.resumen)
                is DashboardUiState.Refreshing -> contenido(state.resumen)
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
                    mensaje = state.mensaje,
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
    gastosPorCategoria: List<GastoCategoria>,
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Gasto mensual: lo más importante, arriba (D-01). Una tarjeta por divisa.
        item {
            if (totalesPorMoneda.isEmpty()) {
                HeroGastoMensual(
                    moneda = "EUR",
                    mensual = resumen.gastoMensual,
                    anual = resumen.gastoAnual,
                    proyeccion = proyeccion
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    totalesPorMoneda.forEach { (moneda, total) ->
                        HeroGastoMensual(
                            moneda = moneda,
                            mensual = total,
                            anual = totalesAnualesPorMoneda[moneda] ?: total * 12.0,
                            proyeccion = proyeccion
                        )
                    }
                }
            }
        }

        // 2. Acceso a la lista con el contador en tinta neutra (el ámbar queda solo para avisos, D-03).
        item { SuscripcionesActivasRow(resumen.totalSuscripciones, onNavigateToSuscripciones) }

        if (pruebasPorVencer.isNotEmpty()) {
            item { PruebasPorVencerCard(pruebasPorVencer) }
        }

        // 3. Tira de cobros (sustituye a "Próximas renovaciones").
        item {
            if (proyeccion != null) {
                TiraCobrosCard(proyeccion, onNavigateToDetalle)
            } else {
                SinCobrosCard()
            }
        }

        // 4. Qué te viene cada mes.
        if (proyeccion != null) {
            item {
                CobrosPorMesCard(
                    meses = proyeccion.meses,
                    moneda = monedaPrincipal,
                    onOpenSuscripcion = onNavigateToDetalle
                )
            }
        }

        item { GastosPorCategoriaCard(gastos = gastosPorCategoria) }

        // Última tarjeta: acceso al resumen anual compartible (R-02).
        item { TuAnioCard(totalAnualTexto = totalAnualTexto, onVerResumen = onNavigateToResumenAnual) }
    }
}

/**
 * Hero del Dashboard: gasto mensual normalizado en `displaySmall` con cifras tabulares, su
 * equivalente anual y, si hay proyección, lo que viene este mes y el siguiente. Único gradiente
 * de la pantalla (tonos 600/700) con todo el texto en blanco al 100 % (D-02).
 */
@Composable
private fun HeroGastoMensual(
    moneda: String,
    mensual: Double,
    anual: Double,
    proyeccion: ProyeccionCobros?
) {
    val etiqueta = stringResource(R.string.hero_monthly_label)
    val importe = formatearImporte(mensual, moneda)
    val anualTexto = stringResource(R.string.hero_per_year, formatearImporte(anual, moneda, decimales = 0))
    val esteMes = proyeccion?.totalMesActual?.get(moneda) ?: 0.0
    val mesSiguiente = proyeccion?.totalMesSiguiente?.get(moneda) ?: 0.0
    val nombreMesSiguiente = proyeccion?.meses?.getOrNull(1)?.let { nombreMesLargo(it.mes) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(GradientIndigoDeepStart, GradientIndigoDeepEnd)))
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$etiqueta $moneda: $importe, $anualTexto"
            }
    ) {
        Text(
            text = if (moneda == "EUR") etiqueta else "$etiqueta · $moneda",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = importe,
            color = Color.White,
            style = MaterialTheme.typography.displaySmall.tabular,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = anualTexto,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.tabular
        )

        if (proyeccion != null && nombreMesSiguiente != null) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                MiniCifra(
                    etiqueta = stringResource(R.string.this_month),
                    valor = formatearImporte(esteMes, moneda),
                    modifier = Modifier.weight(1f)
                )
                MiniCifra(
                    etiqueta = nombreMesSiguiente,
                    valor = formatearImporte(mesSiguiente, moneda),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniCifra(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = etiqueta,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
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
 * Tarjeta de la tira de cobros: título, línea del próximo cobro con urgencia en texto e icono
 * ("Netflix · Mañana · 12,99 €", D-04) y la tira de 14 días a sangre dentro de la tarjeta.
 */
@Composable
private fun TiraCobrosCard(proyeccion: ProyeccionCobros, onNavigateToDetalle: (Long) -> Unit) {
    val proximo = proyeccion.proximoCobro
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = stringResource(R.string.tira_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                if (proximo != null) {
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
            TiraCobros(
                dias = proyeccion.dias,
                proximoCobro = proximo,
                onOpenSuscripcion = onNavigateToDetalle
            )
        }
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
