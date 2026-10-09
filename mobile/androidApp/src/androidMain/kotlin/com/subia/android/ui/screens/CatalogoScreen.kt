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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.stringResource
import com.subia.android.R
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.components.ErrorState
import com.subia.android.ui.components.textoErrorRemoto
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.components.formatearImporteCorto
import com.subia.android.ui.components.sufijoPeriodo
import com.subia.android.ui.theme.success
import com.subia.shared.model.CatalogItem
import com.subia.shared.viewmodel.CatalogoUiState
import com.subia.shared.viewmodel.CatalogoViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Mapeo de clave de categoría → nombre legible. */
@Composable
private fun nombreCategoria(key: String): String = when (key) {
    "ia"          -> stringResource(R.string.cat_ia)
    "streaming"   -> stringResource(R.string.cat_streaming)
    "musica"      -> stringResource(R.string.cat_musica)
    "software"    -> stringResource(R.string.cat_software)
    "cloud"       -> stringResource(R.string.cat_cloud)
    "gaming"      -> stringResource(R.string.cat_gaming)
    "seguridad"   -> stringResource(R.string.cat_seguridad)
    "noticias"    -> stringResource(R.string.cat_noticias)
    "salud"       -> stringResource(R.string.cat_salud)
    "desarrollo"  -> stringResource(R.string.cat_desarrollo)
    "prueba"      -> stringResource(R.string.cat_prueba)
    "finanzas"    -> stringResource(R.string.cat_finanzas)
    "educacion"   -> stringResource(R.string.cat_educacion)
    "creatividad" -> stringResource(R.string.cat_creatividad)
    "citas"       -> stringResource(R.string.cat_citas)
    "hogar"       -> stringResource(R.string.cat_hogar)
    "seguros"     -> stringResource(R.string.cat_seguros)
    "telecos"     -> stringResource(R.string.cat_telecos)
    "transporte"  -> stringResource(R.string.cat_transporte)
    else          -> key.replaceFirstChar { it.uppercaseChar() }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))

        // La TopAppBar ya dice "Catálogo": aquí solo el contador de servicios como subtítulo.
        Text(
            text = if (itemsFiltrados.isEmpty() && uiState is CatalogoUiState.Loading)
                stringResource(R.string.services_available_loading)
            else
                stringResource(R.string.services_available, itemsFiltrados.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        // Campo de búsqueda
        TextField(
            value = busqueda,
            onValueChange = { viewModel.busqueda.value = it },
            placeholder = { Text(stringResource(R.string.search_service)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
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
                    label = { Text(stringResource(R.string.all_categories), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
                        BannerOffline(stringResource(R.string.offline_catalog))
                        CatalogoGrid(itemsFiltrados, onSeleccionarItem)
                    }
                }
                is CatalogoUiState.Success -> CatalogoGrid(itemsFiltrados, onSeleccionarItem)
                is CatalogoUiState.Error -> ErrorState(
                    mensaje = textoErrorRemoto(state.error, R.string.error_load_catalog),
                    onRetry = { viewModel.cargarCatalogo() }
                )
                is CatalogoUiState.SesionExpirada -> Unit
            }
        }
    }
}

@Composable
private fun CatalogoGrid(items: List<CatalogItem>, onSeleccionar: (CatalogItem) -> Unit) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(stringResource(R.string.no_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        // Rejilla adaptable: ~3 columnas en teléfonos normales pero tarjetas más grandes
        // y legibles, y más columnas en pantallas anchas, sin texto minúsculo.
        columns = GridCells.Adaptive(minSize = 104.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { item ->
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
            ServiceLogo(nombre = item.nombre, size = 44.dp, domain = item.domain, iconUrl = item.iconUrl, contentDescription = null)
            Text(
                item.nombre,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                // Siempre dos líneas de alto: si no, las tarjetas de una misma fila medían
                // distinto según el nombre ("AdGuard Premium" vs "1Password").
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            item.precioMensual?.let {
                // Recibos de importe variable (luz, teléfono, seguro): "≈" y la etiqueta debajo,
                // para que nadie lea 70 € como la tarifa exacta. `price` es el precio del ciclo
                // del servicio: un seguro anual es "450 €/año", no "/mes".
                PrecioCatalogo(
                    importe = formatearImporteCorto(it, item.moneda),
                    periodo = "/" + sufijoPeriodo(item.periodoFacturacion),
                    aproximado = item.variablePrice
                )
                if (item.variablePrice) {
                    Text(
                        stringResource(R.string.price_approx_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
            item.annualSavingsPercent()?.takeIf { it > 0 }?.let { pct ->
                Text(
                    stringResource(R.string.annual_savings, pct),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.success,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * "≈ 30 €/mes" en una sola línea: el importe manda y el periodo va más pequeño. Si aun así
 * no cabe en la tarjeta (importes largos, fuentes grandes) se reduce el tamaño en vez de partir
 * la línea por la barra o cortar el texto.
 */
@Composable
private fun PrecioCatalogo(importe: String, periodo: String, aproximado: Boolean) {
    val base = MaterialTheme.typography.titleSmall
    var escala by remember(importe, periodo) { mutableFloatStateOf(1f) }
    val texto = buildAnnotatedString {
        if (aproximado) append("≈ ")
        append(importe)
        withStyle(SpanStyle(fontSize = base.fontSize * 0.78f, fontWeight = FontWeight.SemiBold)) { append(periodo) }
    }
    Text(
        texto,
        style = base.copy(fontSize = base.fontSize * escala),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { if (it.hasVisualOverflow && escala > 0.7f) escala -= 0.08f }
    )
}
