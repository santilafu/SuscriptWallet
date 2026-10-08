package com.subia.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary            = Violet400,
    onPrimary          = BackgroundDark,
    primaryContainer   = Violet800,
    onPrimaryContainer = Violet200,
    secondary          = Pink400,
    onSecondary        = BackgroundDark,
    tertiary           = Sky400,
    onTertiary         = BackgroundDark,
    background         = BackgroundDark,
    onBackground       = TextPrimary,
    surface            = Surface900,
    onSurface          = TextPrimary,
    surfaceVariant     = Surface800,
    onSurfaceVariant   = TextSecondary,
    outline            = Surface700,
    error              = Error
)

private val LightColors = lightColorScheme(
    primary            = Violet600,
    onPrimary          = SurfaceLight,
    primaryContainer   = Color(0xFFEDE9FE),   // violet-100
    onPrimaryContainer = Violet800,
    secondary          = Pink600,
    onSecondary        = SurfaceLight,
    tertiary           = Sky600,
    onTertiary         = SurfaceLight,
    background         = BackgroundLight,
    onBackground       = Surface900,
    surface            = SurfaceLight,
    onSurface          = Surface900,
    surfaceVariant     = SurfaceVariantLight,
    onSurfaceVariant   = Surface700,
    outline            = Surface800,
    error              = Error
)

// ── Colores semánticos según tema ─────────────────────────────────────────────
// En oscuro se mantienen los tonos 500 de siempre; en claro se usan los tonos 700
// para que el texto y los chips cumplan 4,5:1 sobre blanco / zinc-50.

/** Verde semántico (activa, ahorro). */
val ColorScheme.success: Color
    @Composable get() = if (isSystemInDarkTheme()) Success else SuccessOnLight

/** Ámbar semántico (contador, renovación en ≤7 días). */
val ColorScheme.warning: Color
    @Composable get() = if (isSystemInDarkTheme()) Warning else WarningOnLight

/** Naranja semántico (pruebas por vencer, ir a cancelar). */
val ColorScheme.urgent: Color
    @Composable get() = if (isSystemInDarkTheme()) Urgent else UrgentOnLight

private val SubIAShapes = Shapes(
    small  = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large  = RoundedCornerShape(24.dp)
)

/**
 * Tema Material 3 de SubIA — dark-first, bordes redondeados generosos,
 * tipografía Plus Jakarta Sans.
 *
 * @param dynamicColor si es `true` y el dispositivo es Android 12+ (S), usa la paleta
 *   Material You derivada del fondo de pantalla del usuario. Por defecto está desactivado
 *   para preservar la identidad violeta de la marca (logo 2026); se puede exponer como preferencia.
 */
@Composable
fun SubIATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes      = SubIAShapes,
        typography  = SubIATypography,
        content     = content
    )
}
