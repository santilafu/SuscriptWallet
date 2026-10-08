package com.subia.android.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.coroutines.flow.first
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.subia.android.R
import com.subia.android.navigation.CatalogoRoute
import com.subia.android.navigation.CategoriasRoute
import com.subia.android.navigation.DashboardRoute
import com.subia.android.navigation.GmailScanRoute
import com.subia.android.navigation.LoginRoute
import com.subia.android.navigation.OnboardingRoute
import com.subia.android.navigation.ResumenAnualRoute
import com.subia.android.navigation.SettingsRoute
import com.subia.android.navigation.SuscripcionDetalleRoute
import com.subia.android.navigation.SuscripcionFormRoute
import com.subia.android.navigation.SuscripcionesRoute
import com.subia.android.ui.screens.CatalogoScreen
import com.subia.android.ui.screens.CategoriasScreen
import com.subia.android.ui.screens.DashboardScreen
import com.subia.android.ui.screens.GmailScanScreen
import com.subia.android.ui.screens.LoginScreen
import com.subia.android.ui.screens.OnboardingScreen
import com.subia.android.ui.screens.ResumenAnualScreen
import com.subia.android.ui.screens.SettingsScreen
import com.subia.android.ui.screens.SuscripcionDetalleScreen
import com.subia.android.ui.screens.SuscripcionFormScreen
import com.subia.android.ui.screens.SuscripcionesScreen
import com.subia.android.util.OnboardingPrefs
import com.subia.shared.viewmodel.AuthViewModel
import com.subia.shared.viewmodel.GmailScanViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel

private data class NavItem(val route: Any, val icon: ImageVector, val labelRes: Int)

/** Clave del SavedStateHandle de la lista: nº de suscripciones añadidas desde Gmail. */
private const val KEY_GMAIL_ADDED = "gmail_added"

/**
 * Tres pestañas: Inicio · Suscripciones · Catálogo. Categorías vive en Ajustes (es una lista
 * de gestión, no un destino de uso diario) y Ajustes se abre desde la TopAppBar.
 */
private val bottomNavItems = listOf(
    NavItem(DashboardRoute, Icons.Default.Home, R.string.nav_home),
    NavItem(SuscripcionesRoute, Icons.Default.CreditCard, R.string.nav_subscriptions),
    NavItem(CatalogoRoute, Icons.Default.Apps, R.string.nav_catalog)
)

/** Composable raíz: gestiona NavHost, barra superior con el título de la sección y barra inferior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubIAApp(
    navController: NavHostController,
    startDestination: Any,
    authViewModel: AuthViewModel,
    gmailReturnStatus: String? = null,
    onGmailReturnConsumed: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

    // Vuelta del consentimiento de Gmail (subia://gmail/done) cuando la pantalla de detección
    // ya no está en el back stack (el sistema mató la app mientras el usuario estaba en el
    // navegador): se abre la pantalla, que consume el estado y pide los resultados (G-07).
    LaunchedEffect(gmailReturnStatus, isLoggedIn) {
        if (gmailReturnStatus == null || !isLoggedIn) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first() // el grafo ya está montado
        val enGmail = navController.currentBackStackEntry?.destination?.hasRoute(GmailScanRoute::class) == true
        if (!enGmail) navController.navigate(GmailScanRoute) { launchSingleTop = true }
    }

    // Pestaña activa (null fuera de las tres pestañas → sin barras del shell).
    val seccionActual = currentDestination?.let { dest ->
        bottomNavItems.firstOrNull { item -> dest.hasRoute(item.route::class) }
    }

    Scaffold(
        topBar = {
            if (seccionActual != null) {
                TopAppBar(
                    title = {
                        // La TopAppBar dice dónde estás; la marca solo vive en Login y en la
                        // tarjeta compartible del resumen anual.
                        Text(
                            text = stringResource(seccionActual.labelRes),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(onClick = {
                            navController.navigate(SettingsRoute) { launchSingleTop = true }
                        }) {
                            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (seccionActual != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val label = stringResource(item.labelRes)
                        val selected = currentDestination?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(DashboardRoute) { saveState = true }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = label) },
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
            modifier = Modifier.padding(innerPadding),
            // Transiciones sutiles: fundido + ligero desplazamiento horizontal que insinúa
            // dirección al navegar (push) y al volver (pop), sin resultar intrusivo entre tabs.
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 12 } }
        ) {
            composable<LoginRoute> {
                LoginScreen(
                    onLoginSuccess = {
                        // Primer login: mostrar onboarding una sola vez; después, directo al dashboard.
                        val destino = if (OnboardingPrefs.isCompleted(context)) DashboardRoute else OnboardingRoute
                        navController.navigate(destino) {
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
                    onNavigateToResumenAnual = { navController.navigate(ResumenAnualRoute) },
                    // Tocar un logo de la tira de cobros o un cargo del mes abre su detalle
                    onNavigateToDetalle = { id -> navController.navigate(SuscripcionDetalleRoute(id)) },
                    onSesionExpirada = { authViewModel.logout() }
                )
            }
            composable<SuscripcionesRoute> { backStackEntry ->
                // Nº de suscripciones añadidas por la detección de Gmail (para el Snackbar).
                val gmailAdded by backStackEntry.savedStateHandle
                    .getStateFlow<Int?>(KEY_GMAIL_ADDED, null).collectAsState()
                SuscripcionesScreen(
                    onNavigateToDetalle = { id -> navController.navigate(SuscripcionDetalleRoute(id)) },
                    onNavigateToNueva = { nombre -> navController.navigate(SuscripcionFormRoute(prefillNombre = nombre)) },
                    onDetectGmail = { navController.navigate(GmailScanRoute) },
                    gmailAdded = gmailAdded,
                    onGmailAddedConsumed = { backStackEntry.savedStateHandle[KEY_GMAIL_ADDED] = null },
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
                    prefillNombre = route.prefillNombre,
                    onSuccess = { navController.popBackStack() },
                    navController = navController
                )
            }
            composable<CategoriasRoute> {
                CategoriasScreen(onBack = { navController.popBackStack() })
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToCategorias = { navController.navigate(CategoriasRoute) },
                    onDetectGmail = { navController.navigate(GmailScanRoute) },
                    onVerTutorial = {
                        OnboardingPrefs.reset(context)
                        navController.navigate(OnboardingRoute)
                    },
                    onLogout = { authViewModel.logout() }
                )
            }
            composable<ResumenAnualRoute> {
                ResumenAnualScreen(onBack = { navController.popBackStack() })
            }
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onFinish = {
                        OnboardingPrefs.setCompleted(context)
                        // Tanto tras el primer login ([Onboarding]) como desde Ajustes
                        // ([Dashboard, Ajustes, Onboarding]) se termina con Inicio como única raíz.
                        navController.navigate(DashboardRoute) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable<GmailScanRoute> {
                val gmailVm: GmailScanViewModel = koinViewModel()
                GmailScanScreen(
                    viewModel = gmailVm,
                    returnStatus = gmailReturnStatus,
                    onReturnConsumed = onGmailReturnConsumed,
                    onDone = { added ->
                        navController.navigate(SuscripcionesRoute) {
                            popUpTo(DashboardRoute)
                            launchSingleTop = true
                        }
                        // La lista (ya en lo alto de la pila) confirma con un Snackbar.
                        navController.currentBackStackEntry?.savedStateHandle?.set(KEY_GMAIL_ADDED, added)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
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
