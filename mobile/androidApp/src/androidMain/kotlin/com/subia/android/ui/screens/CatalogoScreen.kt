package com.subia.android.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.theme.GradientIndigoEnd
import com.subia.android.ui.theme.GradientIndigoStart
import com.subia.android.ui.theme.Indigo400
import com.subia.shared.model.CatalogItem
import com.subia.shared.viewmodel.CatalogoUiState
import com.subia.shared.viewmodel.CatalogoViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Mapeo de clave de categoría → nombre legible. */
private fun nombreCategoria(key: String): String = when (key) {
    "ia"          -> "IA"
    "streaming"   -> "Streaming"
    "musica"      -> "Música"
    "software"    -> "Software"
    "cloud"       -> "Cloud"
    "gaming"      -> "Gaming"
    "seguridad"   -> "Seguridad"
    "noticias"    -> "Noticias"
    "salud"       -> "Salud"
    "desarrollo"  -> "Desarrollo"
    "prueba"      -> "Pruebas"
    "finanzas"    -> "Finanzas"
    "educacion"   -> "Educación"
    "creatividad" -> "Creatividad"
    "citas"       -> "Social"
    else          -> key.replaceFirstChar { it.uppercaseChar() }
}

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    onSeleccionarItem: (CatalogItem) -> Unit,
    viewModel: CatalogoViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val itemsFiltrados by viewModel.itemsFiltrados.collectAsState()
    val busqueda by viewModel.busqueda.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val categoriaFiltro by viewModel.categoriaFiltro.collectAsState()

    val gradientBrush = Brush.linearGradient(
        colors = listOf(GradientIndigoStart, GradientIndigoEnd, Color(0xFFA78BFA)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(20.dp))

        // Cabecera
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(brush = gradientBrush, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp)) {
                    append("Catálogo")
                }
            }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${if (itemsFiltrados.isEmpty() && uiState is CatalogoUiState.Loading) "320+" else itemsFiltrados.size} servicios disponibles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(16.dp))

        // Campo de búsqueda
        TextField(
            value = busqueda,
            onValueChange = { viewModel.busqueda.value = it },
            placeholder = { Text("Buscar servicio...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Indigo400) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLeadingIconColor = Indigo400,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Chips de categoría (solo cuando hay datos)
        if (categorias.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chip "Todos"
                FilterChip(
                    selected = categoriaFiltro == null,
                    onClick = { viewModel.categoriaFiltro.value = null },
                    label = { Text("Todos", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Indigo400,
                        selectedLabelColor = Color.White
                    )
                )
                // Chips por categoría
                categorias.forEach { key ->
                    FilterChip(
                        selected = categoriaFiltro == key,
                        onClick = {
                            viewModel.categoriaFiltro.value = if (categoriaFiltro == key) null else key
                        },
                        label = { Text(nombreCategoria(key), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Indigo400,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (val state = uiState) {
                is CatalogoUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is CatalogoUiState.Offline -> {
                    Column(Modifier.fillMaxSize()) {
                        BannerOffline("Catálogo guardado — sin conexión")
                        CatalogoGrid(itemsFiltrados, onSeleccionarItem)
                    }
                }
                is CatalogoUiState.Success -> CatalogoGrid(itemsFiltrados, onSeleccionarItem)
                is CatalogoUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.cargarCatalogo() }) { Text("Reintentar") }
                    }
                }
                is CatalogoUiState.SesionExpirada -> Unit
            }
        }
    }
}

@Composable
private fun CatalogoGrid(items: List<CatalogItem>, onSeleccionar: (CatalogItem) -> Unit) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Sin resultados", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { item ->
            CatalogoItemCard(item, onSeleccionar)
        }
    }
}

@Composable
private fun CatalogoItemCard(item: CatalogItem, onSeleccionar: (CatalogItem) -> Unit) {
    Card(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .clickable { onSeleccionar(item) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ServiceLogo(nombre = item.nombre, size = 44.dp, domain = item.domain)
            Text(
                item.nombre,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            item.precioMensual?.let {
                Text(
                    "%.2f €/m".format(it),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo400,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
