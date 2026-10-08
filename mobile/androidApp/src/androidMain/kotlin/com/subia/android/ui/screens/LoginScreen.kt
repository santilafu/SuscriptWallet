package com.subia.android.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subia.android.R
import com.subia.android.auth.GoogleSignInHelper
import com.subia.android.auth.GoogleSignInResult
import com.subia.android.ui.theme.GradientBrandEnd
import com.subia.android.ui.theme.GradientBrandMid
import com.subia.android.ui.theme.GradientBrandStart
import com.subia.shared.viewmodel.AuthError
import com.subia.shared.viewmodel.AuthUiState
import com.subia.shared.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import com.subia.android.ui.theme.Violet600
import com.subia.android.ui.theme.Violet400

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
    var mostrarPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity
    val isLoading = uiState is AuthUiState.Loading

    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(GradientBrandStart, GradientBrandMid, GradientBrandEnd)
    )

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onLoginSuccess()
    }

    // El fondo oscuro cubre también la barra de estado (edge-to-edge); el contenido respeta
    // los insets para no quedar bajo la cámara ni la barra de navegación.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1E))
            .windowInsetsPadding(WindowInsets.safeDrawing),
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
                // El mismo logo que el icono de la app (el nombre va al lado: logo decorativo).
                Image(
                    painter = painterResource(R.drawable.logo_app),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Violet400, fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                            append("Suscript")
                        }
                        withStyle(SpanStyle(color = Color(0xFFF1F5F9), fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                            append("Wallet")
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.subscription_management),
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
                        text = stringResource(R.string.sign_in),
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
                                text = authErrorText((uiState as AuthUiState.Error).error),
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
                    Text(stringResource(R.string.email_label), style = MaterialTheme.typography.labelMedium, color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text(stringResource(R.string.email_placeholder), color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet600,
                            unfocusedBorderColor = Color(0x1FFFFFFF),
                            focusedTextColor = Color(0xFFF1F5F9),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            cursorColor = Violet600,
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
                    Text(stringResource(R.string.password_label), style = MaterialTheme.typography.labelMedium, color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet600,
                            unfocusedBorderColor = Color(0x1FFFFFFF),
                            focusedTextColor = Color(0xFFF1F5F9),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            cursorColor = Violet600,
                            focusedContainerColor = Color(0xFF1A2235),
                            unfocusedContainerColor = Color(0xFF1A2235)
                        ),
                        visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                                Icon(
                                    imageVector = if (mostrarPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = stringResource(if (mostrarPassword) R.string.password_hide else R.string.password_show),
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
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

                    // ¿Olvidaste tu contraseña? — TextButton: área táctil de 48 dp y rol de botón.
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$WEB_BASE/forgot-password")))
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.forgot_password),
                                color = Violet400,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                Text(stringResource(R.string.login_button), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0x1FFFFFFF)
                        )
                        Text(
                            text = stringResource(R.string.or_divider),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0x1FFFFFFF)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            val act = activity ?: return@OutlinedButton
                            scope.launch {
                                when (val result = GoogleSignInHelper.signIn(act)) {
                                    is GoogleSignInResult.Success ->
                                        viewModel.loginWithGoogle(result.idToken)
                                    GoogleSignInResult.UserCancelled -> Unit
                                    GoogleSignInResult.NoGoogleAccounts ->
                                        viewModel.showGoogleError(context.getString(R.string.google_no_accounts))
                                    GoogleSignInResult.NotConfigured ->
                                        viewModel.showGoogleError(context.getString(R.string.google_not_configured))
                                    is GoogleSignInResult.Unknown ->
                                        viewModel.showGoogleError(context.getString(R.string.google_error, result.message))
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_google),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.continue_with_google),
                            color = Color(0xFFF1F5F9),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Crear cuenta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.no_account),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$WEB_BASE/register")))
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.create_account),
                                style = MaterialTheme.typography.bodySmall,
                                color = Violet400,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/** Traduce el error tipado del ViewModel al texto localizado que ve el usuario. */
@Composable
private fun authErrorText(error: AuthError): String = when (error) {
    AuthError.CredencialesVacias -> stringResource(R.string.auth_error_empty_credentials)
    AuthError.CredencialesIncorrectas -> stringResource(R.string.auth_error_bad_credentials)
    AuthError.TokenGoogleVacio -> stringResource(R.string.auth_error_google_token_empty)
    AuthError.GoogleNoVerificado -> stringResource(R.string.auth_error_google_not_verified)
    AuthError.SinConexion -> stringResource(R.string.auth_error_offline)
    is AuthError.Desconocido -> stringResource(R.string.auth_error_generic)
    is AuthError.Mensaje -> error.texto
}
