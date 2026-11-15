package com.subia.android.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Colores de datos del Dashboard. Forma "énfasis" de la skill dataviz: un único acento
 * (el `primary` del tema, índigo o Material You) para el dato que importa y un gris de
 * de-énfasis para el resto. Nunca se usa el ámbar de aviso ni el rojo de error para datos.
 *
 * Validado con `validate_palette.js` sobre las superficies reales (#FFFFFF / #18181B):
 * contraste de marca ≥ 3:1 y separación CVD ΔE ≈ 20 en ambos modos.
 */
object ChartColors {
    /** Marca enfatizada (mes actual / seleccionado, barras de categoría). */
    val acento: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    /** Marcas de contexto (el resto de meses): gris suave para que el mes elegido mande; con 0,6 la pared de barras grises dominaba la tarjeta. */
    val neutro: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    /** Pista vacía bajo una barra / línea base: un paso fuera de la superficie. */
    val pista: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
}

/** Hasta dónde crecen las etiquetas de los gráficos con la fuente grande del sistema. */
internal const val ESCALA_MAXIMA_ETIQUETAS = 1.2f

/**
 * Tamaño de las etiquetas pequeñas de los gráficos (meses, importe sobre la barra, importe del
 * día): siguen a la fuente del sistema solo hasta 1,2×. Con fuente 1,5× los 12 meses se
 * montaban ("NovDicEne…") y los importes no cabían en su columna. La cifra completa sigue en
 * TalkBack y en la lista de cobros de debajo, que sí crecen sin límite.
 */
@Composable
fun tamEtiquetaGrafico(base: Float): TextUnit = tamConTope(base, ESCALA_MAXIMA_ETIQUETAS)

/**
 * Tamaño [base] (sp) que sigue a la fuente del sistema solo hasta [tope]×. Para textos que viven
 * en una caja de ancho fijo (columna de la tira, círculo del día, etiqueta de la barra inferior).
 */
@Composable
fun tamConTope(base: Float, tope: Float): TextUnit {
    val escala = LocalDensity.current.fontScale
    return (base * minOf(escala, tope) / escala).sp
}

/** Escala de la fuente del sistema limitada a [1, tope]: para que una caja crezca con su texto topado. */
@Composable
fun escalaFuenteConTope(tope: Float): Float = LocalDensity.current.fontScale.coerceIn(1f, tope)
