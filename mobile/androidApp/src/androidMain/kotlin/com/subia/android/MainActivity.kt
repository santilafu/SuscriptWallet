package com.subia.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.subia.android.navigation.DashboardRoute
import com.subia.android.navigation.LoginRoute
import com.subia.android.ui.SubIAApp
import com.subia.android.ui.theme.SubIATheme
import com.subia.android.ui.theme.ThemeState
import com.subia.android.worker.RenovacionWorker
import com.subia.shared.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.viewmodel.koinViewModel
import java.util.concurrent.TimeUnit

/** Punto de entrada de la app. Única Activity, todo lo demás es Compose. */
class MainActivity : AppCompatActivity() {

    /**
     * Lanzador de petición de permiso POST_NOTIFICATIONS (Android 13+).
     * Si el usuario deniega el permiso, WorkManager sigue programado pero las notificaciones
     * no se mostrarán hasta que el permiso sea concedido — sin crash ni reintentos forzados.
     */
    private val solicitarPermisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // No se requiere acción en caso de denegación: degradación elegante.
        }

    /** Estado del deep link de vuelta del consentimiento de Gmail: "ok"/"error", o null. */
    private val gmailReturnStatus = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleGmailDeepLink(intent)

        // Cargar la preferencia de color dinámico (Material You) antes de componer el tema.
        ThemeState.load(this)

        // Solicitar POST_NOTIFICATIONS en Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            solicitarPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Programar la revisión periódica de renovaciones (24 h).
        // ExistingPeriodicWorkPolicy.KEEP evita reprogramar si ya hay una tarea activa.
        val renovacionRequest = PeriodicWorkRequestBuilder<RenovacionWorker>(24, TimeUnit.HOURS)
            .addTag(RenovacionWorker.TAG)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RenovacionWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            renovacionRequest
        )

        setContent {
            SubIATheme(dynamicColor = ThemeState.dynamicColor) {
                val authViewModel: AuthViewModel = koinViewModel()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                LaunchedEffect(Unit) { authViewModel.checkSession() }

                // Cancelar WorkManager cuando el usuario cierra sesión
                LaunchedEffect(isLoggedIn) {
                    if (!isLoggedIn) {
                        WorkManager.getInstance(this@MainActivity)
                            .cancelAllWorkByTag(RenovacionWorker.TAG)
                    }
                }

                val navController = rememberNavController()
                val startDestination = if (isLoggedIn) DashboardRoute else LoginRoute
                val gmailStatus by gmailReturnStatus.collectAsState()

                SubIAApp(
                    navController = navController,
                    startDestination = startDestination,
                    authViewModel = authViewModel,
                    gmailReturnStatus = gmailStatus,
                    onGmailReturnConsumed = { gmailReturnStatus.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleGmailDeepLink(intent)
    }

    /** Captura el deep link subia://gmail/done?status=... y publica el resultado para la UI. */
    private fun handleGmailDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "subia" && uri.host == "gmail") {
            gmailReturnStatus.value = uri.getQueryParameter("status") ?: "ok"
        }
    }
}
