package com.subia.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.subia.android.navigation.CatalogoRoute
import com.subia.android.navigation.CategoriasRoute
import com.subia.android.navigation.DashboardRoute
import com.subia.android.navigation.LoginRoute
import com.subia.android.navigation.SuscripcionDetalleRoute
import com.subia.android.navigation.SuscripcionFormRoute
import com.subia.android.navigation.SuscripcionesRoute
import com.subia.android.ui.screens.CatalogoScreen
import com.subia.android.ui.screens.CategoriasScreen
import com.subia.android.ui.screens.DashboardScreen
import com.subia.android.ui.screens.LoginScreen
import com.subia.android.ui.screens.SuscripcionDetalleScreen
import com.subia.android.ui.screens.SuscripcionFormScreen
import com.subia.android.ui.screens.SuscripcionesScreen
import com.subia.shared.viewmodel.AuthViewModel

private val bottomNavItems = listOf(
    Triple(DashboardRoute, Icons.Default.Home, "Inicio"),
    Triple(SuscripcionesRoute, Icons.Default.List, "Suscripciones"),
    Triple(CategoriasRoute, Icons.Default.Category, "Categorías"),
    Triple(CatalogoRoute, Icons.Default.Shop, "Catálogo")
)

/** Composable raíz: gestiona NavHost y barra de navegación inferior. */
@Composable
fun SubIAApp(
    navController: NavHostController,
    startDestination: Any,
    authViewModel: AuthViewModel
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Cuando la sesión expire, forzar vuelta al login
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate(LoginRoute) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showBottomBar = currentDestination?.let { dest ->
        bottomNavItems.any { (route, _, _) -> dest.hasRoute(route::class) }
    } ?: false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { (route, icon, label) ->
                        val selected = currentDestination?.hasRoute(route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(DashboardRoute) { saveState = true }
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<LoginRoute> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    viewModel = authViewModel
                )
            }
            composable<DashboardRoute> { DashboardScreen() }
            composable<SuscripcionesRoute> {
                SuscripcionesScreen(
                    onNavigateToDetalle = { id -> navController.navigate(SuscripcionDetalleRoute(id)) },
                    onNavigateToNueva = { navController.navigate(SuscripcionFormRoute()) }
                )
            }
            composable<SuscripcionDetalleRoute> { backStackEntry ->
                val route: SuscripcionDetalleRoute = backStackEntry.toRoute()
                SuscripcionDetalleScreen(
                    suscripcionId = route.id,
                    onNavigateToEditar = { id -> navController.navigate(SuscripcionFormRoute(id)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<SuscripcionFormRoute> { backStackEntry ->
                val route: SuscripcionFormRoute = backStackEntry.toRoute()
                SuscripcionFormScreen(
                    suscripcionId = route.id,
                    onSuccess = { navController.popBackStack() }
                )
            }
            composable<CategoriasRoute> { CategoriasScreen() }
            composable<CatalogoRoute> {
                CatalogoScreen(
                    onSeleccionarItem = { item ->
                        navController.navigate(SuscripcionFormRoute())
                        // El item seleccionado se pasa via SavedStateHandle en una implementación completa
                    }
                )
            }
        }
    }
}
