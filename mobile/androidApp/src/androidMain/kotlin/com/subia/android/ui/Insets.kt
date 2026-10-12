package com.subia.android.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

/**
 * Añade el recorte lateral de la pantalla (notch en horizontal) a unos insets.
 *
 * Con edge-to-edge la ventana se extiende bajo el recorte (layoutInDisplayCutoutMode=ALWAYS,
 * obligatorio en Android 15+ con targetSdk 35) y los insets por defecto de Material 3
 * (`ScaffoldDefaults.contentWindowInsets`, `TopAppBarDefaults.windowInsets`,
 * `NavigationBarDefaults.windowInsets`) solo contemplan `systemBars`, así que el contenido y los
 * iconos de las barras quedaban bajo el recorte. Al unir solo los lados horizontales las barras
 * siguen pintando hasta el borde y en vertical no cambia nada (ahí el recorte ya lo cubre la
 * barra de estado).
 */
@Composable
fun WindowInsets.conRecorteLateral(): WindowInsets =
    union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
