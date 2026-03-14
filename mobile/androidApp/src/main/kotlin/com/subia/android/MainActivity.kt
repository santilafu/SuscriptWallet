package com.subia.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.subia.android.navigation.LoginRoute
import com.subia.android.navigation.DashboardRoute
import com.subia.android.ui.SubIAApp
import com.subia.android.ui.theme.SubIATheme
import com.subia.shared.viewmodel.AuthViewModel
import org.koin.androidx.compose.koinViewModel

/** Punto de entrada de la app. Única Activity, todo lo demás es Compose. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubIATheme {
                val authViewModel: AuthViewModel = koinViewModel()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                LaunchedEffect(Unit) { authViewModel.checkSession() }

                val navController = rememberNavController()
                val startDestination = if (isLoggedIn) DashboardRoute else LoginRoute

                SubIAApp(
                    navController = navController,
                    startDestination = startDestination,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
