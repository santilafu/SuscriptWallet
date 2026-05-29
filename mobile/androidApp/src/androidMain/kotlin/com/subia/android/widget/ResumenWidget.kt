package com.subia.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.subia.android.MainActivity
import com.subia.android.R
import com.subia.shared.model.DashboardSummary
import kotlinx.serialization.json.Json

private val widgetJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
private val Indigo = Color(0xFF4F46E5)

/**
 * Widget de pantalla de inicio que muestra el gasto mensual y la próxima renovación,
 * leyendo de la misma caché (SharedPreferences "subia_cache") que alimenta el dashboard.
 * Al pulsarlo abre la app.
 */
class ResumenWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val resumen = leerResumen(context)
        provideContent {
            ContenidoWidget(context, resumen)
        }
    }

    private fun leerResumen(context: Context): DashboardSummary? {
        val prefs = context.getSharedPreferences("subia_cache", Context.MODE_PRIVATE)
        val raw = prefs.getString("dashboard_summary", null) ?: return null
        return runCatching { widgetJson.decodeFromString<DashboardSummary>(raw) }.getOrNull()
    }
}

@Composable
private fun ContenidoWidget(context: Context, resumen: DashboardSummary?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Indigo)
            .cornerRadius(16.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = context.getString(R.string.widget_monthly_label),
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp)
        )
        Text(
            text = if (resumen != null) "%.2f €".format(resumen.gastoMensual) else "—",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )
        val proxima = resumen?.renovacionesProximas?.firstOrNull()
        Text(
            text = if (proxima != null)
                context.getString(R.string.widget_next, proxima.nombre, proxima.diasRestantes)
            else
                context.getString(R.string.widget_no_data),
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp)
        )
    }
}

/** Receiver que registra el widget en el sistema. */
class ResumenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ResumenWidget()
}
