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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.R
import com.subia.android.ui.BannerAdView
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.components.ErrorState
import com.subia.android.ui.components.GastosPorCategoriaCard
import com.subia.android.ui.components.TopSuscripcionesChartCard
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.theme.GradientIndigoDeepEnd
import com.subia.android.ui.theme.GradientIndigoDeepStart
import com.subia.android.ui.theme.GradientTealDeepEnd
import com.subia.android.ui.theme.GradientTealDeepStart
import com.subia.android.ui.theme.Warning
import com.subia.android.ui.theme.success
import com.subia.android.ui.theme.urgent
import com.subia.android.ui.theme.warning
import com.subia.shared.model.DashboardSummary
import com.subia.shared.model.ProximaRenovacion
import com.subia.shared.viewmodel.DashboardUiState
import com.subia.shared.viewmodel.DashboardViewModel
import com.subia.shared.viewmodel.TopSuscripcion
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSuscripciones: () -> Unit = {},
    onNavigateToResumenAnual: () -> Unit = {},
    onSesionExpirada: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalesPorMoneda by viewModel.totalesPorMoneda.collectAsState()
    val totalesAnualesPorMoneda by viewModel.totalesAnualesPorMoneda.collectAsState()
    val gastosPorCategoria by viewModel.gastosPorCategoria.collectAsState()
    val pruebasPorVencer by viewModel.pruebasPorVencer.collectAsState()
    val topSuscripciones by viewModel.topSuscripciones.collectAsState()
    val isRefreshing = uiState is DashboardUiState.Loading

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
                is DashboardUiState.Success -> DashboardContent(state.resumen, totalesPorMoneda, totalesAnualesPorMoneda, gastosPorCategoria, pruebasPorVencer, topSuscripciones, onNavigateToSuscripciones, onNavigateToResumenAnual)
                is DashboardUiState.Offline -> Column {
                    BannerOffline(stringResource(R.string.offline_data))
                    state.resumenCacheado?.let { DashboardContent(it, totalesPorMoneda, totalesAnualesPorMoneda, gastosPorCategoria, pruebasPorVencer, topSuscripciones, onNavigateToSuscripciones, onNavigateToResumenAnual) }
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
    totalesPorMoneda: Map<String, Double> = emptyMap(),
    totalesAnualesPorMoneda: Map<String, Double> = emptyMap(),
    gastosPorCategoria: Map<String, Double> = emptyMap(),
    pruebasPorVencer: List<com.subia.shared.model.ProximaRenovacion> = emptyList(),
    topSuscripciones: List<TopSuscripcion> = emptyList(),
    onNavigateToSuscripciones: () -> Unit = {},
    onNavigateToResumenAnual: () -> Unit = {}
) {
    // Gradientes "profundos" (tonos 600/700): el texto blanco al 100 % cumple 4,5:1 encima.
    val gradientsMensual = listOf(
        Brush.linearGradient(listOf(GradientIndigoDeepStart, GradientIndigoDeepEnd)),
        Brush.linearGradient(listOf(GradientIndigoDeepStart, GradientIndigoDeepEnd)),
        Brush.linearGradient(listOf(GradientIndigoDeepStart, GradientIndigoDeepEnd))
    )
    val gradientsAnual = listOf(
        Brush.linearGradient(listOf(GradientTealDeepStart, GradientTealDeepEnd)),
        Brush.linearGradient(listOf(GradientTealDeepStart, GradientTealDeepEnd)),
        Brush.linearGradient(listOf(GradientTealDeepStart, GradientTealDeepEnd))
    )

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
        // El primer elemento es el gasto mensual: lo más importante, arriba (D-01).
        item {
            if (totalesPorMoneda.isEmpty()) {
                // Sin datos detallados: mostrar tarjetas del resumen del servidor
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.EuroSymbol,
                        label = stringResource(R.string.monthly),
                        value = "%.2f €".format(resumen.gastoMensual),
                        gradient = gradientsMensual[0]
                    )
                    GradientStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarToday,
                        label = stringResource(R.string.yearly),
                        value = "%.0f €".format(resumen.gastoAnual),
                        gradient = gradientsAnual[0]
                    )
                }
            } else {
                // Con datos detallados: fila mensual + fila anual por divisa
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Fila 1: totales mensuales
                    val entradasMensuales = totalesPorMoneda.entries.toList()
                    entradasMensuales.chunked(2).forEachIndexed { rowIdx, rowEntries ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowEntries.forEachIndexed { colIdx, (moneda, total) ->
                                val gradientIdx = (rowIdx * 2 + colIdx) % gradientsMensual.size
                                GradientStatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.EuroSymbol,
                                    label = stringResource(R.string.monthly_currency, moneda),
                                    value = "%.2f %s".format(total, moneda),
                                    gradient = gradientsMensual[gradientIdx]
                                )
                            }
                            if (rowEntries.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Fila 2: totales anuales (gradiente teal para diferenciarlos)
                    val entradasAnuales = totalesAnualesPorMoneda.entries.toList()
                    if (entradasAnuales.isNotEmpty()) {
                        entradasAnuales.chunked(2).forEachIndexed { rowIdx, rowEntries ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowEntries.forEachIndexed { colIdx, (moneda, total) ->
                                    val gradientIdx = (rowIdx * 2 + colIdx) % gradientsAnual.size
                                    GradientStatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.CalendarToday,
                                        label = stringResource(R.string.yearly_currency, moneda),
                                        value = "%.0f %s".format(total, moneda),
                                        gradient = gradientsAnual[gradientIdx]
                                    )
                                }
                                if (rowEntries.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToSuscripciones() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Subscriptions,
                        null,
                        tint = MaterialTheme.colorScheme.warning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.active_subscriptions),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${resumen.totalSuscripciones}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.warning
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.view_subscriptions),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (pruebasPorVencer.isNotEmpty()) {
            item {
                PruebasPorVencerCard(pruebasPorVencer)
            }
        }

        // Sección "Próximas renovaciones": el título se mantiene siempre y, si no hay
        // ninguna, un estado vacío compacto dentro de la sección (D-09).
        item {
            Text(
                stringResource(R.string.upcoming_renewals),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (resumen.renovacionesProximas.isNotEmpty()) {
            items(resumen.renovacionesProximas) { RenovacionCard(it) }
        } else {
            item { SinRenovacionesCard() }
        }

        item {
            GastosPorCategoriaCard(gastosPorCategoria = gastosPorCategoria)
        }

        item {
            TopSuscripcionesChartCard(topSuscripciones = topSuscripciones)
        }

        // Última tarjeta: acceso al resumen anual compartible (R-02).
        item {
            TuAnioCard(totalAnualTexto = totalAnualTexto, onVerResumen = onNavigateToResumenAnual)
        }
    }
}

@Composable
private fun GradientStatCard(modifier: Modifier, icon: ImageVector, label: String, value: String, gradient: Brush) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .padding(16.dp)
    ) {
        Column {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 13.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }
    }
}

