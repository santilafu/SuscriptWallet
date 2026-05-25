package com.subia.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.subia.android.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.theme.GradientIndigoEnd
import com.subia.android.ui.theme.GradientIndigoStart
import com.subia.android.ui.theme.Indigo400
import com.subia.android.ui.theme.Indigo500
import com.subia.shared.repository.CatalogRepository
import com.subia.shared.viewmodel.SuscripcionesUiState
import com.subia.shared.viewmodel.SuscripcionesViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Pantalla de detalle de una suscripción con opciones de editar y eliminar. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
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
    val catalogRepository: CatalogRepository = koinInject()
    val uriHandler = LocalUriHandler.current
    var cancelUrl by remember { mutableStateOf<String?>(null) }

    val suscripcion = when (val state = uiState) {
        is SuscripcionesUiState.Success -> state.suscripciones.find { it.id == suscripcionId }
        is SuscripcionesUiState.Offline -> state.suscripciones.find { it.id == suscripcionId }
        else -> null
    }

    LaunchedEffect(uiState) {
        if (uiState is SuscripcionesUiState.Error) {
            snackbarHostState.showSnackbar((uiState as SuscripcionesUiState.Error).mensaje)
        }
    }

    // Busca la URL de cancelación en el catálogo por nombre de servicio
    LaunchedEffect(suscripcion?.nombre) {
        val nombre = suscripcion?.nombre ?: return@LaunchedEffect
        catalogRepository.getAll().onSuccess { items ->
            cancelUrl = items.firstOrNull {
                it.nombre.equals(nombre, ignoreCase = true)
            }?.cancelUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(suscripcion?.nombre ?: stringResource(R.string.detail)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Cabecera con logo y nombre
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        )
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ServiceLogo(nombre = suscripcion.nombre, size = 72.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(GradientIndigoStart, GradientIndigoEnd, Color(0xFFA78BFA)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                )
                            ) {
                                append(suscripcion.nombre)
                            }
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (suscripcion.activa) stringResource(R.string.active) else stringResource(R.string.inactive),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (suscripcion.activa) Indigo400 else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                }

                // Filas de información
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    DetalleRow(stringResource(R.string.amount), "${suscripcion.precio} ${suscripcion.moneda}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    DetalleRow(stringResource(R.string.billing), periodoCiclo(suscripcion.periodoFacturacion))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    DetalleRow(stringResource(R.string.next_renewal), suscripcion.fechaRenovacion)
                    if (suscripcion.descripcion.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        DetalleRow(stringResource(R.string.description), suscripcion.descripcion)
                    }
                    if (suscripcion.notas.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        DetalleRow(stringResource(R.string.notes), suscripcion.notas)
                    }
                }

                // Botones de acción
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    suscripcion.let {
                        // Botón editar — filled indigo
                        Button(
                            onClick = { onNavigateToEditar(suscripcionId) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo500,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.edit_subscription), fontWeight = FontWeight.SemiBold)
                        }
                        // Botón cancelar suscripción — solo si existe URL oficial
                        if (cancelUrl != null) {
                            OutlinedButton(
                                onClick = { uriHandler.openUri(cancelUrl!!) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF97316)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFFF97316).copy(alpha = 0.6f)
                                )
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.go_cancel_subscription), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        // Botón eliminar — outlined error
                        OutlinedButton(
                            onClick = { mostrarDialogoEliminar = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.delete_subscription), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (mostrarDialogoEliminar) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoEliminar = false },
                title = { Text(stringResource(R.string.delete_subscription_confirm)) },
                text = { Text(stringResource(R.string.delete_irreversible)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mostrarDialogoEliminar = false
                            viewModel.eliminar(suscripcionId)
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoEliminar = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@Composable
private fun DetalleRow(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun periodoCiclo(ciclo: String): String = when (ciclo) {
    "MONTHLY" -> stringResource(R.string.monthly)
    "YEARLY" -> stringResource(R.string.yearly)
    "WEEKLY" -> stringResource(R.string.weekly)
    else -> ciclo
}
