package com.subia.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.subia.android.R

/**
 * Familia tipográfica de la app: Plus Jakarta Sans (fuente variable, OFL).
 *
 * Se declara cada peso apuntando al mismo archivo variable y fijando el eje `wght`
 * vía [FontVariation]. Requiere API 26+ (minSdk del proyecto = 26), donde el sistema
 * aplica los ejes variables; por debajo Compose haría fallback al peso por defecto.
 */
@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val PlusJakarta = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold)
)

/**
 * Escala tipográfica M3 con Plus Jakarta Sans. Parte de la [Typography] por defecto
 * (tamaños y line-heights estándar de Material 3) y solo sustituye la familia y, en los
 * niveles de cabecera, el peso — para dar una jerarquía con más carácter "fintech".
 */
private val base = Typography()

val SubIATypography = Typography(
    displayLarge   = base.displayLarge.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.Bold),
    displayMedium  = base.displayMedium.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.Bold),
    displaySmall   = base.displaySmall.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.Bold),
    headlineLarge  = base.headlineLarge.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold),
    headlineSmall  = base.headlineSmall.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold),
    titleLarge     = base.titleLarge.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold),
    titleMedium    = base.titleMedium.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold),
    titleSmall     = base.titleSmall.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.Medium),
    bodyLarge      = base.bodyLarge.copy(fontFamily = PlusJakarta),
    bodyMedium     = base.bodyMedium.copy(fontFamily = PlusJakarta),
    bodySmall      = base.bodySmall.copy(fontFamily = PlusJakarta),
    labelLarge     = base.labelLarge.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.Medium),
    labelMedium    = base.labelMedium.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.Medium),
    labelSmall     = base.labelSmall.copy(fontFamily = PlusJakarta, fontWeight = FontWeight.Medium)
)
