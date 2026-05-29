package com.subia.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.R
import com.subia.android.ui.theme.GradientIndigoEnd
import com.subia.android.ui.theme.GradientIndigoStart
import com.subia.android.util.ResumenCompartible
import com.subia.android.util.compartirResumenAnual
import com.subia.shared.viewmodel.DashboardUiState
import com.subia.shared.viewmodel.DashboardViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Pantalla "Tu año en suscripciones": muestra una tarjeta-resumen (gasto anual, nº de
 * suscripciones, servicio más caro y categoría con más gasto) y permite compartirla como
 * imagen — palanca de adquisición orgánica / boca a boca.
 *
 * La imagen compartida se genera con Canvas nativo (ver [compartirResumenAnual]); la tarjeta
 * Compose de esta pantalla es la vista previa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenAnualScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalesAnuales by viewModel.totalesAnualesPorMoneda.collectAsState()
    val tops by viewModel.topSuscripciones.collectAsState()
    val gastosCat by viewModel.gastosPorCategoria.collectAsState()

    val numSubs = (uiState as? DashboardUiState.Success)?.resumen?.totalSuscripciones ?: tops.size
    val entradaAnual = totalesAnuales.entries.firstOrNull()
    val moneda = entradaAnual?.key ?: "EUR"
    val totalAnual = entradaAnual?.value ?: 0.0
    val servicioTop = tops.firstOrNull()?.nombre
    // Las claves de gastosPorCategoria vienen como "Nombre (EUR)"; quitamos el sufijo de divisa.
    val categoriaTop = gastosCat.keys.firstOrNull()?.substringBeforeLast(" (")

    val hayDatos = totalAnual > 0.0
    val simbolo = if (moneda == "EUR") "€" else moneda
    val totalValor = "%,.0f %s".format(totalAnual, simbolo)

    val context = LocalContext.current
    val titulo = stringResource(R.string.resumen_anual_title)
    val totalLabel = stringResource(R.string.resumen_anual_total_label)
    val subsTexto = stringResource(R.string.resumen_anual_subs, numSubs)
    val servicioLabel = stringResource(R.string.resumen_anual_top_service)
    val categoriaLabel = stringResource(R.string.resumen_anual_top_category)
    val marca = stringResource(R.string.resumen_anual_brand)
    val tituloSelector = stringResource(R.string.resumen_anual_share)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hayDatos) {
                Spacer(Modifier.height(48.dp))
                Text(
                    text = stringResource(R.string.resumen_anual_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                return@Column
            }

            TarjetaResumen(
                titulo = titulo,
                totalLabel = totalLabel,
                totalValor = totalValor,
                subsTexto = subsTexto,
                servicioLabel = servicioLabel,
                servicioTop = servicioTop,
                categoriaLabel = categoriaLabel,
                categoriaTop = categoriaTop,
                marca = marca
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    compartirResumenAnual(
                        context = context,
                        datos = ResumenCompartible(
                            titulo = titulo,
                            totalLabel = totalLabel,
                            totalValor = totalValor,
                            subsTexto = subsTexto,
                            servicioLabel = servicioLabel,
                            servicioValor = servicioTop,
                            categoriaLabel = categoriaLabel,
                            categoriaValor = categoriaTop,
                            marca = marca
                        ),
                        tituloSelector = tituloSelector
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.resumen_anual_share), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TarjetaResumen(
    titulo: String,
    totalLabel: String,
    totalValor: String,
    subsTexto: String,
    servicioLabel: String,
    servicioTop: String?,
    categoriaLabel: String,
    categoriaTop: String?,
    marca: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(GradientIndigoStart, GradientIndigoEnd, Color(0xFFA78BFA))))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = titulo.uppercase(),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Text(text = totalLabel, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = totalValor,
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subsTexto,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(24.dp))
        servicioTop?.let {
            FilaResumen(label = servicioLabel, valor = it)
            Spacer(Modifier.height(12.dp))
        }
        categoriaTop?.let {
            FilaResumen(label = categoriaLabel, valor = it)
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = marca,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun FilaResumen(label: String, valor: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text(valor, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
