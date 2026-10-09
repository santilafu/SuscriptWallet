package com.subia.android

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.subia.android.BuildConfig
import com.subia.android.util.IdiomaApp
import com.subia.android.worker.NotificadorAvisos
import com.subia.android.worker.ProgramadorAvisos
import com.subia.shared.di.androidModule
import com.subia.shared.di.sharedModule
import com.subia.shared.repository.AuthRepository
import com.subia.shared.viewmodel.SuscripcionesCambios
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Application principal de SubIA. Inicializa Koin con los módulos compartidos y Android. */
class SubIAApp : Application() {

    /** Ámbito de vida del proceso para escuchar cambios de suscripciones fuera de la UI. */
    private val ambitoApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // MobileAds es prescindible: si su init falla (Play Services, ID inválido), la app
        // debe seguir arrancando sin anuncios en lugar de crashear.
        try {
            MobileAds.initialize(this) {}
        } catch (e: Throwable) {
            Log.w("SubIAApp", "Fallo al inicializar MobileAds; se continúa sin anuncios", e)
        }
        startKoin {
            androidContext(this@SubIAApp)
            modules(
                androidModule,
                sharedModule(apiBaseUrl = BuildConfig.API_BASE_URL, isDebug = BuildConfig.DEBUG)
            )
        }
        iniciarAvisos()
    }

    /**
     * Los avisos no dependen de la UI (N-01, N-15):
     * - el canal existe desde el primer arranque (visible en los ajustes del sistema);
     * - con sesión, la comprobación diaria queda programada aunque el usuario no abra la app
     *   (p. ej. el proceso arranca por el widget o tras actualizar);
     * - tras crear, editar o borrar una suscripción se comprueba enseguida (refrescando del
     *   servidor), sin depender de qué pantallas tengan su caché al día (N-07).
     * Nada de esto debe poder tumbar el arranque.
     */
    private fun iniciarAvisos() {
        runCatching { NotificadorAvisos.crearCanal(IdiomaApp.contexto(this)) }
            .onFailure { Log.w("SubIAApp", "No se pudo crear el canal de avisos", it) }
        runCatching { if (get<AuthRepository>().hasValidSession()) ProgramadorAvisos.programar(this) }
            .onFailure { Log.w("SubIAApp", "No se pudo programar la comprobación de avisos", it) }
        ambitoApp.launch {
            // drop(1): el valor actual al suscribirse no es un cambio nuevo.
            SuscripcionesCambios.version.drop(1).collect {
                runCatching { ProgramadorAvisos.comprobarAhora(this@SubIAApp, retrasoSegundos = 5, refrescarRed = true) }
            }
        }
    }
}
