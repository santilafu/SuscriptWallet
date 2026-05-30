package com.subia.android.ui.screens

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.subia.android.R
import kotlinx.coroutines.launch

private data class OnbPagina(val icon: ImageVector, val titleRes: Int, val descRes: Int)

/**
 * Onboarding de bienvenida (3 páginas) que se muestra una sola vez tras el primer login.
 * Comunica la propuesta de valor (catálogo, gasto real, avisos) y termina llevando al usuario
 * a la app. Ataca la retención del primer día.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit, onDetectGmail: () -> Unit = {}) {
    val paginas = listOf(
        OnbPagina(Icons.Default.Apps, R.string.onb1_title, R.string.onb1_desc),
        OnbPagina(Icons.Default.QueryStats, R.string.onb2_title, R.string.onb2_desc),
        OnbPagina(Icons.Default.NotificationsActive, R.string.onb3_title, R.string.onb3_desc),
        OnbPagina(Icons.Default.MarkEmailRead, R.string.onb4_title, R.string.onb4_desc)
    )
    val pagerState = rememberPagerState(pageCount = { paginas.size })
    val scope = rememberCoroutineScope()
    val esUltima = pagerState.currentPage == paginas.lastIndex

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Saltar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text(stringResource(R.string.onb_skip)) }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { page ->
            val p = paginas[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        p.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(Modifier.height(40.dp))
                Text(
                    text = stringResource(p.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(p.descRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Indicadores de página
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(paginas.size) { i ->
                val seleccionado = pagerState.currentPage == i
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

        if (esUltima) {
            // Última página: ofrece la detección por Gmail o continuar sin ella.
            Button(
                onClick = onDetectGmail,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = stringResource(R.string.onb4_action), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onb4_skip))
            }
        } else {
            Button(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = stringResource(R.string.onb_next), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
