package com.subia.android.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subia.android.ui.ServiceLogo
import com.subia.shared.model.Subscription
import com.subia.shared.viewmodel.SuscripcionesUiState
import com.subia.shared.viewmodel.SuscripcionesViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionesScreen(
    onNavigateToDetalle: (Long) -> Unit,
    onNavigateToNueva: () -> Unit,
    viewModel: SuscripcionesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNueva,
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White)
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState is SuscripcionesUiState.Loading,
            onRefresh = { viewModel.cargar() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when (val state = uiState) {
                is SuscripcionesUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is SuscripcionesUiState.Success -> {
                    if (state.suscripciones.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sin suscripciones", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("Toca + para añadir la primera", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        ListaSuscripciones(state.suscripciones, onNavigateToDetalle)
                    }
                }
                is SuscripcionesUiState.Offline -> Column {
                    BannerOffline("Mostrando datos guardados — sin conexión")
                    ListaSuscripciones(state.suscripciones, onNavigateToDetalle)
                }
                is SuscripcionesUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.cargar() }) { Text("Reintentar") }
                    }
                }
                is SuscripcionesUiState.SesionExpirada -> Unit
            }
        }
    }
}

@Composable
private fun ListaSuscripciones(suscripciones: List<Subscription>, onNavigateToDetalle: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Suscripciones", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${suscripciones.size} activas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
        }
        items(suscripciones, key = { it.id }) { sub ->
            SuscripcionCard(sub, onNavigateToDetalle)
        }
    }
}

@Composable
private fun SuscripcionCard(sub: Subscription, onNavigateToDetalle: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onNavigateToDetalle(sub.id) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ServiceLogo(nombre = sub.nombre, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(sub.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    periodoCiclo(sub.periodoFacturacion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Renueva: ${sub.fechaRenovacion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("%.2f €".format(sub.precio), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun periodoCiclo(ciclo: String): String = when (ciclo) {
    "MONTHLY" -> "Mensual"
    "YEARLY" -> "Anual"
    "WEEKLY" -> "Semanal"
    else -> ciclo
}
