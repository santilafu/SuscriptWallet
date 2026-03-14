package com.subia.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subia.shared.model.CatalogItem
import com.subia.shared.viewmodel.CatalogoUiState
import com.subia.shared.viewmodel.CatalogoViewModel
import org.koin.androidx.compose.koinViewModel

/** Pantalla del catálogo de servicios con búsqueda client-side y selección para prerellenar formulario. */
@Composable
fun CatalogoScreen(
    onSeleccionarItem: (CatalogItem) -> Unit,
    viewModel: CatalogoViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val itemsFiltrados by viewModel.itemsFiltrados.collectAsState()
    val busqueda by viewModel.busqueda.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Catálogo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = busqueda,
            onValueChange = { viewModel.busqueda.value = it },
            label = { Text("Buscar servicio") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        when (val state = uiState) {
            is CatalogoUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            is CatalogoUiState.Offline -> {
                BannerOffline("Mostrando catálogo guardado — sin conexión")
                ListaCatalogo(itemsFiltrados, onSeleccionarItem)
            }
            is CatalogoUiState.Success -> ListaCatalogo(itemsFiltrados, onSeleccionarItem)
            is CatalogoUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.cargarCatalogo() }) { Text("Reintentar") }
                }
            }
            is CatalogoUiState.SesionExpirada -> Unit
        }
    }
}

@Composable
private fun ListaCatalogo(
    items: List<CatalogItem>,
    onSeleccionar: (CatalogItem) -> Unit
) {
    if (items.isEmpty()) {
        Text("No se han encontrado servicios", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onSeleccionar(item) }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(item.nombre, fontWeight = FontWeight.SemiBold)
                    item.precioMensual?.let {
                        Text("${it} ${item.moneda}/mes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
