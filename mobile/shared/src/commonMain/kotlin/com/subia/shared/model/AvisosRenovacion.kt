package com.subia.shared.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/** Qué se avisa: un cobro de una suscripción de pago o el fin de una prueba gratuita. */
enum class TipoAviso { COBRO, FIN_PRUEBA }

/**
 * Un aviso que toca mostrar hoy.
 *
 * @property fecha fecha del cobro ([TipoAviso.COBRO]) o del fin de la prueba ([TipoAviso.FIN_PRUEBA]).
 * @property diasRestantes días desde hoy hasta [fecha] (0 = hoy).
 */
data class AvisoRenovacion(
    val suscripcion: Subscription,
    val tipo: TipoAviso,
    val fecha: LocalDate,
    val diasRestantes: Int
) {
    /** Clave de deduplicación: un aviso por suscripción, tipo y fecha de cobro. */
    val clave: String get() = claveAviso(suscripcion.id, tipo, fecha)
}

/**
 * Clave persistente "TIPO:id@yyyy-MM-dd". Incluye la fecha del cobro (no la de hoy) para que
 * cada ciclo se avise una sola vez, y el siguiente ciclo vuelva a avisarse.
 */
fun claveAviso(suscripcionId: Long, tipo: TipoAviso, fecha: LocalDate): String =
    "${tipo.name}:$suscripcionId@$fecha"

/**
 * Decide qué avisos tocan hoy. Función pura: sin reloj, sin preferencias, sin Android.
 *
 * Reglas:
 * - Solo suscripciones **activas**.
 * - Prueba gratuita **vigente** (`esPrueba` con `fechaFinPrueba >= hoy`): solo se avisa del fin
 *   de la prueba, no de cobros (el aviso de fin de prueba ya dice cuánto se cobrará después).
 * - Prueba **vencida** o sin fecha de fin: se trata como de pago (si no, la suscripción se quedaba
 *   muda para siempre al acabar la prueba).
 * - Cobros: se calculan con [ocurrencias], la misma proyección que usan la lista y el Inicio, así
 *   que una fecha de renovación pasada avanza de ciclo (mensual, anual, semanal, día 31…).
 * - **Ventana** en lugar de igualdad exacta: se avisa si la fecha cae en `[hoy, hoy + umbral]`.
 *   Así no se pierde el aviso si el trabajo en segundo plano no corrió justo el día "umbral"
 *   (Doze, app recién instalada, suscripción creada tarde, umbral cambiado a uno mayor).
 * - Solo el cobro **más próximo** de cada suscripción: con ciclos cortos y umbral largo (semanal
 *   y 14 días) no se avisa de dos cobros a la vez; el siguiente se avisará cuando sea el próximo.
 * - **Deduplicación**: se omiten las claves de [yaAvisados] ([AvisoRenovacion.clave]).
 *
 * @param umbralDias días de antelación elegidos por el usuario (negativo se trata como 0).
 * @return avisos ordenados por fecha y, a igual fecha, por nombre.
 */
fun avisosPendientes(
    subs: List<Subscription>,
    hoy: LocalDate,
    umbralDias: Int,
    yaAvisados: Set<String>
): List<AvisoRenovacion> {
    val umbral = umbralDias.coerceAtLeast(0)
    val limite = hoy.plus(umbral, DateTimeUnit.DAY)

    return subs.asSequence()
        .filter { it.activa }
        .mapNotNull { sub ->
            val finPrueba = sub.fechaFinPrueba?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
            val pruebaVigente = sub.esPrueba && finPrueba != null && finPrueba >= hoy
            if (pruebaVigente) {
                // finPrueba no es null aquí (pruebaVigente lo garantiza)
                val fin = finPrueba!!
                if (fin <= limite) AvisoRenovacion(sub, TipoAviso.FIN_PRUEBA, fin, hoy.daysUntil(fin)) else null
            } else {
                ocurrencias(sub, hoy, limite).firstOrNull()?.let { fecha ->
                    AvisoRenovacion(sub, TipoAviso.COBRO, fecha, hoy.daysUntil(fecha))
                }
            }
        }
        .filter { it.clave !in yaAvisados }
        .sortedWith(compareBy<AvisoRenovacion> { it.fecha }.thenBy { it.suscripcion.nombre.lowercase() })
        .toList()
}

/**
 * Quita del registro de avisos ya mostrados los de fechas pasadas (ya no pueden volver a salir
 * porque [avisosPendientes] solo mira de hoy en adelante), para que el registro no crezca sin fin.
 * Las claves con formato irreconocible también se descartan.
 */
fun purgarAvisosAntiguos(yaAvisados: Set<String>, hoy: LocalDate): Set<String> =
    yaAvisados.filterTo(mutableSetOf()) { clave ->
        val fecha = runCatching { LocalDate.parse(clave.substringAfterLast('@')) }.getOrNull()
        fecha != null && fecha >= hoy
    }

/**
 * Id de notificación estable por suscripción: el mismo id siempre pisa su propio aviso anterior
 * (y no el de otra suscripción, como pasaba con ids por índice). Pliega los 64 bits en 32.
 */
fun idNotificacion(suscripcionId: Long): Int = (suscripcionId xor (suscripcionId ushr 32)).toInt()

/**
 * Minutos desde [ahora] hasta la próxima [hora]:[minuto] local: hoy si aún no ha llegado, si no
 * mañana. Sirve para anclar el aviso diario a una hora razonable en lugar de a la hora en que se
 * programó por primera vez (que podía ser de madrugada). Ignora cambios de horario (±1 h).
 */
fun minutosHasta(ahora: LocalDateTime, hora: Int, minuto: Int = 0): Long {
    val actual = ahora.hour * 60 + ahora.minute
    val objetivo = hora * 60 + minuto
    val diferencia = objetivo - actual
    return (if (diferencia <= 0) diferencia + MINUTOS_DIA else diferencia).toLong()
}

/** `true` entre las [desde] y las [hasta] (por defecto 22:00–08:00): no se lanzan avisos con sonido. */
fun esHoraDeSilencio(hora: Int, desde: Int = 22, hasta: Int = 8): Boolean = hora >= desde || hora < hasta

private const val MINUTOS_DIA = 24 * 60
