package com.subia.android.ui.screens

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import android.Manifest
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.R
import com.subia.android.ui.ServiceLogo
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.theme.GradientBrandDeepEnd
import com.subia.android.ui.theme.GradientBrandDeepMid
import com.subia.android.ui.theme.GradientBrandDeepStart
import com.subia.android.util.NotificacionesPermiso
import kotlinx.coroutines.launch
import com.subia.android.worker.NotificadorAvisos
import com.subia.shared.model.AvisoRenovacion
import com.subia.shared.model.Subscription
import com.subia.shared.model.TipoAviso
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/** Servicio de ejemplo del onboarding: nombre, dominio del logo y precio mensual ilustrativo. */
private data class ServicioEjemplo(val nombre: String, val dominio: String, val precioMes: Double)

/** Página 1: logos reales de lo que la gente ya paga (streaming, IA, luz, teléfono, seguro). */
private val serviciosPortada = listOf(
    ServicioEjemplo("Netflix", "netflix.com", 12.99),
    ServicioEjemplo("Spotify", "spotify.com", 10.99),
    ServicioEjemplo("ChatGPT", "openai.com", 21.99),
    // Naturgy y no Iberdrola: el favicon de iberdrola.es solo existe a 16 px y se veía borroso.
    ServicioEjemplo("Naturgy", "naturgy.es", 48.0),
    ServicioEjemplo("Movistar", "movistar.es", 39.9),
    ServicioEjemplo("Mapfre", "mapfre.es", 27.5)
)

/** Página 2: la suma de cuatro servicios corrientes. 47,96 €/mes → 575,52 €/año. */
private val serviciosGasto = listOf(
    ServicioEjemplo("Netflix", "netflix.com", 12.99),
    ServicioEjemplo("Spotify", "spotify.com", 10.99),
    ServicioEjemplo("ChatGPT", "openai.com", 21.99),
    ServicioEjemplo("iCloud", "icloud.com", 1.99)
)
private val GASTO_EJEMPLO_MES = serviciosGasto.sumOf { it.precioMes }

private const val PAGINAS = 3

