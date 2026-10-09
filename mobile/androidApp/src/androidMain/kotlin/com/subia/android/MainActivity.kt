package com.subia.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.subia.android.navigation.DashboardRoute
import com.subia.android.navigation.LoginRoute
import com.subia.android.ui.SubIAApp
import com.subia.android.ui.theme.SubIATheme
import com.subia.android.ui.theme.ThemeState
import com.subia.android.util.IdiomaApp
import com.subia.android.worker.ProgramadorAvisos
import com.subia.shared.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.viewmodel.koinViewModel

/** Punto de entrada de la app. Única Activity, todo lo demás es Compose. */
class MainActivity : AppCompatActivity() {

    /** Estado del deep link de vuelta del consentimiento de Gmail: "ok"/"error", o null. */
    private val gmailReturnStatus = MutableStateFlow<String?>(null)

    /** Suscripción a abrir porque el usuario tocó su aviso, o null. */
    private val suscripcionDesdeAviso = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Debe ir antes de super.onCreate: instala el splash (Theme.SubIA.Starting) y
        // cambia a postSplashScreenTheme (Theme.SubIA) sin flash de fondo.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        IdiomaApp.sincronizarDesdeAppCompat(this)
        handleGmailDeepLink(intent)
        // Solo en el primer onCreate: tras rotar, el intent original sigue ahí y volvería a
        // abrir el detalle aunque el usuario ya hubiera salido de él.
        if (savedInstanceState == null) handleAvisoIntent(intent)

        // Cargar la preferencia de color dinámico (Material You) antes de componer el tema.
        ThemeState.load(this)

        // El permiso POST_NOTIFICATIONS NO se pide aquí: se pide en contexto (página de avisos
        // del onboarding, al guardar la primera suscripción o desde Ajustes). Si el usuario
        // no lo concede, la comprobación sigue programada y las notificaciones simplemente no
        // se muestran — degradación elegante, sin crash.

        setContent {
            SubIATheme(dynamicColor = ThemeState.dynamicColor) {
                val authViewModel: AuthViewModel = koinViewModel()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                LaunchedEffect(Unit) { authViewModel.checkSession() }

                // Con sesión (restaurada o tras login): comprobación diaria programada y una
                // comprobación ahora. NO se cancela al ver isLoggedIn = false: ese es el valor
                // inicial antes de checkSession() y cancelaba los avisos en cada arranque
                // (N-01). Solo se cancela en el cierre de sesión explícito (ui/SubIAApp).
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        ProgramadorAvisos.programar(this@MainActivity)
                        // Margen para que el Inicio pida la lista antes; el worker refresca igual.
                        ProgramadorAvisos.comprobarAhora(this@MainActivity, retrasoSegundos = 10, refrescarRed = true)
                    }
                }

                val navController = rememberNavController()
                val startDestination = if (isLoggedIn) DashboardRoute else LoginRoute
                val gmailStatus by gmailReturnStatus.collectAsState()
                val suscripcionAviso by suscripcionDesdeAviso.collectAsState()

                SubIAApp(
                    navController = navController,
                    startDestination = startDestination,
                    authViewModel = authViewModel,
                    gmailReturnStatus = gmailStatus,
                    onGmailReturnConsumed = { gmailReturnStatus.value = null },
                    suscripcionDesdeAviso = suscripcionAviso,
                    onSuscripcionDesdeAvisoConsumida = { suscripcionDesdeAviso.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleGmailDeepLink(intent)
        handleAvisoIntent(intent)
    }

    /** Toque en un aviso de cobro: el id llega como extra (ver NotificadorAvisos). */
    private fun handleAvisoIntent(intent: Intent?) {
        intent ?: return
        // Al reabrir desde Recientes (también tras morir el proceso) Android reentrega el intent
        // con el que se creó la tarea, con el extra incluido: no es un toque nuevo en el aviso.
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        val id = intent.getLongExtra(EXTRA_SUSCRIPCION_ID, -1L)
        // Consumido: que un getIntent() posterior no lo vuelva a ver.
        intent.removeExtra(EXTRA_SUSCRIPCION_ID)
        if (id > 0) suscripcionDesdeAviso.value = id
    }

    /** Captura el deep link subia://gmail/done?status=... y publica el resultado para la UI. */
    private fun handleGmailDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "subia" && uri.host == "gmail") {
            gmailReturnStatus.value = uri.getQueryParameter("status") ?: "ok"
        }
    }

    companion object {
        /** Extra del PendingIntent de los avisos con el id de la suscripción a abrir. */
        const val EXTRA_SUSCRIPCION_ID = "com.subia.android.extra.SUSCRIPCION_ID"
    }
}
