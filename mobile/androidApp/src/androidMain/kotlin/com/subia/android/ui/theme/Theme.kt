package com.subia.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary            = Indigo400,
    onPrimary          = BackgroundDark,
    primaryContainer   = Indigo700,
    onPrimaryContainer = Indigo200,
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
    primary            = Indigo500,
    onPrimary          = SurfaceLight,
    primaryContainer   = Indigo200,
    onPrimaryContainer = Indigo700,
    background         = BackgroundLight,
    onBackground       = Surface900,
    surface            = SurfaceLight,
    onSurface          = Surface900,
    surfaceVariant     = SurfaceVariantLight,
    onSurfaceVariant   = Surface700,
    outline            = Surface800,
    error              = Error
)

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
 *   para preservar la identidad índigo de la marca; se puede exponer como preferencia.
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
