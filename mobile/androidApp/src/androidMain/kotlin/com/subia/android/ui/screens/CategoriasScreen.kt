package com.subia.android.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subia.shared.model.Category
import com.subia.shared.viewmodel.CategoriasUiState
import com.subia.shared.viewmodel.CategoriasViewModel
import com.subia.shared.viewmodel.CrearCategoriaUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoriasScreen(viewModel: CategoriasViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val crearState by viewModel.crearState.collectAsState()
    var mostrarFormulario by remember { mutableStateOf(false) }
    var nombreNueva by remember { mutableStateOf("") }
    val crearDeshabilitado = uiState is CategoriasUiState.Offline

    LaunchedEffect(crearState) {
        if (crearState is CrearCategoriaUiState.Success) {
            mostrarFormulario = false
            nombreNueva = ""
            viewModel.resetCrearState()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!crearDeshabilitado) mostrarFormulario = true },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva categoría", tint = Color.White)
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is CategoriasUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            is CategoriasUiState.Success -> CategoriasList(
                state.categorias, Modifier.padding(innerPadding)
            )
            is CategoriasUiState.Offline -> Column(Modifier.padding(innerPadding)) {
                BannerOffline("Sin conexión — no es posible crear categorías")
                CategoriasList(state.categorias, Modifier)
            }
            is CategoriasUiState.Error -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.cargarCategorias() }) { Text("Reintentar") }
                }
            }
            is CategoriasUiState.SesionExpirada -> Unit
        }
    }

    if (mostrarFormulario) {
        AlertDialog(
            onDismissRequest = { mostrarFormulario = false },
            title = { Text("Nueva categoría", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreNueva,
                        onValueChange = { nombreNueva = it },
                        label = { Text("Nombre *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (crearState is CrearCategoriaUiState.Error) {
                        Spacer(Modifier.height(4.dp))
                        Text((crearState as CrearCategoriaUiState.Error).mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.crearCategoria(nombreNueva) }, enabled = crearState !is CrearCategoriaUiState.Loading) {
                    if (crearState is CrearCategoriaUiState.Loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarFormulario = false; nombreNueva = "" }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CategoriasList(categorias: List<Category>, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Categorías", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${categorias.size} categorías", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
        }
        items(categorias, key = { it.id }) { cat ->
            CategoriaCard(cat)
        }
    }
}

@Composable
private fun CategoriaCard(cat: Category) {
    val colorStr = cat.color.trimStart('#')
    val colorInt = try { android.graphics.Color.parseColor("#$colorStr") } catch (e: Exception) { android.graphics.Color.GRAY }
    val color = Color(colorInt)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Circle, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(cat.nombre, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                if (cat.icon.isNotEmpty()) {
                    Text(cat.icon, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
