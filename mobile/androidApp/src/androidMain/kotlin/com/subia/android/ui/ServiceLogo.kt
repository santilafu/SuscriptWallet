package com.subia.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Muestra el logo de un servicio con fallback en cascada:
 *  1. iconUrl del catálogo (si existe)
 *  2. icon.horse (gratuito, fiable)
 *  3. Google Favicons (sz=128, muy amplia cobertura)
 *  4. Inicial del nombre sobre fondo de color
 */
@Composable
fun ServiceLogo(
    nombre: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    domain: String? = null,
    iconUrl: String? = null
) {
    val resolvedDomain = domain?.takeIf { it.isNotBlank() } ?: getLogoDomain(nombre)
    val shape = RoundedCornerShape(12.dp)

    if (resolvedDomain.isEmpty() && iconUrl.isNullOrBlank()) {
        LogoFallback(nombre, size, shape, modifier)
        return
    }

    val urls = remember(resolvedDomain, iconUrl) {
        buildList {
            if (!iconUrl.isNullOrBlank()) add(iconUrl)
            if (resolvedDomain.isNotEmpty()) {
                add("https://icon.horse/icon/$resolvedDomain")
                add("https://www.google.com/s2/favicons?domain=$resolvedDomain&sz=128")
            }
        }
    }
    var intentoActual by remember(resolvedDomain) { mutableIntStateOf(0) }

    if (intentoActual >= urls.size) {
        LogoFallback(nombre, size, shape, modifier)
        return
    }

    val context = LocalContext.current
    // El avance al siguiente fallback se hace en el listener onError de Coil
    // (callback fuera de la fase de composición). Hacerlo dentro del slot `error`
    // mutaría estado durante la composición → recomposición en bucle / crash.
    val request = remember(urls, intentoActual) {
        ImageRequest.Builder(context)
            .data(urls[intentoActual])
            .listener(onError = { _, _ -> intentoActual++ })
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = nombre,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color.White),
        loading = { LogoFallback(nombre, size, shape, modifier) },
        error = { LogoFallback(nombre, size, shape, modifier) }
    )
}

@Composable
private fun LogoFallback(
    nombre: String,
    size: Dp,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val letra = nombre.firstOrNull()?.uppercaseChar() ?: '?'
    val color = colorForLetter(letra)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letra.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}

private fun colorForLetter(c: Char): Color {
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFF14B8A6), Color(0xFFF59E0B), Color(0xFF10B981),
        Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFFF97316)
    )
    return colors[c.code % colors.size]
}
