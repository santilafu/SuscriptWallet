package com.subia.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.subia.android.worker.RenovacionWorker
import com.subia.shared.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel
import java.util.concurrent.TimeUnit

/** Punto de entrada de la app. Única Activity, todo lo demás es Compose. */
class MainActivity : ComponentActivity() {

    /**
     * Lanzador de petición de permiso POST_NOTIFICATIONS (Android 13+).
     * Si el usuario deniega el permiso, WorkManager sigue programado pero las notificaciones
     * no se mostrarán hasta que el permiso sea concedido — sin crash ni reintentos forzados.
     */
    private val solicitarPermisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // No se requiere acción en caso de denegación: degradación elegante.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            SubIATheme {
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

                SubIAApp(
                    navController = navController,
                    startDestination = startDestination,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
