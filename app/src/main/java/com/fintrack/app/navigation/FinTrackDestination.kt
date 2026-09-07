package com.fintrack.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The 5 destinations shown in the bottom navigation bar, matching the FinTrack design.
 * "Categorías", "Subcategorías" and "Mi Perfil" are reached from Profile / Categories,
 * not from the bottom bar, mirroring the Figma prototype flow.
 */
sealed class FinTrackDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Home : FinTrackDestination("home", "Inicio", Icons.Filled.Home)
    data object Accounts : FinTrackDestination("accounts", "Cuenta", Icons.Filled.AccountBalanceWallet)
    data object Transactions : FinTrackDestination("transactions", "Transacciones", Icons.Filled.Receipt)
    data object Budget : FinTrackDestination("budget", "Presupuesto", Icons.Filled.PieChart)
    data object Prediction : FinTrackDestination("prediction", "Predicción", Icons.Filled.ShowChart)

    // Secondary destinations, reached via navigation, not part of the bottom bar.
    data object Categories : FinTrackDestination("categories", "Categorías", Icons.Filled.Home)
    data object Subcategories : FinTrackDestination("subcategories/{categoryId}", "Subcategorías", Icons.Filled.Home) {
        fun createRoute(categoryId: String) = "subcategories/$categoryId"
    }
    data object Profile : FinTrackDestination("profile", "Mi Perfil", Icons.Filled.Home)

    companion object {
        val bottomBarItems = listOf(Home, Accounts, Transactions, Budget, Prediction)
    }
}
