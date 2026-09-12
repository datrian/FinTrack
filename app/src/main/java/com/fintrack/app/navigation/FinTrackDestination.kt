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
// "route" es el identificador de texto que usa el sistema de navegación de
// Compose para saber a qué pantalla ir (equivalente a una URL interna).
// "label" e "icon" son lo que se muestra en la barra inferior.
sealed class FinTrackDestination(val route: String, val label: String, val icon: ImageVector) {
    // Las 5 pestañas que aparecen en la barra de navegación inferior.
    data object Home : FinTrackDestination("home", "Inicio", Icons.Filled.Home)
    data object Accounts : FinTrackDestination("accounts", "Cuenta", Icons.Filled.AccountBalanceWallet)
    data object Transactions : FinTrackDestination("transactions", "Transacciones", Icons.Filled.Receipt)
    data object Budget : FinTrackDestination("budget", "Presupuesto", Icons.Filled.PieChart)
    data object Prediction : FinTrackDestination("prediction", "Predicción", Icons.Filled.ShowChart)

    // Secondary destinations, reached via navigation, not part of the bottom bar.
    // Destinos secundarios: no están en la barra inferior, se llega a ellos
    // navegando desde otra pantalla (por ejemplo, Perfil -> Categorías).
    data object Categories : FinTrackDestination("categories", "Categorías", Icons.Filled.Home)
    // Ruta con parámetro ({categoryId}): createRoute arma la ruta real
    // reemplazando el parámetro por el id concreto de la categoría elegida.
    data object Subcategories : FinTrackDestination("subcategories/{categoryId}", "Subcategorías", Icons.Filled.Home) {
        fun createRoute(categoryId: String) = "subcategories/$categoryId"
    }
    data object Profile : FinTrackDestination("profile", "Mi Perfil", Icons.Filled.Home)

    companion object {
        // Lista usada para dibujar los botones de la barra inferior, en orden.
        val bottomBarItems = listOf(Home, Accounts, Transactions, Budget, Prediction)
    }
}
