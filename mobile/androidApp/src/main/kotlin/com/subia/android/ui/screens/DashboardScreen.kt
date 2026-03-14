package com.subia.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subia.shared.model.DashboardSummary
import com.subia.shared.viewmodel.DashboardUiState
import com.subia.shared.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

/** Pantalla principal con totales de gasto y renovaciones próximas. */
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
                    Button(onClick = { viewModel.cargarEstadisticas() }) { Text("Reintentar") }
                }
            }
            is DashboardUiState.SesionExpirada -> Unit // Manejado en SubIAApp
        }
    }
}

@Composable
private fun DashboardContent(resumen: DashboardSummary) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Panel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Gasto mensual", formatEuros(resumen.gastoMensual))
                StatCard(Modifier.weight(1f), "Gasto anual", formatEuros(resumen.gastoAnual))
            }
        }
        item {
            StatCard(Modifier.fillMaxWidth(), "Suscripciones activas", resumen.totalSuscripciones.toString())
        }
        item {
            Text("Próximas renovaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (resumen.renovacionesProximas.isEmpty()) {
            item { Text("No tienes renovaciones próximas", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(resumen.renovacionesProximas) { renovacion ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(renovacion.nombre, fontWeight = FontWeight.Medium)
                            Text(renovacion.fechaRenovacion, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatEuros(renovacion.precio), fontWeight = FontWeight.SemiBold)
                            Text("en ${renovacion.diasRestantes} días",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (renovacion.diasRestantes <= 7) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, titulo: String, valor: String) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BannerOffline(mensaje: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatEuros(valor: Double): String =
    String.format(Locale("es", "ES"), "%,.2f €", valor)
