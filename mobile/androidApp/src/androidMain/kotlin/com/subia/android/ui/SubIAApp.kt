package com.subia.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.subia.android.ui.theme.GradientIndigoEnd
import com.subia.android.ui.theme.GradientIndigoStart
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.subia.android.navigation.CatalogoRoute
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

/** Composable raíz: gestiona NavHost, barra superior con logout y barra de navegación inferior. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
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

    val gradientBrush = Brush.linearGradient(
        colors = listOf(GradientIndigoStart, GradientIndigoEnd, Color(0xFFA78BFA)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        brush = gradientBrush,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                ) {
                                    append("SuscriptWallet")
                                }
                            }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión") },
                                onClick = {
                                    authViewModel.logout()
                                    showMenu = false
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = androidx.compose.ui.unit.Dp.Unspecified
                ) {
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
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
            composable<DashboardRoute> {
                DashboardScreen(
                    onNavigateToSuscripciones = {
                        navController.navigate(SuscripcionesRoute) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(DashboardRoute) { saveState = true }
                        }
                    },
                    onSesionExpirada = { authViewModel.logout() }
                )
            }
            composable<SuscripcionesRoute> {
                SuscripcionesScreen(
                    onNavigateToDetalle = { id -> navController.navigate(SuscripcionDetalleRoute(id)) },
                    onNavigateToNueva = { navController.navigate(SuscripcionFormRoute()) },
                    onSesionExpirada = { authViewModel.logout() }
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
                    onSuccess = { navController.popBackStack() },
                    navController = navController
                )
            }
            composable<CategoriasRoute> { CategoriasScreen() }
            composable<CatalogoRoute> {
                CatalogoScreen(
                    onSeleccionarItem = { item ->
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "catalog_item_json",
                            Json.encodeToString(item)
                        )
                        navController.navigate(SuscripcionFormRoute())
                    }
                )
            }
        }
    }
}
