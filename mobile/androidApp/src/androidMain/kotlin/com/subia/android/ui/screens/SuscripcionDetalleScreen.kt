package com.subia.android.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.subia.android.ui.conRecorteLateral
import com.subia.android.util.fechaIsoLegible
import com.subia.android.util.proximaRenovacionIso
import com.subia.android.ui.components.EmptyState
import com.subia.android.ui.components.ErrorState
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.components.textoErrorSuscripciones
import com.subia.android.ui.components.textoPeriodo
import com.subia.shared.model.Periodicidad
import com.subia.shared.model.PeriodoCobro
import com.subia.shared.model.equivalenteMensualInterno
import com.subia.shared.model.periodoCobro
import com.subia.shared.model.totalAnualInterno
import com.subia.shared.model.plazoGasto
import com.subia.shared.model.terminado
import com.subia.android.ui.components.EaseOutFuerte
import com.subia.android.ui.components.animacionesActivas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.Locale
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.SearchOff
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
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.subia.android.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.theme.Violet600
import com.subia.android.ui.theme.success
import com.subia.android.ui.theme.urgent
import com.subia.shared.repository.CatalogRepository
import com.subia.shared.model.Subscription
import com.subia.shared.viewmodel.SuscripcionesError
import com.subia.shared.viewmodel.SuscripcionesUiState
import com.subia.shared.viewmodel.SuscripcionesViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Pantalla de detalle de una suscripción con opciones de editar y eliminar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionDetalleScreen(
    suscripcionId: Long,
    onNavigateToEditar: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onSesionExpirada: () -> Unit = {},
    viewModel: SuscripcionesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Igual que Inicio y Suscripciones: sesión caducada → cierre de sesión y login. Antes el
    // detalle se quedaba con el indicador de carga para siempre.
    if (uiState is SuscripcionesUiState.SesionExpirada) LaunchedEffect(Unit) { onSesionExpirada() }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var eliminando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val catalogRepository: CatalogRepository = koinInject()
    val uriHandler = LocalUriHandler.current
    var cancelUrl by remember { mutableStateOf<String?>(null) }

    val encontrada = when (val state = uiState) {
        is SuscripcionesUiState.Success -> state.suscripciones.find { it.id == suscripcionId }
        is SuscripcionesUiState.Offline -> state.suscripciones.find { it.id == suscripcionId }
        else -> null
    }
    // Un fallo al eliminar pone el estado en Error (sin lista): antes la suscripción
    // "desaparecía" y el detalle se quedaba con el indicador de carga para siempre. Se sigue
    // mostrando la última conocida y el error sale en el Snackbar.
    var ultimaConocida by remember { mutableStateOf<Subscription?>(null) }
    SideEffect { if (encontrada != null) ultimaConocida = encontrada }
    val suscripcion = encontrada ?: ultimaConocida.takeIf { uiState is SuscripcionesUiState.Error }
    // Sin datos y la carga falló (sin red y sin caché): error con reintento, no un spinner eterno.
    val errorDeCarga = ((uiState as? SuscripcionesUiState.Error)?.error as? SuscripcionesError.CargaFallida)
        ?.takeIf { suscripcion == null }

    // Lista ya cargada (no en carga inicial ni refrescando): si la suscripción no está, no va a
    // aparecer. Pasa al tocar el aviso de una suscripción borrada después: antes el detalle se
    // quedaba con el indicador de carga para siempre.
    val listaCargada = when (val state = uiState) {
        is SuscripcionesUiState.Success -> !state.isRefreshing
        is SuscripcionesUiState.Offline -> true
        else -> false
    }

    // El texto se resuelve aquí (contexto composable) y el efecto solo lo muestra.
    val mensajeError = (uiState as? SuscripcionesUiState.Error)?.let { textoErrorSuscripciones(it.error) }
    LaunchedEffect(uiState) {
        if (mensajeError != null && errorDeCarga == null) snackbarHostState.showSnackbar(mensajeError)
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
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.conRecorteLateral(),
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.conRecorteLateral(),
                title = { Text(stringResource(R.string.detail)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (errorDeCarga != null) {
            ErrorState(
                mensaje = textoErrorSuscripciones(errorDeCarga),
                onRetry = { viewModel.cargar() },
                modifier = Modifier.padding(innerPadding)
            )
        } else if (suscripcion == null && listaCargada && !eliminando) {
            EmptyState(
                icon = Icons.Outlined.SearchOff,
                titulo = stringResource(R.string.subscription_not_found_title),
                subtitulo = stringResource(R.string.subscription_not_found_desc),
                modifier = Modifier.padding(innerPadding),
                actionLabel = stringResource(R.string.back),
                onAction = onNavigateBack
            )
        } else if (suscripcion == null) {
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
                val hoy = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
                val terminado = suscripcion.terminado(hoy)
                val plazo = plazoGasto(suscripcion, hoy)
                // Cabecera: logo + nombre, sin gradientes. El logo del servicio pone el color (T-06).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ServiceLogo(nombre = suscripcion.nombre, size = 56.dp, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = suscripcion.nombre,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(6.dp))
                        // Chip de estado con color semántico (verde = activa). Precedencia:
                        // Inactiva (lo decide el usuario) > Terminado (pasó la fecha de fin) > Activa.
                        val vigente = suscripcion.activa && !terminado
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (vigente) MaterialTheme.colorScheme.success.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    when {
                                        !suscripcion.activa -> R.string.inactive
                                        terminado -> R.string.plazo_terminado
                                        else -> R.string.active
                                    }
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (vigente) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Filas de información
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    DetalleRow(stringResource(R.string.amount), formatearImporte(suscripcion.precio, suscripcion.moneda))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    val periodo = suscripcion.periodoCobro()
                    DetalleRow(stringResource(R.string.billing), nombrePeriodo(periodo))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    // Cuánto supone al mes y al año (mismas cifras que el Inicio), sea cual sea la
                    // periodicidad: también en la mensual y la anual, para que todos los gastos
                    // muestren las mismas filas (395 € al mes → 395,00 € y 4.740,00 €).
                    DetalleRow(
                        stringResource(R.string.hero_monthly_label),
                        formatearImporte(Periodicidad.aCifraVisible(suscripcion.equivalenteMensualInterno()), suscripcion.moneda)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    DetalleRow(
                        stringResource(R.string.resumen_anual_total_label),
                        formatearImporte(Periodicidad.aCifraVisible(suscripcion.totalAnualInterno()), suscripcion.moneda)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    // Terminado: en vez de la próxima renovación (que ya no llega), cuándo terminó.
                    if (terminado && plazo != null) {
                        DetalleRow(stringResource(R.string.plazo_ended_label), fechaIsoLegible(plazo.fin.toString()))
                    } else {
                        DetalleRow(stringResource(R.string.next_renewal), fechaIsoLegible(proximaRenovacionIso(suscripcion)))
                    }
                    suscripcion.fechaInicio?.takeIf { it.isNotBlank() }?.let { inicio ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        DetalleRow(stringResource(R.string.plazo_start_label), fechaIsoLegible(inicio))
                    }
                    if (plazo != null && !terminado) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        DetalleRow(stringResource(R.string.plazo_end_label), fechaIsoLegible(plazo.fin.toString()))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        DetalleRow(stringResource(R.string.plazo_cuotas_restantes), plazo.cuotasRestantes.toString())
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        DetalleRow(stringResource(R.string.plazo_pendiente), formatearImporte(plazo.pendiente, suscripcion.moneda))
                    }
                    // Barra de progreso: solo con fecha de inicio (sin ella no se saben las cuotas totales).
                    val totales = plazo?.cuotasTotales
                    val progreso = plazo?.progreso
                    if (plazo != null && totales != null && progreso != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        ProgresoPlazo(pagadas = plazo.cuotasPagadas ?: 0, totales = totales, progreso = progreso)
                    }
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
                                containerColor = Violet600,
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
                                // Sin navegador que abra la URL, openUri lanza: no debe cerrar la app.
                                onClick = { cancelUrl?.let { url -> runCatching { uriHandler.openUri(url) } } },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                // Neutro: abre la web del proveedor, no borra nada aquí. Dos botones
                                // seguidos en naranja y rojo competían; el rojo queda para Eliminar.
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.go_cancel_subscription), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        // Botón eliminar — outlined error
                        OutlinedButton(
                            onClick = { mostrarDialogoEliminar = true },
                            enabled = !eliminando,
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
                            if (eliminando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
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
                            // Se vuelve atrás solo cuando el servidor confirma: navegar antes
                            // destruía el ViewModel y cancelaba el DELETE en vuelo.
                            eliminando = true
                            scope.launch {
                                if (viewModel.eliminarAhora(suscripcionId)) onNavigateBack()
                                else eliminando = false
                            }
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

/**
 * "9 de 12 cuotas pagadas" con su barra. Texto y barra se leen juntos en TalkBack. La barra crece
 * desde 0 al abrir el detalle, salvo con las animaciones del sistema desactivadas.
 */
@Composable
private fun ProgresoPlazo(pagadas: Int, totales: Int, progreso: Float) {
    val animar = animacionesActivas()
    val valor = remember { Animatable(if (animar) 0f else progreso) }
    LaunchedEffect(progreso) {
        if (animar) valor.animateTo(progreso, tween(PROGRESO_PLAZO_MS, easing = EaseOutFuerte)) else valor.snapTo(progreso)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = pluralStringResource(R.plurals.plazo_progreso, totales, pagadas, totales),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { valor.value },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Violet600,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}

private const val PROGRESO_PLAZO_MS = 600

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

/**
 * Nombre del periodo como valor de la fila "Facturación": "Mensual", "Anual", "Semanal" o
 * "Cada 3 meses". Los intermedios y los ciclos antiguos (quincenal, diario) reutilizan la frase
 * de [textoPeriodo] con mayúscula inicial; antes un ciclo desconocido salía en crudo ("BIWEEKLY").
 */
@Composable
private fun nombrePeriodo(periodo: PeriodoCobro): String = when (periodo) {
    PeriodoCobro.PorMeses(1) -> stringResource(R.string.monthly)
    PeriodoCobro.PorMeses(12) -> stringResource(R.string.yearly)
    PeriodoCobro.Semanal -> stringResource(R.string.weekly)
    else -> textoPeriodo(periodo).replaceFirstChar { it.titlecase(Locale.getDefault()) }
}
