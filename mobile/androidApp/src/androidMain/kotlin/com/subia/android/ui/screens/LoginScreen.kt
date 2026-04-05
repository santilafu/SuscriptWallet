package com.subia.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.ui.theme.GradientIndigoEnd
import com.subia.android.ui.theme.GradientIndigoStart
import com.subia.android.ui.theme.Indigo400
import com.subia.shared.viewmodel.AuthUiState
import com.subia.shared.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Pantalla de inicio de sesión con email y contraseña. */
@OptIn(ExperimentalTextApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val isLoading = uiState is AuthUiState.Loading

    val gradientBrush = Brush.linearGradient(
        colors = listOf(GradientIndigoStart, GradientIndigoEnd, Color(0xFFA78BFA)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(GradientIndigoStart, GradientIndigoEnd)
    )

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo / icono superior
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = Indigo400,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Título con gradiente
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        brush = gradientBrush,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp
                    )
                ) {
                    append("SuscriptWallet")
                }
            },
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Controla tus suscripciones",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Indigo400,
                focusedLabelColor = Indigo400
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Indigo400,
                focusedLabelColor = Indigo400
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.login(email, password)
                }
            ),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (uiState is AuthUiState.Error) {
            Text(
                text = (uiState as AuthUiState.Error).mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Botón con gradiente indigo→violeta
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = if (!isLoading) buttonGradient else Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Iniciar sesión", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}
