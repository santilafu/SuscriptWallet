package com.subia.android.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subia.android.R
import com.subia.android.util.Funciones
import com.subia.android.ui.BannerAdView
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.components.ErrorState
import com.subia.android.ui.components.textoErrorSuscripciones
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.components.importeCadaPeriodo
import com.subia.android.ui.theme.Violet600
import com.subia.android.ui.theme.urgent
import com.subia.android.util.fechaIsoLegible
import com.subia.android.util.proximaRenovacionIso
import com.subia.android.util.fechaIsoCorta
import com.subia.shared.model.Category
import com.subia.shared.model.Periodicidad
import com.subia.shared.model.Subscription
import com.subia.shared.model.equivalenteMensualInterno
import com.subia.shared.model.periodoCobro
import com.subia.shared.model.cuentaEnTotales
import com.subia.shared.model.plazoGasto
import com.subia.shared.model.terminado
import com.subia.shared.viewmodel.SuscripcionesUiState
import com.subia.shared.viewmodel.SuscripcionesViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

/** Servicios propuestos en el estado vacío: los que casi todo el mundo paga. */
private val serviciosSugeridos = listOf(
    "Netflix" to "netflix.com",
    "Spotify" to "spotify.com",
    "ChatGPT" to "openai.com",
    // Un recibo (teleco) en vez de Iberdrola, cuyo favicon solo existe a 16 px y salía borroso.
    "Movistar" to "movistar.es"
)

/**
 * Lista de suscripciones.
 *
 * - La recarga la decide el ViewModel ([com.subia.shared.viewmodel.SuscripcionesCambios]):
 *   al volver del detalle no se parpadea; solo se refresca cuando algo cambió.
 * - Deslizar una fila hacia la izquierda la elimina con "Deshacer" (el borrado real se
 *   difiere en el ViewModel); el botón Eliminar del detalle sigue existiendo como alternativa
 *   sin gesto.
 * - Sin suscripciones no hay banner de anuncios: la pantalla entera es la invitación.
 *
 * @param gmailAdded nº de suscripciones añadidas desde la detección por Gmail (para el Snackbar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionesScreen(
    onNavigateToDetalle: (Long) -> Unit,
    onNavigateToNueva: (prefillNombre: String?) -> Unit,
    onDetectGmail: () -> Unit = {},
    gmailAdded: Int? = null,
    onGmailAddedConsumed: () -> Unit = {},
    onSesionExpirada: () -> Unit = {},
    viewModel: SuscripcionesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Confirmación de la detección por Gmail: "3 suscripciones añadidas".
    LaunchedEffect(gmailAdded) {
        if (gmailAdded != null) {
            onGmailAddedConsumed()
            snackbarHostState.showSnackbar(
                context.resources.getQuantityString(R.plurals.gmail_added_snackbar, gmailAdded, gmailAdded)
            )
        }
    }

    val eliminarConDeshacer: (Subscription) -> Unit = { sub ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.eliminarConDeshacer(sub.id)
        scope.launch {
            // Un segundo borrado cierra el Snackbar anterior; su ventana de deshacer sigue en el VM.
            snackbarHostState.currentSnackbarData?.dismiss()
            val resultado = snackbarHostState.showSnackbar(
                message = context.getString(R.string.sub_deleted, sub.nombre),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Long
            )
            if (resultado == SnackbarResult.ActionPerformed) viewModel.deshacerEliminacion(sub.id)
        }
    }

    val hayFilas = when (val s = uiState) {
        is SuscripcionesUiState.Success -> s.suscripciones.isNotEmpty() || s.categoriaSeleccionada != null
        is SuscripcionesUiState.Offline -> s.suscripciones.isNotEmpty()
        else -> false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (hayFilas) {
                FloatingActionButton(
                    onClick = { onNavigateToNueva(null) },
                    containerColor = Violet600,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = Color.White)
                }
            }
        },
        bottomBar = {
            // Sin suscripciones, sin anuncios: el primer uso no se interrumpe con un banner.
            if (hayFilas) BannerAdView(modifier = Modifier.fillMaxWidth())
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = (uiState as? SuscripcionesUiState.Success)?.isRefreshing == true,
            onRefresh = { viewModel.cargar() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when (val state = uiState) {
                is SuscripcionesUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is SuscripcionesUiState.Success -> {
                    if (state.suscripciones.isEmpty() && state.categoriaSeleccionada == null) {
                        EmptyStateSuscripciones(onAnadir = onNavigateToNueva, onDetectGmail = onDetectGmail)
                    } else {
                        ListaSuscripciones(
                            suscripciones = state.suscripciones,
                            categorias = state.categorias,
                            categoriaSeleccionada = state.categoriaSeleccionada,
                            onNavigateToDetalle = onNavigateToDetalle,
                            onFiltrar = { viewModel.filtrarPorCategoria(it) },
                            onEliminar = eliminarConDeshacer
                        )
                    }
                }
                is SuscripcionesUiState.Offline -> Column {
                    BannerOffline(stringResource(R.string.offline_data))
                    ListaSuscripciones(
                        suscripciones = state.suscripciones,
                        categorias = state.categorias,
                        categoriaSeleccionada = null,
                        onNavigateToDetalle = onNavigateToDetalle,
                        onFiltrar = {},
                        onEliminar = null // sin red no se puede borrar: el gesto se desactiva
                    )
                }
                is SuscripcionesUiState.Error -> ErrorState(
                    mensaje = textoErrorSuscripciones(state.error),
                    onRetry = { viewModel.cargar() }
                )
                is SuscripcionesUiState.SesionExpirada -> LaunchedEffect(Unit) { onSesionExpirada() }
            }
        }
    }
}

/**
 * Estado vacío: en vez de "no hay nada", una invitación concreta. Cada logo abre el
 * formulario con el servicio ya rellenado desde el catálogo.
 */
