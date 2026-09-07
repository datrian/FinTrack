package com.fintrack.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.Color
import com.fintrack.app.data.model.Account
import com.fintrack.app.data.model.AccountType
import com.fintrack.app.data.model.BudgetCategoryLimit
import com.fintrack.app.data.model.FuturePrediction
import com.fintrack.app.data.model.MonthProjection
import com.fintrack.app.data.model.SpendingCategory
import com.fintrack.app.data.model.Subcategory
import com.fintrack.app.data.model.Transaction
import com.fintrack.app.data.model.TransactionDirection
import com.fintrack.app.data.model.UserProfile
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackOrange
import com.fintrack.app.ui.theme.FinTrackPurple
import com.fintrack.app.ui.theme.FinTrackRed
import com.fintrack.app.ui.theme.FinTrackTeal

/**
 * Sample / preview data mirroring the values shown in the FinTrack Figma design.
 * Replace this with a real repository (network or local database) when wiring up
 * the app to an actual backend.
 */
object FakeData {


    const val USER_NAME = "Carlos Mendoza"
    const val TOTAL_BALANCE = 45280.50
    const val BALANCE_CHANGE_PERCENT = 8.4
    const val MONTHLY_INCOME = 5400.00
    const val MONTHLY_EXPENSES = 2120.00

    val accounts = listOf(
        Account(
            id = "acc-1",
            name = "BBVA Nómina",
            subtitle = "Cuenta Corriente",
            type = AccountType.PRINCIPAL,
            balance = 24500.00
        ),
        Account(
            id = "acc-2",
            name = "Santander Ahorro",
            subtitle = "Cuenta de Ahorros",
            type = AccountType.INVERSION,
            balance = 18200.00
        ),
        Account(
            id = "acc-3",
            name = "Tarjeta Visa Platinum",
            subtitle = "Crédito",
            type = AccountType.CREDITO,
            balance = -1420.50
        ),
        Account(
            id = "acc-4",
            name = "Efectivo",
            subtitle = "Billetera",
            type = AccountType.EFECTIVO,
            balance = 4000.00
        )
    )

    val transactions = listOf(
        Transaction(
            id = "tx-1",
            title = "Supermercado Walmart",
            category = "Alimentos",
            date = "Hoy, 10:45 AM",
            amount = -85.50,
            direction = TransactionDirection.GASTO,
            icon = Icons.Filled.ShoppingCart,
            iconBackground = FinTrackNavy.copy(alpha = 0.1f)
        ),
        Transaction(
            id = "tx-2",
            title = "Sueldo Quincenal",
            category = "Nómina",
            date = "Ayer, 6:00 PM",
            amount = 2700.00,
            direction = TransactionDirection.INGRESO,
            icon = Icons.Filled.AccountBalanceWallet,
            iconBackground = FinTrackGreen.copy(alpha = 0.15f)
        ),
        Transaction(
            id = "tx-3",
            title = "Suscripción Netflix",
            category = "Entretenimiento",
            date = "24 Oct",
            amount = -12.99,
            direction = TransactionDirection.GASTO,
            icon = Icons.Filled.Tv,
            iconBackground = FinTrackNavy.copy(alpha = 0.1f)
        ),
        Transaction(
            id = "tx-4",
            title = "Gasolinera Pemex",
            category = "Transporte",
            date = "23 Oct",
            amount = -45.00,
            direction = TransactionDirection.GASTO,
            icon = Icons.Filled.DirectionsCar,
            iconBackground = FinTrackNavy.copy(alpha = 0.1f)
        ),
        Transaction(
            id = "tx-5",
            title = "Transferencia Recibida",
            category = "Otros",
            date = "22 Oct",
            amount = 120.00,
            direction = TransactionDirection.INGRESO,
            icon = Icons.Filled.ArrowDownward,
            iconBackground = FinTrackGreen.copy(alpha = 0.15f)
        )
    )

    const val BUDGET_TOTAL_SPENT = 2060.00
    const val BUDGET_TOTAL_LIMIT = 2700.00

    val budgetCategories = listOf(
        BudgetCategoryLimit("Alimentos y Bebidas", 1240.00, 1500.00, FinTrackNavy),
        BudgetCategoryLimit("Transporte y Gasolina", 380.00, 500.00, FinTrackGreen),
        BudgetCategoryLimit("Entretenimiento", 290.00, 300.00, FinTrackRed),
        BudgetCategoryLimit("Servicios Básicos", 150.00, 400.00, FinTrackPurple)
    )

    const val TREND_ANALYSIS_TEXT =
        "Basado en tus últimos 3 meses, se prevé una tendencia de ahorro ascendente de un 12%. " +
            "Recomendamos mantener los límites actuales."

    val monthProjections = listOf(
        MonthProjection("Ago", 2100.0, "\$2.1k", isProjected = false),
        MonthProjection("Sep", 2300.0, "\$2.3k", isProjected = false),
        MonthProjection("Oct", 2100.0, "\$2.1k", isProjected = false, isSelected = true),
        MonthProjection("Nov*", 2240.0, "\$2.24k", isProjected = true)
    )

    val futurePredictions = listOf(
        FuturePrediction("Noviembre (Próximo)", 2240.00, "Alta confianza"),
        FuturePrediction("Diciembre", 2610.00, "Confianza Media"),
        FuturePrediction("Enero", 2080.00, "Confianza Media")
    )

    val categories = listOf(
        SpendingCategory(
            id = "cat-1",
            name = "Alimentos y Bebidas",
            subcategoryCount = 12,
            icon = Icons.Filled.ShoppingCart,
            iconBackground = FinTrackNavy.copy(alpha = 0.1f),
            iconTint = FinTrackNavy
        ),
        SpendingCategory(
            id = "cat-2",
            name = "Transporte público y taxi",
            subcategoryCount = 4,
            icon = Icons.Filled.DirectionsCar,
            iconBackground = FinTrackGreen.copy(alpha = 0.15f),
            iconTint = FinTrackGreen
        ),
        SpendingCategory(
            id = "cat-3",
            name = "Entretenimiento y ocio",
            subcategoryCount = 8,
            icon = Icons.Filled.Tv,
            iconBackground = FinTrackRed.copy(alpha = 0.12f),
            iconTint = FinTrackRed
        ),
        SpendingCategory(
            id = "cat-4",
            name = "Servicios domésticos",
            subcategoryCount = 6,
            icon = Icons.Filled.Home,
            iconBackground = FinTrackPurple.copy(alpha = 0.12f),
            iconTint = FinTrackPurple
        ),
        SpendingCategory(
            id = "cat-5",
            name = "Salud y Bienestar",
            subcategoryCount = 5,
            icon = Icons.Filled.Favorite,
            iconBackground = FinTrackTeal.copy(alpha = 0.15f),
            iconTint = FinTrackTeal
        )
    )

    val subcategoriesByCategory: Map<String, List<Subcategory>> = mapOf(
        "cat-1" to listOf(
            Subcategory("sub-1", "Restaurantes y cafeterías", "Sin límite"),
            Subcategory("sub-2", "Supermercados", "Límite: \$800.00"),
            Subcategory("sub-3", "Comida rápida y snacks", "Sin límite"),
            Subcategory("sub-4", "Delivery de alimentos", "Límite: \$200.00"),
            Subcategory("sub-5", "Bebidas y licores", "Sin límite")
        )
    )

    val userProfile = UserProfile(
        name = "Carlos Mendoza",
        email = "carlos.mendoza@email.com",
        currency = "USD (\$)",
        budgetNotificationsEnabled = true
    )
}
