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
import androidx.compose.material.icons.AutoMirrored
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.components.GastosPorCategoriaCard
import com.subia.android.ui.theme.GradientAmberEnd
import com.subia.android.ui.theme.GradientAmberStart
import com.subia.android.ui.theme.GradientIndigoEnd
import com.subia.android.ui.theme.GradientIndigoStart
import com.subia.android.ui.theme.GradientTealEnd
import com.subia.android.ui.theme.GradientTealStart
import com.subia.android.ui.theme.Warning
import com.subia.shared.model.DashboardSummary
import com.subia.shared.model.ProximaRenovacion
import com.subia.shared.viewmodel.DashboardUiState
import com.subia.shared.viewmodel.DashboardViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSuscripciones: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalesPorMoneda by viewModel.totalesPorMoneda.collectAsState()
    val totalesAnualesPorMoneda by viewModel.totalesAnualesPorMoneda.collectAsState()
    val gastosPorCategoria by viewModel.gastosPorCategoria.collectAsState()
    val isRefreshing = uiState is DashboardUiState.Loading

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refrescar() },
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            is DashboardUiState.Success -> DashboardContent(state.resumen, totalesPorMoneda, totalesAnualesPorMoneda, gastosPorCategoria, onNavigateToSuscripciones)
            is DashboardUiState.Offline -> Column {
                BannerOffline("Mostrando datos guardados — sin conexión")
                state.resumenCacheado?.let { DashboardContent(it, totalesPorMoneda, totalesAnualesPorMoneda, gastosPorCategoria, onNavigateToSuscripciones) }
            }
            is DashboardUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.cargarEstadisticas() }) { Text("Reintentar") }
                }
            }
            is DashboardUiState.SesionExpirada -> Unit
        }
    }
}

@Composable
private fun DashboardContent(
    resumen: DashboardSummary,
    totalesPorMoneda: Map<String, Double> = emptyMap(),
    totalesAnualesPorMoneda: Map<String, Double> = emptyMap(),
    gastosPorCategoria: Map<String, Double> = emptyMap(),
    onNavigateToSuscripciones: () -> Unit = {}
) {
    val gradientsMensual = listOf(
        Brush.linearGradient(listOf(GradientIndigoStart, GradientIndigoEnd)),
        Brush.linearGradient(listOf(GradientIndigoStart, GradientIndigoEnd)),
        Brush.linearGradient(listOf(GradientIndigoStart, GradientIndigoEnd))
    )
    val gradientsAnual = listOf(
        Brush.linearGradient(listOf(GradientTealStart, GradientTealEnd)),
        Brush.linearGradient(listOf(GradientTealStart, GradientTealEnd)),
        Brush.linearGradient(listOf(GradientTealStart, GradientTealEnd))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Resumen de tus suscripciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            if (totalesPorMoneda.isEmpty()) {
                // Sin datos detallados: mostrar tarjetas del resumen del servidor
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.EuroSymbol,
                        label = "Mensual",
                        value = "%.2f €".format(resumen.gastoMensual),
                        gradient = gradientsMensual[0]
                    )
                    GradientStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarToday,
                        label = "Anual",
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
                                    label = "Mensual ($moneda)",
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
                                        label = "Anual ($moneda)",
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
                        tint = Warning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Suscripciones activas",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${resumen.totalSuscripciones}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Warning
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ver suscripciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (resumen.renovacionesProximas.isNotEmpty()) {
            item {
                Text(
                    "Próximas renovaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(resumen.renovacionesProximas) { RenovacionCard(it) }
        } else {
            item {
                Text(
                    "No tienes renovaciones próximas",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GastosPorCategoriaCard(gastosPorCategoria = gastosPorCategoria)
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
            Icon(icon, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun RenovacionCard(renovacion: ProximaRenovacion) {
    val diasColor = when {
        renovacion.diasRestantes <= 3 -> MaterialTheme.colorScheme.error
        renovacion.diasRestantes <= 7 -> Warning
        else -> Color(0xFF22C55E)
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
            ServiceLogo(nombre = renovacion.nombre, size = 40.dp)
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
                    "${renovacion.diasRestantes}d",
                    style = MaterialTheme.typography.bodySmall,
                    color = diasColor,
                    fontWeight = FontWeight.SemiBold
                )
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
        Text(mensaje, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
