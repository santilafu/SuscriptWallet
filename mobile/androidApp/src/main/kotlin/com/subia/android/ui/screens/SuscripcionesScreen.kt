package com.subia.android.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subia.shared.model.Subscription
import com.subia.shared.viewmodel.SuscripcionesUiState
import com.subia.shared.viewmodel.SuscripcionesViewModel
import org.koin.androidx.compose.koinViewModel

/** Pantalla con la lista de suscripciones del usuario. */
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
            FloatingActionButton(onClick = onNavigateToNueva) {
                Icon(Icons.Default.Add, contentDescription = "Añadir suscripción")
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState is SuscripcionesUiState.Loading,
            onRefresh = { viewModel.cargar() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when (val state = uiState) {
                is SuscripcionesUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                is SuscripcionesUiState.Success -> {
                    if (state.suscripciones.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("Aún no tienes suscripciones.\nToca el botón + para añadir una",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Button(onClick = { viewModel.cargar() }) { Text("Reintentar") }
                    }
                }
                is SuscripcionesUiState.SesionExpirada -> Unit
            }
        }
    }
}

@Composable
private fun ListaSuscripciones(
    suscripciones: List<Subscription>,
    onNavigateToDetalle: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Suscripciones", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        items(suscripciones, key = { it.id }) { sub ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToDetalle(sub.id) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sub.nombre, fontWeight = FontWeight.SemiBold)
                        Text(
                            periodoCiclo(sub.periodoFacturacion),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${sub.precio} ${sub.moneda}", fontWeight = FontWeight.Bold)
                        Text(sub.fechaRenovacion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
