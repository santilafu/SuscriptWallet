package com.subia.android.ui.screens

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.AnimatedVisibility
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.subia.android.R
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.components.importeConPeriodo
import com.subia.android.ui.theme.Violet600
import com.subia.android.util.NotificacionesPermiso
import com.subia.android.util.fechaIsoLegible
import com.subia.shared.model.BillingCycle
import com.subia.shared.model.CatalogItem
import com.subia.shared.viewmodel.Campo
import com.subia.shared.viewmodel.FormError
import com.subia.shared.viewmodel.FormUiState
import com.subia.shared.viewmodel.SuscripcionFormViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel

/**
 * Formulario de alta y edición de una suscripción.
 *
 * - Validación en vivo: cada campo muestra su error (`isError` + `supportingText`) en cuanto
 *   el usuario lo abandona, y Guardar solo se habilita cuando todo es válido.
 * - "Nombre del servicio" autocompleta desde el catálogo (logo + nombre + precio) y, al
 *   elegir, prerrellena precio, ciclo, moneda, prueba y categoría.
 * - Salir con cambios (botón atrás o flecha) pregunta antes de descartar.
 *
 * @param prefillNombre nombre con el que abrir el alta (desde el estado vacío de la lista).
 * @param onSuccess navegación de vuelta tras guardar.
 * @param onCancel navegación de vuelta sin guardar (por defecto la misma que [onSuccess]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionFormScreen(
    suscripcionId: Long? = null,
    prefillNombre: String? = null,
    onSuccess: () -> Unit,
    onCancel: () -> Unit = onSuccess,
    navController: NavController? = null,
    formViewModel: SuscripcionFormViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

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
    val sugerencias by formViewModel.sugerencias.collectAsState()
    val catalogoSeleccionado by formViewModel.catalogoSeleccionado.collectAsState()
    val errores by formViewModel.errores.collectAsState()
    val puedeGuardar by formViewModel.puedeGuardar.collectAsState()
    val hayCambios by formViewModel.hayCambios.collectAsState()
    val cargandoEdicion by formViewModel.cargandoEdicion.collectAsState()

    val esEdicion = suscripcionId != null
    val isLoading = uiState is FormUiState.Loading
    val camposBloqueados = isLoading || cargandoEdicion

    // ── Carga inicial. El ViewModel sobrevive a la rotación: si ya tiene datos, no se repite ──
    LaunchedEffect(Unit) {
        if (nombre.isNotBlank() || hayCambios) return@LaunchedEffect
        val itemJson = navController?.previousBackStackEntry?.savedStateHandle?.get<String>("catalog_item_json")
        when {
            suscripcionId != null -> formViewModel.cargarParaEditar(suscripcionId)
            itemJson != null -> {
                navController.previousBackStackEntry?.savedStateHandle?.remove<String>("catalog_item_json")
                runCatching { Json.decodeFromString<CatalogItem>(itemJson) }.getOrNull()?.let { item ->
                    formViewModel.seleccionarServicioDelCatalogo(item)
                    formViewModel.marcarComoSinCambios()
                }
            }
            !prefillNombre.isNullOrBlank() -> formViewModel.prerellenarPorNombre(prefillNombre)
        }
    }

    // ── Errores por campo: solo tras tocar el campo (no al abrir un formulario vacío) ────
    var tocados by remember { mutableStateOf(setOf<Campo>()) }
    fun errorDe(campo: Campo): FormError? = errores[campo]?.takeIf { campo in tocados }
    val errorGeneral = (uiState as? FormUiState.Error)?.error
        ?.takeIf { it == FormError.SinConexion || it == FormError.GuardadoFallido }

    // ── Permiso de notificaciones como fallback al guardar la primera suscripción ───────
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onSuccess()
    }
    LaunchedEffect(uiState) {
        if (uiState is FormUiState.Success) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!esEdicion && NotificacionesPermiso.hayQuePedir(context) && !NotificacionesPermiso.yaPedidoAlGuardar(context)) {
                NotificacionesPermiso.marcarPedidoAlGuardar(context)
                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onSuccess()
            }
        }
    }

    // ── Descartar cambios ───────────────────────────────────────────────────────────────
    var mostrarDescartar by remember { mutableStateOf(false) }
    val intentarSalir = { if (hayCambios) mostrarDescartar = true else onCancel() }
    BackHandler(enabled = hayCambios) { mostrarDescartar = true }

    // ── Selectores de fecha (UTC en ambos sentidos: el DatePicker de M3 trabaja en UTC) ──
    var mostrarFechaRenovacion by remember { mutableStateOf(false) }
    var mostrarFechaFinPrueba by remember { mutableStateOf(false) }
    val fuenteFechaRenovacion = remember { MutableInteractionSource() }
    val fuenteFechaFinPrueba = remember { MutableInteractionSource() }
    // Un campo readOnly consume el toque para enfocarse; escuchamos la pulsación para abrir el
    // picker tocando en cualquier parte del campo, no solo en el icono.
    LaunchedEffect(fuenteFechaRenovacion) {
        fuenteFechaRenovacion.interactions.collectLatest { if (it is PressInteraction.Release) mostrarFechaRenovacion = true }
    }
    LaunchedEffect(fuenteFechaFinPrueba) {
        fuenteFechaFinPrueba.interactions.collectLatest { if (it is PressInteraction.Release) mostrarFechaFinPrueba = true }
    }

    var expandedMoneda by remember { mutableStateOf(false) }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var expandedCategoria by remember { mutableStateOf(false) }
    var menuNombreAbierto by remember { mutableStateOf(false) }
    val monedasOpciones = listOf("EUR", "USD", "GBP")
    val periodosOpciones = listOf(
        "MONTHLY" to stringResource(R.string.monthly),
        "YEARLY" to stringResource(R.string.yearly),
        "WEEKLY" to stringResource(R.string.weekly)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (esEdicion) R.string.edit_subscription else R.string.new_subscription)) },
                navigationIcon = {
                    IconButton(onClick = intentarSalir) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        // imePadding: con edge-to-edge, adjustResize ya no encoge la ventana y el teclado tapaba
        // Notas y Guardar. consumeWindowInsets evita sumar dos veces la barra de navegación.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            if (cargandoEdicion) LinearProgressIndicator(Modifier.fillMaxWidth())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Información del servicio ────────────────────────────────────────
                SeccionFormulario(stringResource(R.string.service_info_section), primera = true)

                // Nombre con autocompletado: desplegable solo mientras hay sugerencias.
                val mostrarSugerencias = menuNombreAbierto && sugerencias.isNotEmpty()
                var nombreTuvoFoco by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = mostrarSugerencias,
                    onExpandedChange = { menuNombreAbierto = it }
                ) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { formViewModel.nombre.value = it; menuNombreAbierto = true },
                        label = { Text(stringResource(R.string.service_name_label)) },
                        leadingIcon = catalogoSeleccionado?.let { item ->
                            {
                                ServiceLogo(
                                    nombre = item.nombre, domain = item.domain, iconUrl = item.iconUrl,
                                    size = 24.dp, contentDescription = null
                                )
                            }
                        },
                        isError = errorDe(Campo.Nombre) != null,
                        supportingText = textoDeError(errorDe(Campo.Nombre)),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = {
                            menuNombreAbierto = false
                            focusManager.moveFocus(FocusDirection.Down)
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable, !camposBloqueados)
                            .onFocusChanged {
                                if (it.isFocused) nombreTuvoFoco = true
                                else if (nombreTuvoFoco) tocados = tocados + Campo.Nombre
                            },
                        enabled = !camposBloqueados,
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = mostrarSugerencias,
                        onDismissRequest = { menuNombreAbierto = false }
                    ) {
                        sugerencias.forEach { item ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    ServiceLogo(
                                        nombre = item.nombre, domain = item.domain, iconUrl = item.iconUrl,
                                        size = 32.dp, contentDescription = null
                                    )
                                },
                                text = { Text(item.nombre, maxLines = 1) },
                                trailingIcon = {
                                    precioCortoDeCatalogo(item)?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    formViewModel.seleccionarServicioDelCatalogo(item)
                                    tocados = tocados + Campo.Nombre
                                    menuNombreAbierto = false
                                    focusManager.clearFocus()
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { formViewModel.descripcion.value = it },
                    label = { Text(stringResource(R.string.description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !camposBloqueados,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    maxLines = 3
                )

                // ── Facturación ─────────────────────────────────────────────────────
                SeccionFormulario(stringResource(R.string.billing_section))

                val transformacionDecimal = remember {
                    if (java.text.DecimalFormatSymbols.getInstance().decimalSeparator == ',') {
                        VisualTransformation { texto ->
                            TransformedText(AnnotatedString(texto.text.replace('.', ',')), OffsetMapping.Identity)
                        }
                    } else VisualTransformation.None
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    var precioTuvoFoco by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = precio,
                        onValueChange = { formViewModel.precio.value = it },
                        label = { Text(stringResource(R.string.amount_label)) },
                        // "11,99 €" como se escribe en España/Francia (antes "€11.99"): símbolo
                        // detrás salvo $ y £, y la coma decimal del idioma solo en pantalla (el
                        // ViewModel acepta punto y coma).
                        prefix = if (simboloDelante(moneda)) { { Text(simboloMoneda(moneda)) } } else null,
                        suffix = if (!simboloDelante(moneda)) { { Text(simboloMoneda(moneda)) } } else null,
                        visualTransformation = transformacionDecimal,
                        isError = errorDe(Campo.Precio) != null,
                        supportingText = textoDeError(errorDe(Campo.Precio)),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged {
                                if (it.isFocused) precioTuvoFoco = true
                                else if (precioTuvoFoco) tocados = tocados + Campo.Precio
                            },
                        enabled = !camposBloqueados,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
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
                            label = { Text(stringResource(R.string.currency_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedMoneda) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, !camposBloqueados),
                            enabled = !camposBloqueados
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

                // Ayuda del importe. Hace de supportingText del campo, pero va bajo la fila entera
                // (a todo el ancho): bajo el campo de importe, que mide 5/8, se partía en tres líneas.
                // - Precio tal cual viene del catálogo: avisa de que es orientativo y editable
                //   (los recibos de importe variable —luz, teléfono, seguro— con su texto propio).
                // - Si no viene del catálogo o el usuario ya lo ha cambiado: qué hay que poner.
                // El error del campo tiene prioridad: mientras se ve, la ayuda se oculta.
                val itemCatalogo = catalogoSeleccionado
                    ?.takeIf { it.nombre.equals(nombre.trim(), ignoreCase = true) }
                val precioEsDelCatalogo = itemCatalogo != null &&
                    itemCatalogo.precioPara(BillingCycle.fromWire(periodoFacturacion))
                        ?.let { it == SuscripcionFormViewModel.parsearPrecio(precio) } == true
                val ayudaImporte = when {
                    !precioEsDelCatalogo -> R.string.price_per_charge_hint
                    itemCatalogo?.variablePrice == true -> R.string.price_adjust_hint
                    else -> R.string.price_catalog_hint
                }
                AnimatedVisibility(visible = errorDe(Campo.Precio) == null) {
                    Text(
                        text = stringResource(ayudaImporte),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp).offset(y = (-4).dp)
                    )
                }

                ExposedDropdownMenuBox(expanded = expandedPeriodo, onExpandedChange = { expandedPeriodo = it }) {
                    OutlinedTextField(
                        value = periodosOpciones.find { it.first == periodoFacturacion }?.second ?: periodoFacturacion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.payment_frequency)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedPeriodo) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, !camposBloqueados),
                        enabled = !camposBloqueados
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
                    value = fechaIsoLegible(fechaRenovacion),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.renewal_date_label)) },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.select_date)) },
                    isError = errorDe(Campo.FechaRenovacion) != null,
                    supportingText = textoDeError(errorDe(Campo.FechaRenovacion)),
                    interactionSource = fuenteFechaRenovacion,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !camposBloqueados,
                    singleLine = true
                )

                if (mostrarFechaRenovacion) {
                    SelectorDeFecha(
                        fechaIso = fechaRenovacion,
                        onElegida = { formViewModel.fechaRenovacion.value = it },
                        onCerrar = {
                            mostrarFechaRenovacion = false
                            tocados = tocados + Campo.FechaRenovacion
                        }
                    )
                }

                // ── Período de prueba ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.free_trial_period), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = esPrueba,
                        onCheckedChange = { formViewModel.esPrueba.value = it },
                        enabled = !camposBloqueados
                    )
                }

                if (esPrueba) {
                    OutlinedTextField(
                        value = fechaIsoLegible(fechaFinPrueba),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.trial_end_label)) },
                        trailingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.select_trial_end_date))
                        },
                        interactionSource = fuenteFechaFinPrueba,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !camposBloqueados,
                        singleLine = true
                    )
                }

                if (mostrarFechaFinPrueba) {
                    SelectorDeFecha(
                        fechaIso = fechaFinPrueba.orEmpty(),
                        onElegida = { formViewModel.seleccionarFechaFinPrueba(it) },
                        onCerrar = { mostrarFechaFinPrueba = false }
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = expandedCategoria,
                    onExpandedChange = { expandedCategoria = it }
                ) {
                    OutlinedTextField(
                        value = categorias.find { it.id == categoriaId }?.nombre ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category_required)) },
                        placeholder = { Text(stringResource(R.string.select_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategoria) },
                        isError = errorDe(Campo.Categoria) != null,
                        supportingText = textoDeError(errorDe(Campo.Categoria)),
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, !camposBloqueados),
                        enabled = !camposBloqueados
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoria,
                        onDismissRequest = {
                            expandedCategoria = false
                            tocados = tocados + Campo.Categoria
                        }
                    ) {
                        categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.nombre) },
                                onClick = {
                                    formViewModel.seleccionarCategoria(categoria.id)
                                    expandedCategoria = false
                                    tocados = tocados + Campo.Categoria
                                }
                            )
                        }
                    }
                }

                // ── Notas ───────────────────────────────────────────────────────────
                SeccionFormulario(stringResource(R.string.notes_section))

                OutlinedTextField(
                    value = notas,
                    onValueChange = { formViewModel.notas.value = it },
                    label = { Text(stringResource(R.string.notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !camposBloqueados,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    maxLines = 3
                )

                Text(
                    text = stringResource(R.string.required_legend),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorGeneral != null) {
                    Text(
                        text = stringResource(errorGeneral.stringRes()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        formViewModel.enviar(esEdicion = esEdicion, id = suscripcionId)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet600, contentColor = Color.White),
                    enabled = puedeGuardar && !camposBloqueados
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (mostrarDescartar) {
        AlertDialog(
            onDismissRequest = { mostrarDescartar = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_text)) },
            confirmButton = {
                TextButton(
                    onClick = { mostrarDescartar = false; onCancel() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDescartar = false }) { Text(stringResource(R.string.keep_editing)) }
            }
        )
    }
}

/**
 * Diálogo de fecha que abre en la fecha ya elegida (o en hoy) y devuelve ISO `yyyy-MM-dd`.
 * Se compone solo mientras está visible, así `rememberDatePickerState` arranca cada vez con la
 * fecha actual del campo (importante al editar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDeFecha(fechaIso: String, onElegida: (String) -> Unit, onCerrar: () -> Unit) {
    val estado = rememberDatePickerState(initialSelectedDateMillis = fechaIso.aMillisUtc() ?: hoyMillisUtc())
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let { onElegida(it.aFechaIso()) }
                onCerrar()
            }) { Text(stringResource(R.string.accept)) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.cancel)) } }
    ) {
        DatePicker(state = estado)
    }
}

/** ISO → milisegundos de medianoche UTC (lo que espera el DatePicker de Material 3). */
private fun String.aMillisUtc(): Long? = runCatching {
    LocalDate.parse(this).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}.getOrNull()

