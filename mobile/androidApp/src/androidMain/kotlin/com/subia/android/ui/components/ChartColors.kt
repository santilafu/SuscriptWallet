package com.subia.android.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
