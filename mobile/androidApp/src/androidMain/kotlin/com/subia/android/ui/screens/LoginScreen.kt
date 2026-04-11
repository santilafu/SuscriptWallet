package com.subia.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
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
import com.subia.android.R
import com.subia.android.ui.theme.GradientIndigoEnd
import com.subia.android.ui.theme.GradientIndigoStart
import com.subia.shared.viewmodel.AuthUiState
import com.subia.shared.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

private const val WEB_BASE = "https://suscriptwallet.onrender.com"

/** Pantalla de inicio de sesión con email y contraseña, diseño oscuro tipo web. */
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
    val context = LocalContext.current
    val isLoading = uiState is AuthUiState.Loading

    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(GradientIndigoStart, GradientIndigoEnd)
    )

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo + nombre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = "SuscriptWallet",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF818CF8), fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                            append("SusCript")
                        }
                        withStyle(SpanStyle(color = Color(0xFFF1F5F9), fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                            append("Wallet")
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gestión de suscripciones",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Card oscura
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF111827),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF1F5F9),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Mensaje de error
                    if (uiState is AuthUiState.Error) {
                        Surface(
                            color = Color(0x1AEF4444),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = (uiState as AuthUiState.Error).mensaje,
                                color = Color(0xFFF87171),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Campo email
                    Text("Correo electrónico", style = MaterialTheme.typography.labelMedium, color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("tu@email.com", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0x1FFFFFFF),
                            focusedTextColor = Color(0xFFF1F5F9),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            cursorColor = Color(0xFF6366F1),
                            focusedContainerColor = Color(0xFF1A2235),
                            unfocusedContainerColor = Color(0xFF1A2235)
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

                    // Campo contraseña
                    Text("Contraseña", style = MaterialTheme.typography.labelMedium, color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0x1FFFFFFF),
                            focusedTextColor = Color(0xFFF1F5F9),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            cursorColor = Color(0xFF6366F1),
                            focusedContainerColor = Color(0xFF1A2235),
                            unfocusedContainerColor = Color(0xFF1A2235)
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

                    // ¿Olvidaste tu contraseña?
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            color = Color(0xFF818CF8),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clickable {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("$WEB_BASE/forgot-password"))
                                    )
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botón entrar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                brush = if (!isLoading) buttonGradient else Brush.horizontalGradient(
                                    listOf(Color(0x1FFFFFFF), Color(0x1FFFFFFF))
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Button(
                            onClick = { viewModel.login(email, password) },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(8.dp),
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
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("Entrar", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Crear cuenta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "¿No tienes cuenta? ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Crear cuenta",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("$WEB_BASE/register"))
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
