package com.subia.android.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.subia.shared.cache.CacheRepository
import com.subia.shared.model.Subscription
import com.subia.shared.model.avisosPendientes
import com.subia.shared.model.esHoraDeSilencio
import com.subia.shared.model.purgarAvisosAntiguos
import com.subia.shared.repository.AuthRepository
import com.subia.shared.repository.SubscriptionRepository
import com.subia.android.util.IdiomaApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Comprueba qué avisos de cobro o de fin de prueba tocan hoy y los muestra.
 *
 * 1. Si es la ejecución diaria y cae en horas de silencio, se aplaza a la hora del aviso.
 * 2. Intenta refrescar la lista desde el servidor (Render puede estar dormido: con timeout) y,
 *    si falla, usa la caché más reciente de las pantallas. La red nunca bloquea el aviso.
 * 3. Sin sesión no avisa (la caché podría ser de otra cuenta).
 * 4. La decisión es [avisosPendientes] (shared, con tests): activas, próxima renovación real,
 *    ventana `[hoy, hoy + umbral]` y sin repetir lo ya avisado ([RegistroAvisos]).
 * 5. Solo se marcan como avisados los que se han mostrado: si las notificaciones están
 *    desactivadas, saldrán cuando se activen (dentro de la ventana).
 */
class RenovacionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    override suspend fun doWork(): Result {
        // En Android 8-12 los recursos del worker son los del sistema: sin esto, los avisos y el
        // nombre del canal salían en el idioma del móvil y no en el elegido en Ajustes.
        val contexto = IdiomaApp.contexto(applicationContext)
        NotificadorAvisos.crearCanal(contexto)

        val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val inmediato = inputData.getBoolean(KEY_INMEDIATO, false)
        if (!inmediato && esHoraDeSilencio(ahora.hour)) {
            ProgramadorAvisos.aplazarHastaLaHoraDelAviso(contexto)
            return Result.success()
        }

        val refrescar = inputData.getBoolean(KEY_REFRESCAR_RED, true)
        val subs = (if (refrescar) refrescarDesdeRed() else null)
            ?: CacheAvisos.leerSuscripciones(contexto)
            ?: return Result.success()

        // Después del refresco: si el servidor rechazó la sesión, ApiClient ya borró los tokens.
        if (!haySesion()) return Result.success()

        val hoy = ahora.date
        // La diaria, la inmediata y la aplazada pueden ejecutarse a la vez: sin exclusión, las
        // dos leían el registro antes de que la otra lo guardase y el aviso salía duplicado.
        candado.withLock {
            // Un aviso de una suscripción ya borrada (aquí o en la web) abriría un detalle vacío.
            NotificadorAvisos.retirarAvisosHuerfanos(contexto, subs)
            val registro = purgarAvisosAntiguos(RegistroAvisos.leer(contexto), hoy)
            val avisos = avisosPendientes(subs, hoy, CacheAvisos.umbralDias(contexto), registro)
            val mostrados = NotificadorAvisos.mostrar(contexto, avisos)
            RegistroAvisos.guardar(contexto, registro + mostrados)
        }
        return Result.success()
    }

    /**
     * Lista fresca del servidor, guardada en caché para la próxima vez; `null` si no hay red,
     * el servidor tarda demasiado o falla (se usará la caché).
     */
    private suspend fun refrescarDesdeRed(): List<Subscription>? = try {
        withTimeoutOrNull(TIMEOUT_RED_MS) { get<SubscriptionRepository>().getAll().getOrNull() }
            ?.also { subs ->
                val cache = get<CacheRepository>()
                cache.saveString(CacheAvisos.CLAVE_SUSCRIPCIONES, CacheAvisos.codificar(subs))
                cache.saveTimestamp(CacheAvisos.CLAVE_SUSCRIPCIONES)
            }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG_LOG, "No se pudo refrescar la lista; se usa la caché", e)
        null
    }

    /** Si Koin no respondiera (no debería: lo arranca la Application), se asume sesión. */
    private fun haySesion(): Boolean =
        runCatching { get<AuthRepository>().hasValidSession() }.getOrDefault(true)

    companion object {
        /** Tag común de todos los trabajos de avisos (para cancelarlos juntos al cerrar sesión). */
        const val TAG = "renovaciones"

        /** `true` en comprobaciones lanzadas con la app en uso: no aplican horas de silencio. */
        const val KEY_INMEDIATO = "inmediato"
        const val KEY_REFRESCAR_RED = "refrescar_red"

        /** Render free tarda ~50 s en despertar; ApiClient corta a los 30 s por petición. */
        private const val TIMEOUT_RED_MS = 45_000L
        private const val TAG_LOG = "RenovacionWorker"

        /** Único por proceso (WorkManager ejecuta todos los workers en el proceso de la app). */
        private val candado = Mutex()
    }
}
