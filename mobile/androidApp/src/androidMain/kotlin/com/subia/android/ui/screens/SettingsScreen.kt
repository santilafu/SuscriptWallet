package com.subia.android.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import com.subia.android.BuildConfig
import com.subia.android.R
import com.subia.android.ui.theme.ThemeState
import com.subia.android.util.NotificacionesPermiso
import com.subia.android.util.openCustomTab
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
private const val PRIVACY_POLICY_URL = "https://suscriptwallet.onrender.com/privacidad"

private val reminderOptions = listOf(1, 3, 7, 14)
private val languageOptions = listOf(
    "" to R.string.language_system,
    "es" to R.string.language_es,
    "en" to R.string.language_en,
    "fr" to R.string.language_fr
)
private val csvJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

/**
 * Ajustes de la app. Las acciones de navegación (Categorías, Gmail) y las de un solo toque
 * (Exportar) van como `ListItem` con chevron; "Cerrar sesión" pide confirmación porque
 * descarta la sesión sin aviso previo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToCategorias: () -> Unit = {},
    onDetectGmail: () -> Unit = {},
    onVerTutorial: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME_SETTINGS, Context.MODE_PRIVATE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var mostrarDialogoLogout by remember { mutableStateOf(false) }

    var selectedDays by remember {
        mutableIntStateOf(prefs.getInt(KEY_NOTIFICATION_DAYS_BEFORE, DEFAULT_NOTIFICATION_DAYS_BEFORE))
    }

    // Estado real de los avisos (permiso + canal): se relee al volver de los ajustes del sistema.
    var avisosActivados by remember { mutableStateOf(NotificacionesPermiso.estanActivadas(context)) }
    LifecycleResumeEffect(Unit) {
        avisosActivados = NotificacionesPermiso.estanActivadas(context)
        onPauseOrDispose { }
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        avisosActivados = NotificacionesPermiso.estanActivadas(context)
        val activity = context as? Activity
        // Denegado de forma permanente (el sistema ya no muestra el diálogo): única salida, Ajustes.
        if (!concedido && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            NotificacionesPermiso.abrirAjustesDelSistema(context)
        }
    }
    val gestionarAvisos = {
        if (NotificacionesPermiso.hayQuePedir(context)) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificacionesPermiso.abrirAjustesDelSistema(context)
        }
    }

    var selectedLocale by remember {
        val current = AppCompatDelegate.getApplicationLocales()
        mutableStateOf(if (current.isEmpty) "" else current.get(0)?.language ?: "")
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
                    } ?: error(context.getString(R.string.file_open_error))
                }
            }
            if (result.isSuccess) {
                snackbarHostState.showSnackbar(context.getString(R.string.export_success, uri.lastPathSegment ?: "file"))
            } else {
                snackbarHostState.showSnackbar(context.getString(R.string.export_error))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                .padding(vertical = 8.dp)
        ) {
            // ── Apariencia (Material You, solo Android 12+) ───────────
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SeccionTitulo(stringResource(R.string.appearance))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.dynamic_color)) },
                    supportingContent = { Text(stringResource(R.string.dynamic_color_desc)) },
                    trailingContent = {
                        Switch(
                            checked = ThemeState.dynamicColor,
                            onCheckedChange = { ThemeState.setDynamicColor(context, it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { ThemeState.setDynamicColor(context, !ThemeState.dynamicColor) }
                )
                SeparadorSeccion()
            }

            // ── Idioma ────────────────────────────────────────────────
            SeccionTitulo(stringResource(R.string.language))
            // Fila `selectable` con rol RadioButton y radio sin onClick: un único nodo
            // enfocable por fila y el grupo se anuncia como tal en TalkBack.
            Column(Modifier.selectableGroup()) {
                languageOptions.forEach { (localeTag, labelRes) ->
                    FilaRadio(
                        selected = selectedLocale == localeTag,
                        label = stringResource(labelRes)
                    ) {
                        selectedLocale = localeTag
                        val locales = if (localeTag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                        else LocaleListCompat.forLanguageTags(localeTag)
                        AppCompatDelegate.setApplicationLocales(locales)
                    }
                }
            }
            SeparadorSeccion()

            // ── Notificaciones ────────────────────────────────────────
            SeccionTitulo(stringResource(R.string.notifications))
            ListItem(
                headlineContent = { Text(stringResource(R.string.notif_status_title)) },
                supportingContent = {
                    Text(stringResource(if (avisosActivados) R.string.notif_status_on else R.string.notif_status_off))
                },
                leadingContent = {
                    Icon(
                        if (avisosActivados) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = if (avisosActivados) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = gestionarAvisos)
            )
            Text(
                text = stringResource(R.string.notify_before_renewal),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Column(Modifier.selectableGroup()) {
                reminderOptions.forEach { dias ->
                    FilaRadio(
                        selected = selectedDays == dias,
                        label = if (dias == 1) stringResource(R.string.one_day) else stringResource(R.string.n_days, dias)
                    ) {
                        selectedDays = dias
                        prefs.edit().putInt(KEY_NOTIFICATION_DAYS_BEFORE, dias).apply()
                    }
                }
            }
            Text(
                text = stringResource(R.string.also_applies_to_trials),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            SeparadorSeccion()

            // ── Datos ─────────────────────────────────────────────────
            SeccionTitulo(stringResource(R.string.data_section))
            FilaNavegacion(
                icon = Icons.Outlined.Category,
                titulo = stringResource(R.string.categories_title),
                descripcion = stringResource(R.string.organize_expenses),
                onClick = onNavigateToCategorias
            )
            FilaNavegacion(
                icon = Icons.Outlined.FileDownload,
                titulo = stringResource(R.string.export_subscriptions),
                descripcion = stringResource(R.string.export_description),
                onClick = { exportLauncher.launch("subia_suscripciones.csv") }
            )
            FilaNavegacion(
                icon = Icons.Outlined.Email,
                titulo = stringResource(R.string.gmail_detect_section),
                descripcion = stringResource(R.string.gmail_detect_desc),
                onClick = onDetectGmail
            )
            SeparadorSeccion()

            // ── Ayuda ─────────────────────────────────────────────────
            SeccionTitulo(stringResource(R.string.help_section))
            FilaNavegacion(
                icon = Icons.Outlined.School,
                titulo = stringResource(R.string.view_tutorial_again),
                descripcion = stringResource(R.string.onb1_title),
                onClick = onVerTutorial
            )
            SeparadorSeccion()

            // ── Cuenta ────────────────────────────────────────────────
            SeccionTitulo(stringResource(R.string.account_section))
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error)
                },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { mostrarDialogoLogout = true }
            )

            // ── Pie: versión y política de privacidad ─────────────────
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(onClick = { abrirPoliticaPrivacidad(context) }) {
                    Text(stringResource(R.string.privacy_policy))
                }
                Text(
                    text = stringResource(R.string.app_version, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (mostrarDialogoLogout) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoLogout = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoLogout = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.logout)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoLogout = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/** Abre la política de privacidad en Custom Tab; si no hay navegador compatible, Intent genérico. */
private fun abrirPoliticaPrivacidad(context: Context) {
    runCatching { openCustomTab(context, PRIVACY_POLICY_URL) }
        .onFailure {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri())) }
        }
}

/** Título de sección al estilo de los Ajustes de Material: pequeño y en color secundario. */
@Composable
private fun SeccionTitulo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SeparadorSeccion() {
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

/** Fila de navegación: icono + título + descripción + chevron. */
@Composable
private fun FilaNavegacion(icon: ImageVector, titulo: String, descripcion: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = { Text(descripcion) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun FilaRadio(selected: Boolean, label: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