/** Milisegundos UTC del DatePicker → ISO sin desplazamiento de zona. */
private fun Long.aFechaIso(): String =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date.toString()

private fun hoyMillisUtc(): Long =
    Clock.System.todayIn(TimeZone.currentSystemDefault()).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

/** Texto de apoyo del campo con su error, o `null` para no reservar espacio. */
@Composable
private fun textoDeError(error: FormError?): (@Composable () -> Unit)? =
    error?.let { { Text(stringResource(it.stringRes())) } }

/** "12,99 €/mes" para la fila del desplegable; `null` si el catálogo no trae precio. */
@Composable
private fun precioCortoDeCatalogo(item: CatalogItem): String? {
    val precioCiclo = item.precioMensual
    val precioAnual = item.precioAnual
    val importe = when {
        precioCiclo != null -> importeConPeriodo(precioCiclo, item.moneda, item.periodoFacturacion)
        precioAnual != null -> importeConPeriodo(precioAnual, item.moneda, "YEARLY")
        else -> return null
    }
    return if (item.variablePrice) stringResource(R.string.price_approx_prefix, importe) else importe
}

private fun simboloDelante(moneda: String): Boolean = moneda.uppercase() in setOf("USD", "GBP")

private fun simboloMoneda(moneda: String): String = when (moneda.uppercase()) {
    "EUR" -> "€"
    "USD" -> "$"
    "GBP" -> "£"
    else -> moneda
}

@Composable
private fun SeccionFormulario(titulo: String, primera: Boolean = false) {
    // Más aire encima que debajo: el título se agrupa con sus campos, no con los anteriores.
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = if (primera) 0.dp else 12.dp),
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

/** Traduce el error tipado del ViewModel (sin textos) al recurso localizado. */
private fun FormError.stringRes(): Int = when (this) {
    FormError.NombreVacio -> R.string.form_error_name_required
    FormError.PrecioInvalido -> R.string.form_error_invalid_amount
    FormError.FechaRenovacionVacia -> R.string.form_error_renewal_date_required
    FormError.CategoriaNoSeleccionada -> R.string.form_error_category_required
    FormError.SinConexion -> R.string.form_error_offline
    FormError.GuardadoFallido -> R.string.form_error_save_failed
}
