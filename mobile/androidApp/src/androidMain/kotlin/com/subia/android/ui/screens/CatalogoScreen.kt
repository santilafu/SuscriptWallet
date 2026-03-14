package com.subia.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.ui.ServiceLogo
import com.subia.shared.model.CatalogItem
import com.subia.shared.viewmodel.CatalogoUiState
import com.subia.shared.viewmodel.CatalogoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CatalogoScreen(
    onSeleccionarItem: (CatalogItem) -> Unit,
    viewModel: CatalogoViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val itemsFiltrados by viewModel.itemsFiltrados.collectAsState()
    val busqueda by viewModel.busqueda.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Catálogo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${itemsFiltrados.size} servicios", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = busqueda,
            onValueChange = { viewModel.busqueda.value = it },
            placeholder = { Text("Buscar servicio...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(12.dp))

        when (val state = uiState) {
            is CatalogoUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is CatalogoUiState.Offline -> {
                BannerOffline("Catálogo guardado — sin conexión")
                CatalogoGrid(itemsFiltrados, onSeleccionarItem)
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

@Composable
private fun CatalogoGrid(items: List<CatalogItem>, onSeleccionar: (CatalogItem) -> Unit) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Sin resultados", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
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
        modifier = Modifier.clickable { onSeleccionar(item) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ServiceLogo(nombre = item.nombre, size = 44.dp)
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
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
