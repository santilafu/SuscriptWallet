package com.subia.android.ui.theme

import androidx.compose.ui.graphics.Color

// ── Paleta base SubIA — dark-first, alineada con el logo 2026 ─────────────────
// El logo es un degradado rosa → violeta → azul con monedas ámbar: el violeta es el
// acento primario y el degradado completo se reserva para las tarjetas destacadas.

// Fondos: neutros con un punto de violeta para que no choquen con el logo
val BackgroundDark  = Color(0xFF0B0A12)   // fondo principal
val Surface900      = Color(0xFF17151F)   // superficies / tarjetas
val Surface800      = Color(0xFF262330)   // bordes sutiles / dividers
val Surface700      = Color(0xFF3D3949)   // outline

// Acento violeta (el color dominante del logo)
val Violet600       = Color(0xFF7C3AED)   // violet-600 — primario en claro (5,7:1 sobre blanco)
val Violet400       = Color(0xFFA78BFA)   // violet-400 — primario en oscuro
val Violet800       = Color(0xFF5B21B6)   // violet-800 — contenedor primario dark
val Violet200       = Color(0xFFDDD6FE)   // violet-200 — on-primary container dark

// Acentos secundarios del logo
val Pink600         = Color(0xFFDB2777)   // pink-600 — secundario en claro
val Pink400         = Color(0xFFF472B6)   // pink-400 — secundario en oscuro
val Sky600          = Color(0xFF0284C7)   // sky-600 — terciario en claro
val Sky400          = Color(0xFF38BDF8)   // sky-400 — terciario en oscuro

// Semánticos (modo oscuro). Sobre blanco no llegan a 4,5:1 → usar las variantes *OnLight
// en claro a través de MaterialTheme.colorScheme.success / warning / urgent (Theme.kt).
val Success         = Color(0xFF22C55E)
val Warning         = Color(0xFFF59E0B)
val Urgent          = Color(0xFFF97316)   // orange-500 — pruebas por vencer, "ir a cancelar"
val Error           = Color(0xFFEF4444)

// Semánticos para texto sobre fondo claro (≈5:1 sobre blanco)
val SuccessOnLight  = Color(0xFF15803D)   // green-700
val WarningOnLight  = Color(0xFFB45309)   // amber-700
val UrgentOnLight   = Color(0xFFC2410C)   // orange-700

// Texto
val TextPrimary     = Color(0xFFFAFAFA)   // zinc-50
val TextSecondary   = Color(0xFFA1A1AA)   // zinc-400

// Fondo light mode
val BackgroundLight = Color(0xFFFAF9FD)   // blanco con un toque violeta
val SurfaceLight    = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF3F1F8)

// ── Gradientes — pares de colores con nombre canónico ─────────────────────────

// Degradado de marca (el del logo): rosa → violeta → azul
val GradientBrandStart  = Color(0xFFEC4899)   // pink-500
val GradientBrandMid    = Color(0xFF8B5CF6)   // violet-500
val GradientBrandEnd    = Color(0xFF3B82F6)   // blue-500

val GradientTealStart   = Color(0xFF0D9488)   // teal-600
val GradientTealEnd     = Color(0xFF0891B2)   // cyan-600

val GradientAmberStart  = Color(0xFFD97706)   // amber-600
val GradientAmberEnd    = Color(0xFFDC2626)   // red-600

// Gradientes "profundos" para tarjetas con texto blanco encima (≥4,5:1 con blanco al 100 %)
val GradientBrandDeepStart  = Color(0xFFDB2777)   // pink-600   (4,6:1 con blanco)
val GradientBrandDeepMid    = Color(0xFF7C3AED)   // violet-600 (5,7:1)
val GradientBrandDeepEnd    = Color(0xFF2563EB)   // blue-600   (5,2:1)

val GradientTealDeepStart   = Color(0xFF0F766E)   // teal-700
val GradientTealDeepEnd     = Color(0xFF0E7490)   // cyan-700
