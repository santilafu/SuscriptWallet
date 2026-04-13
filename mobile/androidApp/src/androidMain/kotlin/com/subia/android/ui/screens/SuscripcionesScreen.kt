package com.subia.android.ui.screens

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.ui.BannerAdView
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.theme.GradientIndigoEnd
import com.subia.android.ui.theme.GradientIndigoStart
import com.subia.android.ui.theme.Indigo400
import com.subia.android.ui.theme.Indigo500
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
                Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White)
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
                        EmptyStateSuscripciones()
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
                    BannerOffline("Mostrando datos guardados — sin conexión")
                    ListaSuscripciones(
                        suscripciones = state.suscripciones,
                        categorias = state.categorias,
                        categoriaSeleccionada = null,
                        onNavigateToDetalle = onNavigateToDetalle,
                        onFiltrar = {}
                    )
                }
                is SuscripcionesUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.cargar() }) { Text("Reintentar") }
                    }
                }
                is SuscripcionesUiState.SesionExpirada -> LaunchedEffect(Unit) { onSesionExpirada() }
            }
        }
    }
}

@Composable
private fun EmptyStateSuscripciones() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Subscriptions,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Sin suscripciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Toca + para añadir la primera",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalTextApi::class)
@Composable
private fun ListaSuscripciones(
    suscripciones: List<Subscription>,
    categorias: List<Category>,
    categoriaSeleccionada: Long?,
    onNavigateToDetalle: (Long) -> Unit,
    onFiltrar: (Long?) -> Unit
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(GradientIndigoStart, GradientIndigoEnd, Color(0xFFA78BFA)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                brush = gradientBrush,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp
                            )
                        ) {
                            append("Mis suscripciones")
                        }
                    }
                )
                // Badge con el conteo
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Indigo500)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${suscripciones.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
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
                            label = { Text("Todas") },
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
                            "No hay suscripciones en esta categoría",
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
    val diasPrueba: Int? = if (sub.esPrueba && !sub.fechaFinPrueba.isNullOrBlank()) {
        runCatching {
            val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            hoy.daysUntil(LocalDate.parse(sub.fechaFinPrueba!!))
        }.getOrNull()
    } else null

    // Borde izquierdo acento + contenedor de tarjeta
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .clickable { onNavigateToDetalle(sub.id) }
    ) {
        // Acento izquierdo indigo
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(80.dp)
                .background(
                    brush = Brush.verticalGradient(listOf(GradientIndigoStart, GradientIndigoEnd)),
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
                    Text("%.2f €".format(sub.precio), fontWeight = FontWeight.Bold, color = Indigo400)
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Badge de prueba gratuita
            if (diasPrueba != null) {
                val badgeColor = if (diasPrueba <= 3) MaterialTheme.colorScheme.error else Color(0xFFF97316)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeColor)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Prueba · ${diasPrueba}d",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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
