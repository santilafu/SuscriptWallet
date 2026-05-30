package com.subia.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subia.android.util.openCustomTab
import com.subia.shared.viewmodel.GmailScanUiState
import com.subia.shared.viewmodel.GmailScanViewModel

/**
 * Pantalla de detección por Gmail. Abre el consentimiento en Custom Tab y, al volver por
 * deep link, muestra la lista seleccionable de suscripciones detectadas.
 *
 * @param returnStatus estado del deep link de vuelta ("ok"/"error"), o null si no se ha vuelto.
 * @param onReturnConsumed limpia el estado del deep link tras procesarlo.
 */
@Composable
fun GmailScanScreen(
    viewModel: GmailScanViewModel,
    returnStatus: String?,
    onReturnConsumed: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val selected by viewModel.selectedIds.collectAsState()

    // Reacciona a transiciones del ViewModel: abrir Custom Tab o cerrar al terminar.
    LaunchedEffect(state) {
        when (val s = state) {
            is GmailScanUiState.LaunchConsent -> {
                openCustomTab(context, s.connectUrl)
                viewModel.onConsentLaunched()
            }
            is GmailScanUiState.Done -> onDone()
            else -> {}
        }
    }

    // Al volver por el deep link, avisa al ViewModel y consume el estado.
    LaunchedEffect(returnStatus) {
        if (returnStatus != null) {
            viewModel.onReturnedFromConsent(returnStatus)
            onReturnConsumed()
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        when (val s = state) {
            is GmailScanUiState.Idle -> {
                Text("Detecta tus suscripciones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Conecta tu Gmail y buscaremos recibos de suscripciones para que no tengas que añadirlas a mano. Solo leemos; no guardamos tus correos.")
                Spacer(Modifier.height(24.dp))
                Button(onClick = { viewModel.startScan() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Conectar con Gmail")
                }
            }
            is GmailScanUiState.LaunchConsent,
            is GmailScanUiState.AwaitingReturn -> CenterProgress("Esperando la autorización…") {
                TextButton(onClick = { viewModel.onReturnedFromConsent("ok") }) { Text("Ya autoricé") }
            }
            is GmailScanUiState.LoadingResults -> CenterProgress("Buscando suscripciones…")
            is GmailScanUiState.Results -> {
                Text("Hemos encontrado ${s.items.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(s.items, key = { it.id }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Checkbox(checked = item.id in selected, onCheckedChange = { viewModel.toggle(item.id) })
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(item.serviceName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${item.price} ${item.currency} · ${cycleLabel(item.billingCycle)} · visto ${item.lastSeen}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.addSelected() },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Añadir ${selected.size} seleccionadas")
                }
            }
            is GmailScanUiState.Empty -> CenterMessage("No encontramos suscripciones en tu correo.", onBack)
            is GmailScanUiState.Adding -> CenterProgress("Añadiendo…")
            is GmailScanUiState.Done -> CenterProgress("Listo")
            is GmailScanUiState.Error -> CenterMessage(s.message, onBack) {
                Button(onClick = { viewModel.startScan() }) { Text("Reintentar") }
            }
        }
    }
}

private fun cycleLabel(cycle: String) = when (cycle) {
    "YEARLY" -> "anual"
    "WEEKLY" -> "semanal"
    else -> "mensual"
}

@Composable
private fun CenterProgress(text: String, extra: @Composable (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(text)
        if (extra != null) {
            Spacer(Modifier.height(16.dp))
            extra()
        }
    }
}

@Composable
private fun CenterMessage(text: String, onBack: () -> Unit, extra: @Composable (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        if (extra != null) {
            extra()
            Spacer(Modifier.height(8.dp))
        }
        TextButton(onClick = onBack) { Text("Volver") }
    }
}
