package com.subia.android.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Movimiento del Inicio. Reglas comunes:
 * - Solo se anima `alpha`, `translationY` y `scale` en `graphicsLayer` (o se dibuja en
 *   `drawBehind`), leyendo el valor animado DENTRO de la lambda: el frame solo repinta esa capa,
 *   no recompone la pantalla.
 * - Nada en bucle: todo termina y lo de "la primera vez" se apunta en [MovimientoUnaVez].
 * - "Quitar animaciones" del sistema (escala de animación 0): Compose ya lo respeta en sus
 *   animaciones (la escala le llega como `MotionDurationScale` y con 0 salta al valor final),
 *   pero un `delay` de escalonado no se escala y el primer frame se pintaría en el estado
 *   inicial (invisible o desplazado). Por eso, además, [animacionesActivas] lee
 *   `ANIMATOR_DURATION_SCALE` y con 0 todo nace ya en su sitio.
 */

/** ease-out fuerte, cubic-bezier(0.23, 1, 0.32, 1): arranca rápido y frena suave. */
val EaseOutFuerte = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** Pulsación: baja rápido (respuesta inmediata) y vuelve algo más despacio. */
private const val PULSAR_BAJA_MS = 100
private const val PULSAR_SUBE_MS = 160

/** Escala de las superficies tocables mientras se pulsan. */
const val ESCALA_PULSADO = 0.97f

/** `false` si el sistema tiene las animaciones desactivadas (escala de duración 0). */
@Composable
fun animacionesActivas(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

/**
 * Animaciones que solo se ven una vez por proceso (la entrada del Inicio, el crecimiento del
 * gráfico): ni al volver atrás, ni al cambiar de pestaña, ni al girar la pantalla. No es estado de
 * Compose (no provoca recomposiciones); el proceso nuevo vuelve a mostrarlas una vez.
 */
object MovimientoUnaVez {
    private val hechas = mutableSetOf<String>()

    /** `true` la primera vez que se pide [clave] y, a partir de ahí, `false`. */
    fun reclamar(clave: String): Boolean = synchronized(hechas) { hechas.add(clave) }
}

/**
 * Escala animada para una superficie pulsable: [ESCALA_PULSADO] mientras [interaccion] está
 * pulsada. Aplicar con [escalaDiferida] para que el valor se lea en la fase de dibujo.
 */
@Composable
fun rememberEscalaPulsacion(interaccion: InteractionSource): State<Float> {
    val pulsado by interaccion.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pulsado) ESCALA_PULSADO else 1f,
        animationSpec = tween(if (pulsado) PULSAR_BAJA_MS else PULSAR_SUBE_MS, easing = EaseOutFuerte),
        label = "escalaPulsacion"
    )
}

/** Escala uniforme leída en la capa gráfica (sin recomponer ni volver a medir). */
fun Modifier.escalaDiferida(escala: State<Float>): Modifier = graphicsLayer {
    val s = escala.value
    scaleX = s
    scaleY = s
}

/** Escalonado de la entrada: retraso entre una tarjeta y la siguiente, y tope. */
private const val ESCALONADO_MS = 45L
private const val ESCALONADO_MAX_PASOS = 6
private const val ENTRADA_MS = 320

/**
 * Entrada suave de una tarjeta del Inicio: aparece (alpha 0 → 1) subiendo 12 dp, con un
 * retraso de [ESCALONADO_MS] por posición. Si [animar] es `false` se pinta directamente en su
 * sitio. El valor se lee en `graphicsLayer`: durante la entrada no se recompone nada.
 */
@Composable
fun EntradaEscalonada(indice: Int, animar: Boolean, content: @Composable () -> Unit) {
    // Se decide al nacer: si [animar] cambia después, la entrada ya empezada no se corta.
    val animarAlNacer = remember { animar }
    val progreso = remember { Animatable(if (animarAlNacer) 0f else 1f) }
    val desplazamiento = with(LocalDensity.current) { 12.dp.toPx() }
    if (animarAlNacer) {
        LaunchedEffect(Unit) {
            delay(indice.coerceAtMost(ESCALONADO_MAX_PASOS) * ESCALONADO_MS)
            progreso.animateTo(1f, tween(ENTRADA_MS, easing = EaseOutFuerte))
        }
    }
    Box(
        Modifier.graphicsLayer {
            val p = progreso.value
            alpha = p
            translationY = (1f - p) * desplazamiento
        }
    ) { content() }
}
