package com.fintrack.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import com.fintrack.app.ui.screens.AccountsScreen
import com.fintrack.app.ui.screens.BudgetScreen
import com.fintrack.app.ui.screens.CategoriesScreen
import com.fintrack.app.ui.screens.HomeScreen
import com.fintrack.app.ui.screens.PredictionScreen
import com.fintrack.app.ui.screens.ProfileScreen
import com.fintrack.app.ui.screens.SubcategoriesScreen
import com.fintrack.app.ui.screens.TransactionsScreen
import com.fintrack.app.ui.theme.FinTrackNavy

@Composable
fun FinTrackApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { FinTrackBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = FinTrackDestination.Home.route,
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            )
        ) {
            val openProfile: () -> Unit = {
                navController.navigate(FinTrackDestination.Profile.route) { launchSingleTop = true }
            }

            composableRoute(FinTrackDestination.Home.route) {
                HomeScreen(
                    onNavigateToAccounts = { navController.navigateSingleTop(FinTrackDestination.Accounts.route) },
                    onAddTransaction = { navController.navigateSingleTop(FinTrackDestination.Transactions.route) },
                    onOpenProfile = openProfile
                )
            }
            composableRoute(FinTrackDestination.Accounts.route) { AccountsScreen(onOpenProfile = openProfile) }
            composableRoute(FinTrackDestination.Transactions.route) { TransactionsScreen(onOpenProfile = openProfile) }
            composableRoute(FinTrackDestination.Budget.route) { BudgetScreen(onOpenProfile = openProfile) }
            composableRoute(FinTrackDestination.Prediction.route) { PredictionScreen(onOpenProfile = openProfile) }

            composableRoute(FinTrackDestination.Categories.route) {
                CategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onCategoryClick = { categoryId ->
                        navController.navigate(FinTrackDestination.Subcategories.createRoute(categoryId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = FinTrackDestination.Subcategories.route,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                SubcategoriesScreen(categoryId = categoryId, onBack = { navController.popBackStack() })
            }
            composableRoute(FinTrackDestination.Profile.route) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToCategories = {
                        navController.navigate(FinTrackDestination.Categories.route) { launchSingleTop = true }
                    }
                )
            }
        }
    }
}

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
 */
private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FinTrackBottomBar(navController: NavHostController) {
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
                    label = { Text(text = item.label) },
                    colors = NavigationBarItemDefaultsColors()
                )
            }
        }
    }
}

@Composable
private fun NavigationBarItemDefaultsColors() = androidx.compose.material3.NavigationBarItemDefaults.colors(
    selectedIconColor = FinTrackNavy,
    selectedTextColor = FinTrackNavy,
    indicatorColor = FinTrackNavy.copy(alpha = 0.12f)
)
