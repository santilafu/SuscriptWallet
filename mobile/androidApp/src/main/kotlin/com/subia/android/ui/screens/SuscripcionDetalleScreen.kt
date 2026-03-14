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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subia.shared.viewmodel.SuscripcionesUiState
import com.subia.shared.viewmodel.SuscripcionesViewModel
import org.koin.androidx.compose.koinViewModel

/** Pantalla de detalle de una suscripción con opciones de editar y eliminar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionDetalleScreen(
    suscripcionId: Long,
    onNavigateToEditar: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SuscripcionesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Busca la suscripción actual en el estado cargado
    val suscripcion = when (val state = uiState) {
        is SuscripcionesUiState.Success -> state.suscripciones.find { it.id == suscripcionId }
        is SuscripcionesUiState.Offline -> state.suscripciones.find { it.id == suscripcionId }
        else -> null
    }

    // Observar errores para mostrarlos como snackbar
    LaunchedEffect(uiState) {
        if (uiState is SuscripcionesUiState.Error) {
            snackbarHostState.showSnackbar((uiState as SuscripcionesUiState.Error).mensaje)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(suscripcion?.nombre ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    suscripcion?.let {
                        IconButton(onClick = { onNavigateToEditar(suscripcionId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { mostrarDialogoEliminar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (suscripcion == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetalleRow("Nombre", suscripcion.nombre)
                if (suscripcion.descripcion.isNotBlank()) DetalleRow("Descripción", suscripcion.descripcion)
                DetalleRow("Importe", "${suscripcion.precio} ${suscripcion.moneda}")
                DetalleRow("Facturación", periodoCiclo(suscripcion.periodoFacturacion))
                DetalleRow("Próxima renovación", suscripcion.fechaRenovacion)
                DetalleRow("Estado", if (suscripcion.activa) "Activa" else "Inactiva")
                if (suscripcion.notas.isNotBlank()) DetalleRow("Notas", suscripcion.notas)
            }
        }

        if (mostrarDialogoEliminar) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoEliminar = false },
                title = { Text("¿Eliminar suscripción?") },
                text = { Text("Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mostrarDialogoEliminar = false
                            viewModel.eliminar(suscripcionId)
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoEliminar = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
private fun DetalleRow(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

private fun periodoCiclo(ciclo: String): String = when (ciclo) {
    "MONTHLY" -> "Mensual"
    "YEARLY" -> "Anual"
    "WEEKLY" -> "Semanal"
    else -> ciclo
}
