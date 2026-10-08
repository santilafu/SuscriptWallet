package com.subia.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.subia.android.R
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.components.formatearImporte
import com.subia.android.util.fechaIsoLegible
import com.subia.android.util.openCustomTab
import com.subia.shared.model.GmailDetected
import com.subia.shared.viewmodel.GmailScanError
import com.subia.shared.viewmodel.GmailScanUiState
import com.subia.shared.viewmodel.GmailScanViewModel

/**
 * Pantalla de detección por Gmail. Abre el consentimiento en Custom Tab y, al volver por
 * deep link, muestra la lista seleccionable de suscripciones detectadas.
 *
 * Es un flujo modal: todos los estados tienen barra superior con "atrás". El botón
 * "Ya autoricé" queda como enlace discreto bajo el indicador de espera, solo por si el deep
 * link no vuelve (p. ej. navegador sin soporte de esquemas personalizados).
 *
 * @param returnStatus estado del deep link de vuelta ("ok"/"error"), o null si no se ha vuelto.
 * @param onReturnConsumed limpia el estado del deep link tras procesarlo.
 * @param onDone se llama con el nº de suscripciones añadidas para que la lista lo confirme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmailScanScreen(
    viewModel: GmailScanViewModel,
    returnStatus: String?,
    onReturnConsumed: () -> Unit,
    onDone: (added: Int) -> Unit,
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
            is GmailScanUiState.Done -> onDone(s.added)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detect_with_gmail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp, vertical = 16.dp)) {
            when (val s = state) {
                is GmailScanUiState.Idle -> Introduccion(onConectar = { viewModel.startScan() })

                is GmailScanUiState.LaunchConsent,
                is GmailScanUiState.AwaitingReturn -> CenterProgress(stringResource(R.string.gmail_scan_waiting)) {
                    TextButton(onClick = { viewModel.onReturnedFromConsent("ok") }) {
                        Text(
                            stringResource(R.string.gmail_scan_already_authorized),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is GmailScanUiState.LoadingResults -> CenterProgress(stringResource(R.string.gmail_scan_searching))

                is GmailScanUiState.Results -> Resultados(
                    items = s.items,
                    selected = selected,
                    todoSeleccionado = s.items.isNotEmpty() && s.items.all { it.id in selected },
                    onToggle = { viewModel.toggle(it) },
                    onAlternarTodo = { viewModel.alternarSeleccionarTodo() },
                    onAnadir = { viewModel.addSelected() }
                )

                is GmailScanUiState.Empty -> CenterMessage(
                    icon = Icons.Outlined.MarkEmailRead,
                    text = stringResource(R.string.gmail_scan_empty),
                    onBack = onBack
                )

                is GmailScanUiState.Adding -> CenterProgress(stringResource(R.string.gmail_scan_adding))

                is GmailScanUiState.Done -> CenterMessage(
                    icon = Icons.Outlined.CheckCircle,
                    text = stringResource(R.string.gmail_scan_done),
                    onBack = null
                )

                is GmailScanUiState.Error -> CenterMessage(
                    icon = null,
                    text = stringResource(s.error.stringRes()),
                    onBack = onBack
                ) {
                    Button(onClick = { viewModel.startScan() }) { Text(stringResource(R.string.retry)) }
                }
            }
        }
    }
}

/** Estado inicial: qué hacemos, qué no hacemos (privacidad) y un único botón para conectar. */
@Composable
private fun Introduccion(onConectar: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        // Los logos que más aparecen en recibos de correo: la promesa en imágenes, no en iconos.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Netflix" to "netflix.com", "Spotify" to "spotify.com", "Amazon" to "amazon.com", "ChatGPT" to "openai.com")
                .forEach { (nombre, dominio) ->
                    ServiceLogo(nombre = nombre, domain = dominio, size = 44.dp, contentDescription = null)
                }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.gmail_scan_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.gmail_scan_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onConectar,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.gmail_scan_connect), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ColumnScope.Resultados(
    items: List<GmailDetected>,
    selected: Set<Long>,
    todoSeleccionado: Boolean,
    onToggle: (Long) -> Unit,
    onAlternarTodo: () -> Unit,
    onAnadir: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            pluralStringResource(R.plurals.gmail_scan_found, items.size, items.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAlternarTodo) {
            Text(stringResource(if (todoSeleccionado) R.string.gmail_clear_selection else R.string.gmail_select_all))
        }
    }
    Spacer(Modifier.height(4.dp))
    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
        items(items, key = { it.id }) { item ->
            val checked = item.id in selected
            // Fila `toggleable` con rol Checkbox y casilla sin callback: un único nodo
            // enfocable por fila, con el estado marcado/no marcado anunciado por TalkBack.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = checked, role = Role.Checkbox) { onToggle(item.id) }
                    .padding(vertical = 8.dp)
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
                Spacer(Modifier.width(8.dp))
                ServiceLogo(nombre = item.serviceName, domain = item.domain, size = 40.dp, contentDescription = null)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(item.serviceName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.gmail_scan_item_meta,
                            formatearImporte(item.price, item.currency),
                            cycleLabel(item.billingCycle),
                            fechaIsoLegible(item.lastSeen)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onAnadir,
        enabled = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(pluralStringResource(R.plurals.gmail_scan_add_selected, selected.size, selected.size), fontWeight = FontWeight.SemiBold)
    }
}

/** Etiqueta localizada del ciclo de facturación que llega del backend (YEARLY/WEEKLY/MONTHLY). */
@Composable
private fun cycleLabel(cycle: String): String = when (cycle) {
    "YEARLY" -> stringResource(R.string.cycle_yearly)
    "WEEKLY" -> stringResource(R.string.cycle_weekly)
    else -> stringResource(R.string.cycle_monthly)
}

/** Traduce el error tipado del ViewModel (sin textos) al recurso localizado. */
private fun GmailScanError.stringRes(): Int = when (this) {
    GmailScanError.NoSePudoIniciar -> R.string.gmail_error_start
    GmailScanError.ConsentimientoFallido -> R.string.gmail_error_consent
    GmailScanError.ResultadosNoDisponibles -> R.string.gmail_error_results
    GmailScanError.AltaFallida -> R.string.gmail_error_add
    GmailScanError.SinConexion -> R.string.gmail_error_offline
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
        Text(text, textAlign = TextAlign.Center)
        if (extra != null) {
            Spacer(Modifier.height(16.dp))
            extra()
        }
    }
}

@Composable
private fun CenterMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    text: String,
    onBack: (() -> Unit)?,
    extra: @Composable (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        if (extra != null) {
            extra()
            Spacer(Modifier.height(8.dp))
        }
        if (onBack != null) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        }
    }
}
