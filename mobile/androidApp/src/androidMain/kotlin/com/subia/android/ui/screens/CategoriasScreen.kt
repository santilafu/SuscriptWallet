package com.subia.android.ui.screens

import androidx.compose.ui.unit.sp
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subia.android.R
import com.subia.android.ui.components.ErrorState
import com.subia.android.ui.components.textoErrorCrearCategoria
import com.subia.android.ui.components.textoErrorRemoto
import com.subia.android.ui.theme.Violet600
import com.subia.shared.model.Category
import com.subia.shared.viewmodel.CategoriasUiState
import com.subia.shared.viewmodel.CategoriasViewModel
import com.subia.shared.viewmodel.CrearCategoriaUiState
import org.koin.compose.viewmodel.koinViewModel

/**
 * Lista de categorías del usuario con alta rápida. Ya no es una pestaña: se abre desde
 * Ajustes → Categorías, por eso lleva su propia TopAppBar con flecha de vuelta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    onBack: () -> Unit,
    viewModel: CategoriasViewModel = koinViewModel()
) {
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!crearDeshabilitado) mostrarFormulario = true },
                containerColor = Violet600,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_category), tint = Color.White)
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
                BannerOffline(stringResource(R.string.offline_no_create))
                CategoriasList(state.categorias, Modifier)
            }
            // Mismo componente de error que el resto de pantallas (C-02).
            is CategoriasUiState.Error -> ErrorState(
                mensaje = textoErrorRemoto(state.error, R.string.error_load_categories),
                onRetry = { viewModel.cargarCategorias() },
                modifier = Modifier.padding(innerPadding)
            )
            is CategoriasUiState.SesionExpirada -> Unit
        }
    }

    if (mostrarFormulario) {
        AlertDialog(
            onDismissRequest = { mostrarFormulario = false },
            title = { Text(stringResource(R.string.new_category), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreNueva,
                        onValueChange = { nombreNueva = it },
                        label = { Text(stringResource(R.string.name_required)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (crearState is CrearCategoriaUiState.Error) {
                        Spacer(Modifier.height(4.dp))
                        Text(textoErrorCrearCategoria((crearState as CrearCategoriaUiState.Error).error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.crearCategoria(nombreNueva) }, enabled = crearState !is CrearCategoriaUiState.Loading) {
                    if (crearState is CrearCategoriaUiState.Loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarFormulario = false; nombreNueva = "" }) { Text(stringResource(R.string.cancel)) }
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
            // La TopAppBar ya dice "Categorías": aquí solo un subtítulo informativo.
            Text(
                text = stringResource(R.string.organize_expenses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
        }
        items(categorias) { cat ->
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Barra de acento izquierda con el color de la categoría
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(70.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    // El backend guarda un emoji por categoría (🏠, 🛡️, 📡, 🚌…): es su icono, no
                    // un subtítulo. Si viene vacío o es una clave de texto, punto del color.
                    if (esEmoji(cat.icon)) {
                        Text(cat.icon, fontSize = 20.sp)
                    } else {
                        Icon(Icons.Default.Circle, null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(cat.nombre, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** `true` si [texto] es un emoji corto (no una clave tipo "bi-house" ni texto normal). */
private fun esEmoji(texto: String): Boolean {
    val t = texto.trim()
    if (t.isEmpty() || t.length > 8) return false
    return t.codePoints().anyMatch { cp ->
        Character.getType(cp) == Character.OTHER_SYMBOL.toInt() || cp >= 0x1F000
    }
}