@Composable
private fun EmptyStateSuscripciones(onAnadir: (String?) -> Unit, onDetectGmail: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.empty_subs_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_subs_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            serviciosSugeridos.forEach { (nombre, dominio) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) { onAnadir(nombre) }
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        shadowElevation = 1.dp
                    ) {
                        Box(Modifier.padding(8.dp)) {
                            ServiceLogo(nombre = nombre, domain = dominio, size = 48.dp, contentDescription = null)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(nombre, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { onAnadir(null) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.empty_add_other), fontWeight = FontWeight.SemiBold)
        }
        if (Funciones.GMAIL) {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onDetectGmail, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.detect_with_gmail))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ListaSuscripciones(
    suscripciones: List<Subscription>,
    categorias: List<Category>,
    categoriaSeleccionada: Long?,
    onNavigateToDetalle: (Long) -> Unit,
    onFiltrar: (Long?) -> Unit,
    onEliminar: ((Subscription) -> Unit)?
) {
    // La TopAppBar ya dice "Suscripciones": en vez de repetir el título con un badge, un
    // subtítulo con significado: "7 activas · 47,96 €/mes" (S-05). Es el "Gasto mensual medio"
    // del Inicio: suma de equivalentes mensuales (importe / intervalo; semanal × 52 / 12) por
    // divisa. Se suma en diezmilésimas y solo el total se redondea a céntimos (regla de
    // Periodicidad): sumar Doubles ya redondeados podía desviarse un céntimo del Inicio.
    // Sin los terminados (fecha de fin pasada): ya no cuentan, como en el Inicio.
    val hoy = rememberHoy()
    val activas = suscripciones.filter { it.cuentaEnTotales(hoy) }
    // Los terminados van al final (orden estable: el resto conserva el suyo) y atenuados: siguen
    // en la lista como historial, pero no compiten con los gastos vivos.
    val ordenadas = remember(suscripciones, hoy) { suscripciones.sortedBy { it.terminado(hoy) } }
    val totalesMensuales = activas
        .groupBy { it.moneda }
        .mapValues { (_, subs) -> Periodicidad.aCifraVisible(subs.sumOf { it.equivalenteMensualInterno() }) }
        .entries
        .sortedBy { entry -> when (entry.key) { "EUR" -> "0"; "USD" -> "1"; else -> "2${entry.key}" } }
    val resumenActivas = pluralStringResource(R.plurals.subs_active_count, activas.size, activas.size)
    val resumenLista = if (totalesMensuales.isEmpty()) resumenActivas else {
        val totalTexto = totalesMensuales.joinToString(" + ") { formatearImporte(it.value, it.key) }
        resumenActivas + " · " + stringResource(R.string.subs_monthly_total, totalTexto)
    }

    // En horizontal la lista es ancha y baja: el "+" flotante caía encima del importe de la
    // primera fila (no solo de la última, que ya cubre el hueco inferior). Se le reserva su propia
    // calle a la derecha de las filas: 56 dp del botón + 16 dp de su margen. Los chips y el
    // resumen siguen a todo el ancho (quedan arriba, lejos del botón).
    val horizontal = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val margenFilas = Modifier.padding(start = 16.dp, end = if (horizontal) 16.dp + CalleFab else 16.dp)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = resumenLista,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )
        }

        stickyHeader {
            // Mismo color que el fondo de la página: con `surface` se veía una franja blanca
            // entre el resumen y la lista. Sigue tapando las filas al quedar fija arriba.
            Surface(color = MaterialTheme.colorScheme.background) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = categoriaSeleccionada == null,
                            onClick = { onFiltrar(null) },
                            label = { Text(stringResource(R.string.all_filter)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Violet600,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(categorias) { cat ->
                        FilterChip(
                            selected = categoriaSeleccionada == cat.id,
                            onClick = {
                                if (categoriaSeleccionada == cat.id) onFiltrar(null)
                                else onFiltrar(cat.id)
                            },
                            label = { Text(cat.nombre) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Violet600,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        if (suscripciones.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_subscriptions_in_category),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(ordenadas, key = { it.id }) { sub ->
                val fila: @Composable () -> Unit = {
                    SuscripcionCard(sub, hoy, onNavigateToDetalle)
                }
                if (onEliminar == null) {
                    Box(margenFilas.animateItem()) { fila() }
                } else {
                    FilaDeslizable(
                        onEliminar = { onEliminar(sub) },
                        modifier = margenFilas.animateItem(),
                        content = fila
                    )
                }
            }
        }
    }
}

/**
 * Deslizar hacia la izquierda elimina. El fondo rojo muestra la papelera, que crece al cruzar
 * el umbral (40 % del ancho) con un toque háptico: el gesto "responde" antes de soltar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilaDeslizable(
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val estado = rememberSwipeToDismissBoxState(
        confirmValueChange = { valor ->
            if (valor == SwipeToDismissBoxValue.EndToStart) {
                onEliminar()
                true
            } else false
        },
        positionalThreshold = { ancho -> ancho * 0.4f }
    )
    val pasadoUmbral = estado.targetValue == SwipeToDismissBoxValue.EndToStart
    LaunchedEffect(pasadoUmbral) {
        if (pasadoUmbral) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val escalaIcono by animateFloatAsState(if (pasadoUmbral) 1.15f else 0.9f, label = "papelera")
    val forma = RoundedCornerShape(14.dp)

    SwipeToDismissBox(
        state = estado,
        modifier = modifier.clip(forma),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(forma)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.scale(escalaIcono)
                )
            }
        },
        content = { content() }
    )
}

/** Desde esta escala de fuente la cifra mensual de la tarjeta va bajo el nombre (ver [SuscripcionCard]). */
private const val ESCALA_FUENTE_TARJETA_APILADA = 1.3f

/**
 * "Hoy" del móvil, recalculado cada vez que la pantalla vuelve a primer plano: con un `remember`
 * sin clave se quedaba en el día en que se abrió si la app seguía abierta al pasar la medianoche.
 * shortcut: no avanza a medianoche con la pantalla delante; si hiciera falta, un ticker diario.
 */
@Composable
internal fun rememberHoy(): LocalDate {
    var hoy by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    LifecycleResumeEffect(Unit) {
        hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
        onPauseOrDispose {}
    }
    return hoy
}

/** Opacidad de las tarjetas de gastos terminados (historial, no compiten con los vivos). */
private const val ALFA_TERMINADO = 0.6f

/** Calle derecha de la lista en horizontal para que el botón "+" no tape filas. */
private val CalleFab = 72.dp

@Composable
private fun SuscripcionCard(
    sub: Subscription,
    hoy: LocalDate,
    onNavigateToDetalle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Días restantes de prueba, si procede. Una fecha nula o mal formada (p. ej. creada desde
    // la web) no debe tirar la lista.
    val fechaFinPrueba = sub.fechaFinPrueba
    // Una prueba ya vencida no lleva insignia: antes salía "Prueba · -12d".
    val diasPrueba: Int? = if (sub.esPrueba && !fechaFinPrueba.isNullOrBlank()) {
        runCatching {
            val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            hoy.daysUntil(LocalDate.parse(fechaFinPrueba.trim()))
        }.getOrNull()?.takeIf { it >= 0 }
    } else null

    // A la derecha, siempre lo que cuesta al mes ("30,00 €/mes"), para comparar filas de un
    // vistazo. Debajo del nombre, lo que se paga de verdad en cada cobro ("90,00 € cada 3 meses"),
    // salvo en las mensuales: ahí sería la misma cifra dos veces.
    val periodo = sub.periodoCobro()
    val mensualTexto = stringResource(
        R.string.subs_monthly_total,
        formatearImporte(Periodicidad.aCifraVisible(sub.equivalenteMensualInterno()), sub.moneda)
    )
    // Plazo (préstamos): terminado → la etiqueta "Terminado" en lugar de la renovación, que ya no
    // llega; con cuotas pendientes, "Quedan N cuotas" en una línea corta.
    val (terminado, cuotasRestantes) = remember(sub, hoy) {
        val fin = sub.terminado(hoy)
        fin to (if (fin) null else plazoGasto(sub, hoy)?.cuotasRestantes)
    }
    val renueva = if (terminado) stringResource(R.string.plazo_terminado)
    else stringResource(R.string.renews_on, fechaIsoCorta(proximaRenovacionIso(sub)))
    val detalleCobro = if (periodo.esMensual) renueva
    else importeCadaPeriodo(sub.precio, sub.moneda, periodo) + " · " + renueva

    // Con fuente grande la cifra de la derecha se comía media fila: nombre y detalle se partían
    // en tres o cuatro líneas y cada tarjeta medía el doble. Ahí la cifra baja bajo el nombre y
    // el texto usa todo el ancho; el logo y el relleno encogen un poco.
    val fuenteGrande = LocalDensity.current.fontScale >= ESCALA_FUENTE_TARJETA_APILADA
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .clickable { onNavigateToDetalle(sub.id) }
            .alpha(if (terminado) ALFA_TERMINADO else 1f)
            .padding(horizontal = 14.dp, vertical = if (fuenteGrande) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ServiceLogo(nombre = sub.nombre, size = if (fuenteGrande) 40.dp else 44.dp, contentDescription = null)
        Spacer(Modifier.width(if (fuenteGrande) 12.dp else 14.dp))
        Column(Modifier.weight(1f)) {
            // Dos líneas: con fuente grande la cifra de la derecha deja poco ancho y el nombre
            // se quedaba en "Gimnasi…".
            Text(sub.nombre, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (fuenteGrande) {
                Text(mensualTexto, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                detalleCobro,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (cuotasRestantes != null) {
                Text(
                    text = pluralStringResource(R.plurals.plazo_quedan_corto, cuotasRestantes, cuotasRestantes),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (diasPrueba != null) {
                // Texto en la columna, no un badge flotante sobre el precio (S-02).
                Text(
                    text = stringResource(R.string.trial_badge, diasPrueba),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (diasPrueba <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.urgent
                )
            }
        }
        if (!fuenteGrande) {
            Spacer(Modifier.width(8.dp))
            Text(
                mensualTexto,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
