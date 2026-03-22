package com.subia.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.subia.android.ui.theme.GradientIndigoEnd
import com.subia.android.ui.theme.GradientIndigoStart
import com.subia.android.ui.theme.Indigo400
import com.subia.android.ui.theme.Indigo500
import com.subia.shared.model.CatalogItem
import com.subia.shared.viewmodel.FormUiState
import kotlinx.serialization.json.Json
import com.subia.shared.viewmodel.SuscripcionFormViewModel
import com.subia.shared.viewmodel.CategoriasViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

/** Pantalla de formulario para crear o editar una suscripción. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionFormScreen(
    suscripcionId: Long? = null,
    onSuccess: () -> Unit,
    navController: NavController? = null,
    formViewModel: SuscripcionFormViewModel = koinViewModel(),
    categoriasViewModel: CategoriasViewModel = koinViewModel()
) {
    val uiState by formViewModel.uiState.collectAsState()
    val nombre by formViewModel.nombre.collectAsState()
    val descripcion by formViewModel.descripcion.collectAsState()
    val precio by formViewModel.precio.collectAsState()
    val moneda by formViewModel.moneda.collectAsState()
    val periodoFacturacion by formViewModel.periodoFacturacion.collectAsState()
    val fechaRenovacion by formViewModel.fechaRenovacion.collectAsState()
    val notas by formViewModel.notas.collectAsState()
    val categoriaId by formViewModel.categoriaId.collectAsState()
    val esPrueba by formViewModel.esPrueba.collectAsState()
    val fechaFinPrueba by formViewModel.fechaFinPrueba.collectAsState()

    val categorias by formViewModel.categorias.collectAsState()
    val serviciosPorCategoria by formViewModel.serviciosPorCategoria.collectAsState()
    val cargandoServicios by formViewModel.cargandoServicios.collectAsState()
    val categoriaSeleccionadaId by formViewModel.categoriaSeleccionadaId.collectAsState()

    val isLoading = uiState is FormUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is FormUiState.Success) onSuccess()
    }

    // Prerrellenado desde el catálogo via SavedStateHandle (T08)
    LaunchedEffect(Unit) {
        val itemJson = navController?.previousBackStackEntry
            ?.savedStateHandle
            ?.get<String>("catalog_item_json")
        itemJson?.let { json ->
            runCatching { Json.decodeFromString<CatalogItem>(json) }.getOrNull()?.let { item ->
                formViewModel.prerellenarDesdeCatalogo(item)
            }
            navController.previousBackStackEntry?.savedStateHandle?.remove<String>("catalog_item_json")
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var showDatePickerPrueba by remember { mutableStateOf(false) }
    val datePickerStatePrueba = rememberDatePickerState()

    var expandedMoneda by remember { mutableStateOf(false) }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var expandedCategoriaForm by remember { mutableStateOf(false) }
    val monedasOpciones = listOf("EUR", "USD", "GBP")
    val periodosOpciones = listOf("MONTHLY" to "Mensual", "YEARLY" to "Anual", "WEEKLY" to "Semanal")

    var selectorExpandido by remember { mutableStateOf(false) }
    var expandedCategoriaSel by remember { mutableStateOf(false) }
    var expandedServicioSel by remember { mutableStateOf(false) }
    var servicioSeleccionado by remember { mutableStateOf<CatalogItem?>(null) }

    val accentGradient = Brush.verticalGradient(
        listOf(GradientIndigoStart, GradientIndigoEnd)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (suscripcionId != null) "Editar suscripción" else "Nueva suscripción") },
                navigationIcon = {
                    IconButton(onClick = onSuccess) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Selector de catálogo en línea ──────────────────────────────
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Borde izquierdo con gradiente indigo
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(if (selectorExpandido) 60.dp else 56.dp)
                            .background(
                                brush = accentGradient,
                                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            )
                    )
                    // Cabecera plegable
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectorExpandido = !selectorExpandido }
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Indigo400
                            )
                            Column {
                                Text(
                                    text = "Elegir del catálogo",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Indigo400
                                )
                                Text(
                                    text = "Rellena los campos automáticamente",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (selectorExpandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (selectorExpandido) "Contraer" else "Expandir",
                            tint = Indigo400
                        )
                    }
                }

                AnimatedVisibility(visible = selectorExpandido) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedCategoriaSel,
                            onExpandedChange = { expandedCategoriaSel = it }
                        ) {
                            OutlinedTextField(
                                value = categorias.find { it.id == categoriaSeleccionadaId }?.nombre ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoría") },
                                placeholder = { Text("Selecciona una categoría") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategoriaSel) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCategoriaSel,
                                onDismissRequest = { expandedCategoriaSel = false }
                            ) {
                                categorias.forEach { categoria ->
                                    DropdownMenuItem(
                                        text = { Text(categoria.nombre) },
                                        onClick = {
                                            formViewModel.seleccionarCategoriaDelSelector(categoria.id)
                                            servicioSeleccionado = null
                                            expandedCategoriaSel = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedServicioSel && categoriaSeleccionadaId != null && !cargandoServicios,
                            onExpandedChange = { if (categoriaSeleccionadaId != null && !cargandoServicios) expandedServicioSel = it }
                        ) {
                            OutlinedTextField(
                                value = when {
                                    cargandoServicios -> "Cargando servicios…"
                                    servicioSeleccionado != null -> servicioSeleccionado!!.nombre
                                    else -> ""
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Servicio") },
                                placeholder = { Text(if (categoriaSeleccionadaId == null) "Primero elige una categoría" else "Selecciona un servicio") },
                                trailingIcon = {
                                    if (cargandoServicios) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expandedServicioSel && categoriaSeleccionadaId != null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                enabled = categoriaSeleccionadaId != null && !cargandoServicios
                            )
                            ExposedDropdownMenu(
                                expanded = expandedServicioSel && categoriaSeleccionadaId != null && !cargandoServicios,
                                onDismissRequest = { expandedServicioSel = false }
                            ) {
                                serviciosPorCategoria.forEach { item ->
                                    val etiquetaPrecio = if (item.precioMensual != null) {
                                        val periodoCorto = when (item.periodoFacturacion.uppercase()) {
                                            "YEARLY" -> "año"
                                            "WEEKLY" -> "sem."
                                            else -> "mes"
                                        }
                                        " — ${item.precioMensual} ${item.moneda}/$periodoCorto"
                                    } else ""
                                    DropdownMenuItem(
                                        text = { Text("${item.nombre}$etiquetaPrecio") },
                                        onClick = {
                                            servicioSeleccionado = item
                                            expandedServicioSel = false
                                        }
                                    )
                                }
                            }
                        }

                        if (servicioSeleccionado != null) {
                            FilledTonalButton(
                                onClick = {
                                    servicioSeleccionado?.let { formViewModel.seleccionarServicioDelCatalogo(it) }
                                    selectorExpandido = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Aplicar selección")
                            }
                        }
                    }
                }
            }

            // ── Sección: Información del servicio ─────────────────────────
            SeccionFormulario("Información del servicio")

            OutlinedTextField(
                value = nombre,
                onValueChange = { formViewModel.nombre.value = it },
                label = { Text("Nombre del servicio *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { formViewModel.descripcion.value = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                maxLines = 3
            )

            // ── Sección: Facturación ───────────────────────────────────────
            SeccionFormulario("Facturación")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = precio,
                    onValueChange = { formViewModel.precio.value = it },
                    label = { Text("Importe *") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = expandedMoneda,
                    onExpandedChange = { expandedMoneda = it },
                    modifier = Modifier.weight(0.6f)
                ) {
                    OutlinedTextField(
                        value = moneda,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Moneda") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedMoneda) },
                        modifier = Modifier.menuAnchor(),
                        enabled = !isLoading
                    )
                    ExposedDropdownMenu(expanded = expandedMoneda, onDismissRequest = { expandedMoneda = false }) {
                        monedasOpciones.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = { formViewModel.moneda.value = opcion; expandedMoneda = false }
                            )
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = expandedPeriodo, onExpandedChange = { expandedPeriodo = it }) {
                OutlinedTextField(
                    value = periodosOpciones.find { it.first == periodoFacturacion }?.second ?: periodoFacturacion,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frecuencia de pago") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedPeriodo) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    enabled = !isLoading
                )
                ExposedDropdownMenu(expanded = expandedPeriodo, onDismissRequest = { expandedPeriodo = false }) {
                    periodosOpciones.forEach { (valor, etiqueta) ->
                        DropdownMenuItem(
                            text = { Text(etiqueta) },
                            onClick = { formViewModel.periodoFacturacion.value = valor; expandedPeriodo = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = if (fechaRenovacion.isNotBlank()) {
                    runCatching {
                        val parts = fechaRenovacion.split("-")
                        "${parts[2]}/${parts[1]}/${parts[0]}"
                    }.getOrDefault(fechaRenovacion)
                } else "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de renovación *") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                enabled = !isLoading,
                singleLine = true
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val localDate = Instant.fromEpochMilliseconds(millis)
                                    .toLocalDateTime(TimeZone.UTC).date
                                formViewModel.fechaRenovacion.value = localDate.toString()
                            }
                            showDatePicker = false
                        }) { Text("Aceptar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // ── Período de prueba gratuita ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Período de prueba gratuita",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = esPrueba,
                    onCheckedChange = { formViewModel.esPrueba.value = it },
                    enabled = !isLoading
                )
            }

            if (esPrueba) {
                val valorFechaFinPrueba: String = if (!fechaFinPrueba.isNullOrBlank()) {
                    runCatching {
                        val parts = fechaFinPrueba!!.split("-")
                        "${parts[2]}/${parts[1]}/${parts[0]}"
                    }.getOrElse { fechaFinPrueba!! }
                } else ""
                OutlinedTextField(
                    value = valorFechaFinPrueba,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fin de prueba") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerPrueba = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha fin de prueba")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePickerPrueba = true },
                    enabled = !isLoading,
                    singleLine = true
                )
            }

            if (showDatePickerPrueba) {
                DatePickerDialog(
                    onDismissRequest = { showDatePickerPrueba = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerStatePrueba.selectedDateMillis?.let { millis ->
                                val localDate = Instant.fromEpochMilliseconds(millis)
                                    .toLocalDateTime(TimeZone.UTC).date
                                formViewModel.seleccionarFechaFinPrueba(localDate.toString())
                            }
                            showDatePickerPrueba = false
                        }) { Text("Aceptar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePickerPrueba = false }) { Text("Cancelar") }
                    }
                ) {
                    DatePicker(state = datePickerStatePrueba)
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedCategoriaForm,
                onExpandedChange = { expandedCategoriaForm = it }
            ) {
                OutlinedTextField(
                    value = categorias.find { it.id == categoriaId }?.nombre ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría *") },
                    placeholder = { Text("Selecciona una categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategoriaForm) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    enabled = !isLoading,
                    isError = uiState is FormUiState.Error && (uiState as FormUiState.Error).mensaje == "Selecciona una categoría"
                )
                ExposedDropdownMenu(
                    expanded = expandedCategoriaForm,
                    onDismissRequest = { expandedCategoriaForm = false }
                ) {
                    categorias.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria.nombre) },
                            onClick = {
                                formViewModel.seleccionarCategoria(categoria.id)
                                expandedCategoriaForm = false
                            }
                        )
                    }
                }
            }

            // ── Sección: Notas ─────────────────────────────────────────────
            SeccionFormulario("Notas adicionales")

            OutlinedTextField(
                value = notas,
                onValueChange = { formViewModel.notas.value = it },
                label = { Text("Notas") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                maxLines = 3
            )

            if (uiState is FormUiState.Error) {
                Text(
                    text = (uiState as FormUiState.Error).mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { formViewModel.enviar(esEdicion = suscripcionId != null, id = suscripcionId) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Indigo500,
                    contentColor = Color.White
                ),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Guardar", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SeccionFormulario(titulo: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