/** Curva ease-out marcada: arranca rápido y frena suave (la cifra "aterriza"). */
private val EaseOutFuerte = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/**
 * Onboarding de tres páginas que se muestra una sola vez tras el primer login (y desde
 * Ajustes › "Ver el tutorial de nuevo"):
 *
 * 1. Valor: logos reales de servicios que ya se pagan, no iconos genéricos.
 * 2. Gasto: una tarjeta como la del Dashboard cuya cifra sube de 0 al ejemplo.
 * 3. Avisos: la notificación que recibirán y el permiso del sistema, pedido en contexto.
 *
 * Las animaciones respetan `ANIMATOR_DURATION_SCALE` (ajustes de accesibilidad / desarrollador):
 * con escala 0 los valores se fijan al instante.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { PAGINAS })
    val scope = rememberCoroutineScope()
    val esUltima = pagerState.currentPage == PAGINAS - 1
    val animar = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    // Permiso de notificaciones (Android 13+): un único botón que abre el diálogo del sistema.
    // Concedido o no, el onboarding termina: la app funciona igual sin avisos.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onFinish()
    }
    // Con el permiso ya concedido (p. ej. al repetir el tutorial desde Ajustes) el botón dice
    // "Empezar" y no hay "Ahora no": ofrecer "Activar avisos" ya activados confundía.
    val hayQuePedirAvisos = remember { NotificacionesPermiso.hayQuePedir(context) }
    val activarAvisos = {
        if (NotificacionesPermiso.hayQuePedir(context)) {
            NotificacionesPermiso.marcarPedidoAlGuardar(context)
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onFinish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // Saltar: siempre visible salvo en la última página, que ya ofrece "Ahora no".
        Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End) {
            if (!esUltima) {
                TextButton(onClick = onFinish) { Text(stringResource(R.string.onb_skip)) }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (page) {
                    0 -> PaginaLogos(animar = animar)
                    1 -> PaginaGasto(activa = pagerState.currentPage == 1, animar = animar)
                    else -> PaginaAvisos()
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    text = stringResource(tituloDePagina(page)),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(descripcionDePagina(page)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        IndicadoresDePagina(actual = pagerState.currentPage, total = PAGINAS)

        // Botón principal SIEMPRE en el mismo sitio y zona secundaria de alto fijo: al pasar
        // de página no saltan ni los puntos ni el botón (antes subían 72 dp en la última).
        Button(
            onClick = {
                if (esUltima) activarAvisos()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(
                    when {
                        !esUltima -> R.string.onb_next
                        hayQuePedirAvisos -> R.string.onb_notif_enable
                        else -> R.string.onb_start
                    }
                ),
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            if (esUltima && hayQuePedirAvisos) {
                TextButton(onClick = onFinish) { Text(stringResource(R.string.onb_notif_later)) }
            }
        }
    }
}

private fun tituloDePagina(page: Int) = when (page) {
    0 -> R.string.onb1_title
    1 -> R.string.onb2_title
    else -> R.string.onb3_title
}

private fun descripcionDePagina(page: Int) = when (page) {
    0 -> R.string.onb1_desc
    1 -> R.string.onb2_desc
    else -> R.string.onb3_desc
}

/** Seis logos reales en dos filas, apareciendo en cascada (50 ms entre cada uno). */
@Composable
private fun PaginaLogos(animar: Boolean) {
    var visible by remember { mutableStateOf(!animar) }
    LaunchedEffect(Unit) { visible = true }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        serviciosPortada.chunked(3).forEachIndexed { fila, grupo ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                grupo.forEachIndexed { col, servicio ->
                    val indice = fila * 3 + col
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(220, delayMillis = indice * 50)) +
                            scaleIn(tween(220, delayMillis = indice * 50), initialScale = 0.92f)
                    ) {
                        // Sin tinte tonal: el lavanda del tinte enmarcaba el fondo blanco del
                        // logo y se veían dos marcos, uno dentro de otro.
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            shadowElevation = 2.dp
                        ) {
                            Box(Modifier.padding(10.dp)) {
                                ServiceLogo(
                                    nombre = servicio.nombre,
                                    domain = servicio.dominio,
                                    size = 56.dp,
                                    contentDescription = servicio.nombre
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de gasto propia del onboarding (mismo lenguaje visual que la del Dashboard, sin
 * depender de ella). La cifra sube de 0 al ejemplo cuando la página pasa a ser la actual.
 */
@Composable
private fun PaginaGasto(activa: Boolean, animar: Boolean) {
    val cifra = remember { Animatable(0f) }
    LaunchedEffect(activa) {
        if (activa) {
            if (animar) cifra.animateTo(GASTO_EJEMPLO_MES.toFloat(), tween(900, easing = EaseOutFuerte))
            else cifra.snapTo(GASTO_EJEMPLO_MES.toFloat())
        }
    }
    val gradiente = remember { Brush.linearGradient(listOf(GradientBrandDeepStart, GradientBrandDeepMid, GradientBrandDeepEnd)) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(gradiente)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.onb2_card_label),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatearImporte(cifra.value.toDouble(), "EUR"),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    lineHeight = 44.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.onb2_card_footer,
                        serviciosGasto.size,
                        formatearImporte(GASTO_EJEMPLO_MES * 12, "EUR")
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        // Los servicios que suman la cifra: la tarjeta no es un número abstracto.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            serviciosGasto.forEach { servicio ->
                ServiceLogo(
                    nombre = servicio.nombre,
                    domain = servicio.dominio,
                    size = 40.dp,
                    contentDescription = servicio.nombre
                )
            }
        }
    }
}

/** Página 3: la notificación tal y como la verán, en lugar de un icono de campana. */
@Composable
private fun PaginaAvisos() {
    // Mismos textos que la notificación real (NotificadorAvisos): cobro dentro de 3 días.
    val context = LocalContext.current
    val ejemplo = remember(context) {
        val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val fecha = hoy.plus(3, DateTimeUnit.DAY)
        val sub = Subscription(
            nombre = "Netflix", precio = 12.99, moneda = "EUR",
            periodoFacturacion = "MONTHLY", fechaRenovacion = fecha.toString()
        )
        NotificadorAvisos.textos(context, AvisoRenovacion(sub, TipoAviso.COBRO, fecha, 3))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ServiceLogo(nombre = "Netflix", domain = "netflix.com", size = 44.dp, contentDescription = null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ejemplo.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = ejemplo.texto,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Indicadores con semántica "Página X de N" para TalkBack (los puntos son decorativos). */
@Composable
private fun IndicadoresDePagina(actual: Int, total: Int) {
    val descripcion = stringResource(R.string.onb_page_of, actual + 1, total)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .semantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(total) { i ->
            val seleccionado = actual == i
            val ancho by animateDpAsState(if (seleccionado) 24.dp else 8.dp, label = "dot")
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = ancho, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (seleccionado) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
