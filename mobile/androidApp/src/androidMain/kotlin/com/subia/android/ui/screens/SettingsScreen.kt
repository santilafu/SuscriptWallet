package com.subia.android.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subia.android.util.toCsv
import com.subia.android.worker.DEFAULT_NOTIFICATION_DAYS_BEFORE
import com.subia.android.worker.KEY_NOTIFICATION_DAYS_BEFORE
import com.subia.shared.model.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter

private const val PREFS_NAME_SETTINGS = "subia_cache"
private const val KEY_SUBSCRIPTIONS = "subscriptions"

private val reminderOptions = listOf(1, 3, 7, 14)
private val csvJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME_SETTINGS, Context.MODE_PRIVATE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedDays by remember {
        mutableIntStateOf(prefs.getInt(KEY_NOTIFICATION_DAYS_BEFORE, DEFAULT_NOTIFICATION_DAYS_BEFORE))
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val subsJson = prefs.getString(KEY_SUBSCRIPTIONS, null).orEmpty()
                    val suscripciones: List<Subscription> = if (subsJson.isBlank()) {
                        emptyList()
                    } else {
                        csvJson.decodeFromString(subsJson)
                    }
                    val csv = suscripciones.toCsv()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                            writer.write(csv)
                        }
                    } ?: error("No se pudo abrir el fichero")
                }
            }
            if (result.isSuccess) {
                snackbarHostState.showSnackbar("Exportado a ${uri.lastPathSegment ?: "fichero"}")
            } else {
                snackbarHostState.showSnackbar("Error al exportar")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Notificaciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Avisarme antes de una renovación",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))

            reminderOptions.forEach { dias ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedDays = dias
                            prefs.edit().putInt(KEY_NOTIFICATION_DAYS_BEFORE, dias).apply()
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedDays == dias,
                        onClick = {
                            selectedDays = dias
                            prefs.edit().putInt(KEY_NOTIFICATION_DAYS_BEFORE, dias).apply()
                        }
                    )
                    Text(
                        text = if (dias == 1) "1 día" else "$dias días",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "También se aplica a las pruebas gratuitas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Exportar suscripciones",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Descarga todas tus suscripciones en formato CSV.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { exportLauncher.launch("subia_suscripciones.csv") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Exportar a CSV")
            }
        }
    }
}
