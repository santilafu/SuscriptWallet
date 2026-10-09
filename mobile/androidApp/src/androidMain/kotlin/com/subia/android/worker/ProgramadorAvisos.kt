package com.subia.android.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.subia.shared.model.minutosHasta
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Único punto que programa y cancela la comprobación de avisos. Antes la programaba la UI y un
 * `LaunchedEffect` la cancelaba en cada arranque porque la sesión empezaba en "no logueado"
 * (N-01). Ahora:
 * - [programar]: al arrancar con sesión, tras login y al restaurar sesión. Idempotente (KEEP).
 * - [alCerrarSesion]: solo en el cierre de sesión explícito o por sesión caducada.
 */
object ProgramadorAvisos {

    /** Hora local del aviso diario: de mañana, pero no a primera hora. */
    const val HORA_AVISO = 9
    const val MINUTO_AVISO = 30

    private const val TRABAJO_DIARIO = "avisos_renovacion_diario"
    private const val TRABAJO_AHORA = "avisos_renovacion_ahora"
    private const val TRABAJO_APLAZADO = "avisos_renovacion_aplazado"

    /** Nombre del trabajo único en versiones ≤ 2.17.3 (sin hora fija): se sustituye. */
    private const val TRABAJO_ANTIGUO = "renovaciones"

    /**
     * Programa la comprobación diaria hacia las [HORA_AVISO]:[MINUTO_AVISO] locales. Sin
     * restricciones de red: el worker intenta refrescar y, si no hay red, usa la caché.
     * KEEP conserva la hora ya anclada en arranques posteriores.
     */
    fun programar(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(TRABAJO_ANTIGUO)
        val request = PeriodicWorkRequestBuilder<RenovacionWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(minutosHastaLaHoraDelAviso(), TimeUnit.MINUTES)
            .addTag(RenovacionWorker.TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(TRABAJO_DIARIO, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * Comprobación puntual (al abrir la app, tras crear/editar/borrar una suscripción, al cambiar
     * el umbral o conceder el permiso). Con la ventana y el registro de avisados no duplica nada;
     * sirve para no esperar al día siguiente (p. ej. alta por la tarde de algo que se cobra mañana).
     *
     * @param retrasoSegundos margen para que las pantallas guarden antes su caché.
     * @param refrescarRed si el worker debe pedir la lista al servidor (con caída a caché).
     */
    fun comprobarAhora(context: Context, retrasoSegundos: Long = 0, refrescarRed: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<RenovacionWorker>()
            .setInitialDelay(retrasoSegundos, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    RenovacionWorker.KEY_INMEDIATO to true,
                    RenovacionWorker.KEY_REFRESCAR_RED to refrescarRed
                )
            )
            .addTag(RenovacionWorker.TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(TRABAJO_AHORA, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * La ejecución diaria cayó en horas de silencio (WorkManager la retrasó o la deriva de cada
     * día la llevó a la noche): se repite a la hora del aviso, sin sonar de madrugada.
     */
    fun aplazarHastaLaHoraDelAviso(context: Context) {
        val request = OneTimeWorkRequestBuilder<RenovacionWorker>()
            .setInitialDelay(minutosHastaLaHoraDelAviso(), TimeUnit.MINUTES)
            .addTag(RenovacionWorker.TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(TRABAJO_APLAZADO, ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Cierre de sesión: nada programado, bandeja vacía y registro de avisados borrado (el
     * siguiente usuario del móvil no debe recibir avisos de esta cuenta, N-06).
     */
    fun alCerrarSesion(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(RenovacionWorker.TAG)
        listOf(TRABAJO_DIARIO, TRABAJO_AHORA, TRABAJO_APLAZADO, TRABAJO_ANTIGUO).forEach(workManager::cancelUniqueWork)
        NotificadorAvisos.cancelarTodos(context)
        RegistroAvisos.borrar(context)
    }

    private fun minutosHastaLaHoraDelAviso(): Long {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return minutosHasta(ahora, HORA_AVISO, MINUTO_AVISO)
    }
}