/** Estado vacío compacto de la sección de renovaciones: misma tarjeta que una renovación, sin datos. */
@Composable
private fun SinRenovacionesCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.EventAvailable,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.no_upcoming_renewals),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Tarjeta "Tu año en suscripciones": cifra anual (si ya está calculada) y CTA al resumen
 * compartible. Sin gradiente: el único gradiente del Dashboard es [GradientStatCard].
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
                    tint = MaterialTheme.colorScheme.primary,
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
                        style = MaterialTheme.typography.headlineSmall,
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
                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RenovacionCard(renovacion: ProximaRenovacion) {
    val diasColor = when {
        renovacion.diasRestantes <= 3 -> MaterialTheme.colorScheme.error
        renovacion.diasRestantes <= 7 -> MaterialTheme.colorScheme.warning
        else -> MaterialTheme.colorScheme.success
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ServiceLogo(nombre = renovacion.nombre, size = 40.dp, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(renovacion.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    renovacion.fechaRenovacion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.2f €".format(renovacion.precio),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (renovacion.diasRestantes == 1) stringResource(R.string.one_day)
                    else stringResource(R.string.n_days, renovacion.diasRestantes),
                    style = MaterialTheme.typography.bodySmall,
                    color = diasColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PruebasPorVencerCard(pruebas: List<com.subia.shared.model.ProximaRenovacion>) {
    val naranja = MaterialTheme.colorScheme.urgent
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, naranja, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = naranja.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.trials_expiring),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = naranja
            )
            pruebas.forEach { prueba ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(prueba.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            stringResource(R.string.expires_on, prueba.fechaRenovacion),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${prueba.diasRestantes}d",
                        fontWeight = FontWeight.Bold,
                        color = if (prueba.diasRestantes <= 3) MaterialTheme.colorScheme.error else naranja,
                        fontSize = 14.sp
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
