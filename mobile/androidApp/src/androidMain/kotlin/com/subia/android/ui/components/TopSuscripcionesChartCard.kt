package com.subia.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.subia.shared.viewmodel.TopSuscripcion
import kotlin.math.min

@Composable
fun TopSuscripcionesChartCard(
    topSuscripciones: List<TopSuscripcion>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top 5 gastos mensuales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (topSuscripciones.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Añade suscripciones para ver estadísticas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Spacer(modifier = Modifier.height(16.dp))

            val moneda = topSuscripciones.first().moneda
            val modelProducer = remember { CartesianChartModelProducer() }

            LaunchedEffect(topSuscripciones) {
                modelProducer.runTransaction {
                    columnSeries {
                        series(topSuscripciones.map { it.gastoMensual })
                    }
                }
            }

            val nombresEjeX = topSuscripciones.map { abreviar(it.nombre) }
            val xFormatter = CartesianValueFormatter { _, value, _ ->
                val idx = value.toInt()
                if (idx in nombresEjeX.indices) nombresEjeX[idx] else ""
            }
            val yFormatter = CartesianValueFormatter { _, value, _ ->
                "%.0f %s".format(value, moneda)
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter)
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
    }
}

private fun abreviar(nombre: String): String {
    val limpio = nombre.trim()
    return limpio.substring(0, min(limpio.length, 10))
}
