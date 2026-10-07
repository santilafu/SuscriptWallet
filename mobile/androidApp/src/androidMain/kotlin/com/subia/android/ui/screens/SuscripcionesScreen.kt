package com.subia.android.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.subia.android.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.ui.BannerAdView
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.components.EmptyState
import com.subia.android.ui.components.ErrorState
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.theme.Indigo500
import com.subia.android.ui.theme.urgent
import com.subia.shared.model.Category
import com.subia.shared.model.Subscription
import com.subia.shared.viewmodel.SuscripcionesUiState
import com.subia.shared.viewmodel.SuscripcionesViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionesScreen(
    onNavigateToDetalle: (Long) -> Unit,
    onNavigateToNueva: () -> Unit,
    onSesionExpirada: () -> Unit = {},
    viewModel: SuscripcionesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.invalidarCacheYRecargar()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNueva,
                containerColor = Indigo500,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = Color.White)
            }
        },
        bottomBar = {
            BannerAdView(modifier = Modifier.fillMaxWidth())
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
                    if (state.suscripciones.isEmpty() && state.categoriaSeleccionada == null) {
                        EmptyStateSuscripciones(onNavigateToNueva)
                    } else {
                        ListaSuscripciones(
                            suscripciones = state.suscripciones,
                            categorias = state.categorias,
                            categoriaSeleccionada = state.categoriaSeleccionada,
                            onNavigateToDetalle = onNavigateToDetalle,
                            onFiltrar = { viewModel.filtrarPorCategoria(it) }
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
                        onFiltrar = {}
                    )
                }
                is SuscripcionesUiState.Error -> ErrorState(
                    mensaje = state.mensaje,
                    onRetry = { viewModel.cargar() }
                )
                is SuscripcionesUiState.SesionExpirada -> LaunchedEffect(Unit) { onSesionExpirada() }
            }
        }
    }
}

@Composable
private fun EmptyStateSuscripciones(onNavigateToNueva: () -> Unit) {
    EmptyState(
        icon = Icons.Default.Subscriptions,
        titulo = stringResource(R.string.no_subscriptions),
        subtitulo = stringResource(R.string.tap_to_add_first),
        actionLabel = stringResource(R.string.add_first_subscription),
        onAction = onNavigateToNueva
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListaSuscripciones(
    suscripciones: List<Subscription>,
    categorias: List<Category>,
    categoriaSeleccionada: Long?,
    onNavigateToDetalle: (Long) -> Unit,
    onFiltrar: (Long?) -> Unit
) {
    // La TopAppBar ya dice "Suscripciones": en vez de repetir el título con un badge, un
    // subtítulo con significado: "7 activas · 47,96 €/mes" (S-05). El gasto mensual se
    // normaliza igual que en DashboardViewModel (anual / 12) y se agrupa por divisa.
    val activas = suscripciones.filter { it.activa }
    val totalesMensuales = activas
        .groupBy { it.moneda }
        .mapValues { (_, subs) -> subs.sumOf { if (it.periodoFacturacion == "YEARLY") it.precio / 12.0 else it.precio } }
        .entries
        .sortedBy { entry -> when (entry.key) { "EUR" -> "0"; "USD" -> "1"; else -> "2${entry.key}" } }
    val resumenActivas = pluralStringResource(R.plurals.subs_active_count, activas.size, activas.size)
    val resumenLista = if (totalesMensuales.isEmpty()) resumenActivas else {
        val totalTexto = totalesMensuales.joinToString(" + ") { formatearImporte(it.value, it.key) }
        resumenActivas + " · " + stringResource(R.string.subs_monthly_total, totalTexto)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = resumenLista,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            )
        }

        stickyHeader {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp
            ) {
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
                                selectedContainerColor = Indigo500,
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
                                selectedContainerColor = Indigo500,
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.no_subscriptions_in_category),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(suscripciones) { sub ->
                SuscripcionCard(sub, onNavigateToDetalle, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun SuscripcionCard(sub: Subscription, onNavigateToDetalle: (Long) -> Unit, modifier: Modifier = Modifier) {
    // Calcula días restantes de prueba si procede
    // Una fecha nula o mal formada (p. ej. creada desde la web) no debe tirar la lista.
    val fechaFinPrueba = sub.fechaFinPrueba
    val diasPrueba: Int? = if (sub.esPrueba && !fechaFinPrueba.isNullOrBlank()) {
        runCatching {
            val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            hoy.daysUntil(LocalDate.parse(fechaFinPrueba))
        }.getOrNull()
    } else null

    // Borde izquierdo acento + contenedor de tarjeta
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .clickable { onNavigateToDetalle(sub.id) }
    ) {
        // Acento izquierdo discreto (outlineVariant, sin gradiente) — crece con el alto de la tarjeta
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceLogo(nombre = sub.nombre, size = 44.dp, contentDescription = null)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(sub.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        periodoCiclo(sub.periodoFacturacion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.renews_on, sub.fechaRenovacion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("%.2f €".format(sub.precio), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Badge de prueba gratuita
            if (diasPrueba != null) {
                val badgeColor = if (diasPrueba <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.urgent
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeColor)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.trial_badge, diasPrueba),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun periodoCiclo(ciclo: String): String = when (ciclo) {
    "MONTHLY" -> stringResource(R.string.monthly)
    "YEARLY" -> stringResource(R.string.yearly)
    "WEEKLY" -> stringResource(R.string.weekly)
    else -> ciclo
}
