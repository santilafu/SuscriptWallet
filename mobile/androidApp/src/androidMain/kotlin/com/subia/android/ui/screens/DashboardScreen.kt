package com.subia.android.ui.screens

import androidx.compose.foundation.background
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
import com.subia.shared.model.DashboardSummary
import com.subia.shared.model.ProximaRenovacion
import com.subia.shared.viewmodel.DashboardUiState
import com.subia.shared.viewmodel.DashboardViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
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
            is DashboardUiState.Success -> DashboardContent(state.resumen)
            is DashboardUiState.Offline -> Column {
                BannerOffline("Mostrando datos guardados — sin conexión")
                state.resumenCacheado?.let { DashboardContent(it) }
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
private fun DashboardContent(resumen: DashboardSummary) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Resumen de tus suscripciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.EuroSymbol,
                    label = "Mensual",
                    value = "%.2f €".format(resumen.gastoMensual),
                    gradient = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
                )
                GradientStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CalendarToday,
                    label = "Anual",
                    value = "%.0f €".format(resumen.gastoAnual),
                    gradient = Brush.linearGradient(listOf(Color(0xFF14B8A6), Color(0xFF06B6D4)))
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Subscriptions, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Suscripciones activas", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("${resumen.totalSuscripciones}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFFF59E0B))
                }
            }
        }

        if (resumen.renovacionesProximas.isNotEmpty()) {
            item {
                Text("Próximas renovaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(resumen.renovacionesProximas) { RenovacionCard(it) }
        } else {
            item {
                Text("No tienes renovaciones próximas", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GradientStatCard(modifier: Modifier, icon: ImageVector, label: String, value: String, gradient: Brush) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(gradient).padding(16.dp)) {
        Column {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun RenovacionCard(renovacion: ProximaRenovacion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ServiceLogo(nombre = renovacion.nombre, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(renovacion.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(renovacion.fechaRenovacion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("%.2f €".format(renovacion.precio), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                val diasColor = when {
                    renovacion.diasRestantes <= 3 -> Color(0xFFEF4444)
                    renovacion.diasRestantes <= 7 -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }
                Text("${renovacion.diasRestantes}d", style = MaterialTheme.typography.bodySmall, color = diasColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun BannerOffline(mensaje: String) {
    Box(Modifier.fillMaxWidth().background(Color(0xFFF59E0B)).padding(8.dp), Alignment.Center) {
        Text(mensaje, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}
