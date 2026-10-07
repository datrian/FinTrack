package com.fintrack.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import com.fintrack.app.ui.screens.AccountsScreen
import com.fintrack.app.ui.screens.AuthWelcomeScreen
import com.fintrack.app.ui.screens.BudgetScreen
import com.fintrack.app.ui.screens.CategoriesScreen
import com.fintrack.app.ui.screens.ChangePasswordScreen
import com.fintrack.app.ui.screens.CreateUserScreen
import com.fintrack.app.ui.screens.GoalsScreen
import com.fintrack.app.ui.screens.HomeScreen
import com.fintrack.app.ui.screens.LoginScreen
import com.fintrack.app.ui.screens.PredictionScreen
import com.fintrack.app.ui.screens.ProfileScreen
import com.fintrack.app.ui.screens.TransactionsScreen
import com.fintrack.app.ui.theme.FinTrackNavy

// Composable raíz de la app: arma el "Scaffold" (barra inferior + área de
// contenido) y define, mediante NavHost, qué pantalla se muestra para cada
// ruta declarada en FinTrackDestination.
@Composable
fun FinTrackApp() {
    // Controlador que guarda el historial de navegación (a qué pantalla ir,
    // cómo volver atrás, etc). "remember" hace que sobreviva a recomposiciones.
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { FinTrackBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            // Sin sesión persistida, la app siempre arranca en el flujo de
            // autenticación (Auth); tras iniciar sesión se navega a Home.
            startDestination = FinTrackDestination.Auth.route,
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            )
        ) {
            // Atajo compartido por varias pantallas para navegar a "Mi Perfil".
            val openProfile: () -> Unit = {
                navController.navigate(FinTrackDestination.Profile.route) { launchSingleTop = true }
            }

            // Pantalla de bienvenida: elegir entre iniciar sesión o crear cuenta.
            composableRoute(FinTrackDestination.Auth.route) {
                AuthWelcomeScreen(
                    onLogin = {
                        navController.navigate(FinTrackDestination.Login.route) { launchSingleTop = true }
                    },
                    onCreateAccount = {
                        navController.navigate(FinTrackDestination.CreateUser.route) { launchSingleTop = true }
                    }
                )
            }
            composableRoute(FinTrackDestination.Login.route) {
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onLoginSuccess = {
                        // Al iniciar sesión se limpia todo el flujo de auth del back stack.
                        navController.navigate(FinTrackDestination.Home.route) {
                            popUpTo(FinTrackDestination.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            composableRoute(FinTrackDestination.CreateUser.route) {
                CreateUserScreen(onBack = { navController.popBackStack() })
            }

            // Cada composableRoute registra una ruta y qué pantalla dibujar para ella.
            composableRoute(FinTrackDestination.Home.route) {
                HomeScreen(
                    onNavigateToAccounts = { navController.navigateSingleTop(FinTrackDestination.Accounts.route) },
                    onAddTransaction = { navController.navigateSingleTop(FinTrackDestination.Transactions.route) },
                    onOpenProfile = openProfile
                )
            }
            // Las 5 pantallas principales (una por cada ítem de la barra inferior).
            composableRoute(FinTrackDestination.Accounts.route) { AccountsScreen(onOpenProfile = openProfile) }
            composableRoute(FinTrackDestination.Transactions.route) { TransactionsScreen(onOpenProfile = openProfile) }
            composableRoute(FinTrackDestination.Budget.route) { BudgetScreen(onOpenProfile = openProfile) }
            composableRoute(FinTrackDestination.Goals.route) { GoalsScreen(onOpenProfile = openProfile) }
            composableRoute(FinTrackDestination.Prediction.route) { PredictionScreen(onOpenProfile = openProfile) }

            // Pantalla de categorías: categorías y subcategorías reales del
            // usuario (ver/crear/renombrar/desactivar), conectada al backend.
            composableRoute(FinTrackDestination.Categories.route) {
                CategoriesScreen(onBack = { navController.popBackStack() })
            }
            composableRoute(FinTrackDestination.Profile.route) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToCategories = {
                        navController.navigate(FinTrackDestination.Categories.route) { launchSingleTop = true }
                    },
                    onChangePassword = {
                        navController.navigate(FinTrackDestination.ChangePassword.route) { launchSingleTop = true }
                    },
                    onLogout = {
                        // Cierra sesión: limpia el perfil en caché y todo el back
                        // stack, y vuelve a la pantalla de bienvenida (iniciar
                        // sesión / crear cuenta).
                        com.fintrack.app.data.local.UserProfileStore.clear()
                        navController.navigate(FinTrackDestination.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composableRoute(FinTrackDestination.ChangePassword.route) {
                ChangePasswordScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

// Pequeño helper para no repetir "composable(route) { content() }" en cada
// pantalla que no necesita argumentos de ruta.
private fun androidx.navigation.NavGraphBuilder.composableRoute(
    route: String,
    content: @Composable () -> Unit
) {
    composable(route) { content() }
}

/**
 * Navigates to [route] the same safe way for every top-level (bottom-bar) destination,
 * whether the trigger is the bottom bar itself or a shortcut like Home's Quick Access
 * cards: launchSingleTop avoids stacking duplicate destinations on repeated/rapid taps,
 * and popUpTo + restoreState keep back-stack state consistent with the bottom bar.
 *
 * Se ancla explícitamente en Home (no en graph.findStartDestination()) porque el
 * NavHost arranca en Auth; una vez con sesión iniciada, Home es la raíz real de
 * las pestañas de la barra inferior.
 */
private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(FinTrackDestination.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// Barra de navegación inferior con los 5 accesos principales.
@Composable
private fun FinTrackBottomBar(navController: NavHostController) {
    // Se "escucha" la ruta actual como state: cada vez que cambia de pantalla,
    // este composable se recompone y actualiza qué ítem aparece seleccionado.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    // Only show the bottom bar on the 5 primary destinations, matching the design.
    val showBottomBar = FinTrackDestination.bottomBarItems.any { item ->
        currentRoute?.hierarchy?.any { it.route == item.route } == true
    }

    if (showBottomBar) {
        NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
            FinTrackDestination.bottomBarItems.forEach { item ->
                val selected = currentRoute?.hierarchy?.any { it.route == item.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = { navController.navigateSingleTop(item.route) },
                    icon = { androidx.compose.material3.Icon(imageVector = item.icon, contentDescription = item.label) },
                    label = {
                        Text(
                            text = item.label,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp
                            )
                        )
                    },
                    colors = NavigationBarItemDefaultsColors()
                )
            }
        }
    }
}

// Colores del ítem seleccionado/no seleccionado de la barra inferior.
@Composable
private fun NavigationBarItemDefaultsColors() = androidx.compose.material3.NavigationBarItemDefaults.colors(
    selectedIconColor = FinTrackNavy,
    selectedTextColor = FinTrackNavy,
    indicatorColor = FinTrackNavy.copy(alpha = 0.12f)
)
