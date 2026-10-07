package com.subia.android.ui.theme

import androidx.compose.ui.graphics.Color

// ── Paleta base SubIA — dark-first fintech 2025 ──────────────────────────────

// Fondos
val BackgroundDark  = Color(0xFF09090B)   // zinc-950 — fondo principal
val Surface900      = Color(0xFF18181B)   // zinc-900 — superficies / tarjetas
val Surface800      = Color(0xFF27272A)   // zinc-800 — bordes sutiles / dividers
val Surface700      = Color(0xFF3F3F46)   // zinc-700 — outline

// Acento índigo
val Indigo500       = Color(0xFF6366F1)   // indigo-500 — acento primario (light mode)
val Indigo400       = Color(0xFF818CF8)   // indigo-400 — acento primario (dark mode)
val Indigo700       = Color(0xFF4338CA)   // indigo-700 — contenedor primario dark
val Indigo200       = Color(0xFFC7D2FE)   // indigo-200 — on-primary container dark

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
val BackgroundLight = Color(0xFFFAFAFA)
val SurfaceLight    = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF4F4F5)  // zinc-100

// ── Gradientes — pares de colores con nombre canónico ─────────────────────────

val GradientIndigoStart = Color(0xFF6366F1)   // indigo-500
val GradientIndigoEnd   = Color(0xFF8B5CF6)   // violet-500

val GradientTealStart   = Color(0xFF0D9488)   // teal-600
val GradientTealEnd     = Color(0xFF0891B2)   // cyan-600

val GradientAmberStart  = Color(0xFFD97706)   // amber-600
val GradientAmberEnd    = Color(0xFFDC2626)   // red-600

// Gradientes "profundos" para tarjetas con texto blanco encima (≥4,5:1 con blanco al 100 %)
val GradientIndigoDeepStart = Color(0xFF4F46E5)   // indigo-600
val GradientIndigoDeepEnd   = Color(0xFF7C3AED)   // violet-600

val GradientTealDeepStart   = Color(0xFF0F766E)   // teal-700
val GradientTealDeepEnd     = Color(0xFF0E7490)   // cyan-700
