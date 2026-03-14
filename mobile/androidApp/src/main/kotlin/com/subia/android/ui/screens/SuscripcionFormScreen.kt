package com.subia.android.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.subia.shared.viewmodel.FormUiState
import com.subia.shared.viewmodel.SuscripcionFormViewModel
import com.subia.shared.viewmodel.CategoriasViewModel
import org.koin.androidx.compose.koinViewModel

/** Pantalla de formulario para crear o editar una suscripción. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionFormScreen(
    suscripcionId: Long? = null,
    onSuccess: () -> Unit,
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

    val isLoading = uiState is FormUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is FormUiState.Success) onSuccess()
    }

    var expandedMoneda by remember { mutableStateOf(false) }
    var expandedPeriodo by remember { mutableStateOf(false) }
    val monedasOpciones = listOf("EUR", "USD", "GBP")
    val periodosOpciones = listOf("MONTHLY" to "Mensual", "YEARLY" to "Anual", "WEEKLY" to "Semanal")

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
                value = fechaRenovacion,
                onValueChange = { formViewModel.fechaRenovacion.value = it },
                label = { Text("Fecha de renovación * (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                placeholder = { Text("2026-04-01") },
                singleLine = true
            )

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
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Guardar")
            }
        }
    }
}
