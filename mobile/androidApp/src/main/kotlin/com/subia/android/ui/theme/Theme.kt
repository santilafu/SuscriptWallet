package com.subia.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Indigo400,
    onPrimary = Surface900,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo200,
    background = Surface900,
    onBackground = TextPrimary,
    surface = Surface800,
    onSurface = TextPrimary,
    surfaceVariant = Surface700,
    onSurfaceVariant = TextSecondary,
    error = Error
)

private val LightColors = lightColorScheme(
    primary = Indigo600,
    onPrimary = TextPrimary,
    primaryContainer = Indigo200,
    onPrimaryContainer = Indigo700,
    background = TextPrimary,
    onBackground = Surface900,
    surface = TextPrimary,
    onSurface = Surface900,
    surfaceVariant = Indigo200,
    onSurfaceVariant = Surface700,
    error = Error
)

/** Tema Material 3 de SubIA con soporte dark/light. */
@Composable
fun SubIATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
