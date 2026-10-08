package com.subia.android.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.subia.android.MainActivity
import com.subia.android.R
import com.subia.android.ui.components.formatearImporte
import com.subia.android.ui.theme.BackgroundDark
import com.subia.android.ui.theme.Violet200
import com.subia.android.ui.theme.Violet400
import com.subia.android.ui.theme.Violet600
import com.subia.android.ui.theme.Violet800
import com.subia.android.ui.theme.Surface700
import com.subia.android.ui.theme.Surface800
import com.subia.android.ui.theme.Surface900
import com.subia.android.ui.theme.SurfaceLight
import com.subia.android.ui.theme.SurfaceVariantLight
import com.subia.android.ui.theme.TextPrimary
import com.subia.android.ui.theme.TextSecondary
import com.subia.shared.model.CobroPrevisto
import com.subia.shared.model.DashboardSummary
import com.subia.shared.model.Subscription
import com.subia.shared.model.calcularProyeccionCobros
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

private val widgetJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

private const val PREFS = "subia_cache"
private const val KEY_SUMMARY = "dashboard_summary"
private const val KEY_SUBS = "dashboard_subscriptions"
private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
private const val DIAS_WIDGET = 7

/** Lo que pinta el widget: total previsto del mes y cobros de los próximos 7 días. */
private data class DatosWidget(
    val totalMes: String?,
    val proximos: List<CobroPrevisto>,
    val hoy: LocalDate
)

/**
 * Widget de pantalla de inicio: total que viene este mes y el próximo cobro (más cuántos
 * quedan en la semana), calculados con la misma proyección que la tira del Dashboard a partir
 * de la caché de suscripciones ("subia_cache"). Colores del tema (claro/oscuro y Material You
 * si el usuario lo activó) vía [GlanceTheme] (W-01). Al pulsarlo abre la app en Inicio.
 */
class ResumenWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Lectura de SharedPreferences + parseo JSON + proyección fuera del hilo principal.
        val datos = withContext(Dispatchers.IO) { leerDatos(context) }
        val dinamico = withContext(Dispatchers.IO) { materialYouActivado(context) }
        provideContent {
            // Material You si el usuario lo activó en Ajustes (Android 12+); si no, la paleta índigo de la app.
            val colores = if (dinamico && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) GlanceTheme.colors else ColoresMarca
            GlanceTheme(colors = colores) {
                ContenidoWidget(context, datos)
            }
        }
    }

    private fun leerDatos(context: Context): DatosWidget? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val subs = prefs.getString(KEY_SUBS, null)
            ?.let { raw -> runCatching { widgetJson.decodeFromString<List<Subscription>>(raw) }.getOrNull() }
        if (subs != null) {
            val proyeccion = calcularProyeccionCobros(subs, hoy)
            val total = proyeccion.totalMesActual.entries.firstOrNull()
            return DatosWidget(
                totalMes = total?.let { formatearImporte(it.value, it.key) }
                    ?: formatearImporte(0.0, subs.firstOrNull()?.moneda ?: "EUR"),
                proximos = proyeccion.dias.take(DIAS_WIDGET).flatMap { it.cobros },
                hoy = hoy
            )
        }

        // Sin caché de suscripciones (instalación recién abierta): resumen del servidor si existe.
        val resumen = prefs.getString(KEY_SUMMARY, null)
            ?.let { raw -> runCatching { widgetJson.decodeFromString<DashboardSummary>(raw) }.getOrNull() }
            ?: return null
        return DatosWidget(
            totalMes = formatearImporte(resumen.gastoMensual, "EUR"),
            proximos = emptyList(),
            hoy = hoy
        )
    }

    private fun materialYouActivado(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DYNAMIC_COLOR, false)
}

/** Misma paleta que `SubIATheme` (Theme.kt mantiene sus esquemas privados; se replican aquí los tokens usados). */
private val ColoresMarca = ColorProviders(
    light = lightColorScheme(
        primary = Violet600, onPrimary = SurfaceLight,
        primaryContainer = Violet200, onPrimaryContainer = Violet800,
        surface = SurfaceLight, onSurface = Surface900,
        surfaceVariant = SurfaceVariantLight, onSurfaceVariant = Surface700
    ),
    dark = darkColorScheme(
        primary = Violet400, onPrimary = BackgroundDark,
        primaryContainer = Violet800, onPrimaryContainer = Violet200,
        surface = Surface900, onSurface = TextPrimary,
        surfaceVariant = Surface800, onSurfaceVariant = TextSecondary
    )
)

@Composable
private fun ContenidoWidget(context: Context, datos: DatosWidget?) {
    val colores = GlanceTheme.colors
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colores.primaryContainer)
            .cornerRadius(16.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = context.getString(R.string.widget_monthly_label),
            style = TextStyle(color = colores.onPrimaryContainer, fontSize = 12.sp)
        )
        Text(
            text = datos?.totalMes ?: "—",
            style = TextStyle(
                color = colores.onPrimaryContainer,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(GlanceModifier.height(4.dp))

        val proximo = datos?.proximos?.firstOrNull()
        val linea = when {
            datos == null -> context.getString(R.string.widget_no_data)
            proximo == null -> context.getString(R.string.widget_no_charges_week)
            else -> context.getString(
                R.string.widget_next,
                "${proximo.nombre} · ${formatearImporte(proximo.importe, proximo.moneda)}",
                textoDiasRelativo(context, datos.hoy.daysUntil(proximo.fecha))
            )
        }
        Text(
            text = linea,
            style = TextStyle(color = colores.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
        val restantes = (datos?.proximos?.size ?: 0) - 1
        if (restantes > 0) {
            Text(
                text = context.resources.getQuantityString(R.plurals.widget_more_this_week, restantes, restantes),
                style = TextStyle(color = colores.onPrimaryContainer, fontSize = 11.sp),
                maxLines = 1
            )
        }
    }
}

/** "Hoy", "Mañana", "En 3 días" fuera de Compose (recursos del contexto). */
private fun textoDiasRelativo(context: Context, dias: Int): String = when {
    dias <= 0 -> context.getString(R.string.day_today)
    dias == 1 -> context.getString(R.string.day_tomorrow)
    else -> context.resources.getQuantityString(R.plurals.in_n_days, dias, dias)
}

/** Repinta todas las instancias del widget (se llama cuando el Dashboard recibe datos frescos). */
suspend fun actualizarWidgetResumen(context: Context) {
    runCatching { ResumenWidget().updateAll(context.applicationContext) }
}

/** Receiver que registra el widget en el sistema. */
class ResumenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ResumenWidget()
}
