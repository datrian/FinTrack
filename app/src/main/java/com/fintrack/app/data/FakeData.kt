package com.fintrack.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.Color
import com.fintrack.app.data.model.BudgetCategoryLimit
import com.fintrack.app.data.model.FuturePrediction
import com.fintrack.app.data.model.MonthProjection
import com.fintrack.app.data.model.Transaction
import com.fintrack.app.data.model.TransactionDirection
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackOrange
import com.fintrack.app.ui.theme.FinTrackPurple
import com.fintrack.app.ui.theme.FinTrackRed

/**
 * Sample / preview data mirroring the values shown in the FinTrack Figma design.
 * Replace this with a real repository (network or local database) when wiring up
 * the app to an actual backend.
 */
object FakeData {

    // Resumen mostrado en la pantalla de Inicio (Home).
    const val TOTAL_BALANCE = 45280.50
    const val BALANCE_CHANGE_PERCENT = 8.4
    const val MONTHLY_INCOME = 5400.00
    const val MONTHLY_EXPENSES = 2120.00

    // Movimientos de ejemplo que aparecen en Inicio y en Transacciones.
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

    // Totales y límites por categoría para la pantalla de Presupuestos.
    const val BUDGET_TOTAL_SPENT = 2060.00
    const val BUDGET_TOTAL_LIMIT = 2700.00

    val budgetCategories = listOf(
        BudgetCategoryLimit("Alimentos y Bebidas", 1240.00, 1500.00, FinTrackNavy),
        BudgetCategoryLimit("Transporte y Gasolina", 380.00, 500.00, FinTrackGreen),
        BudgetCategoryLimit("Entretenimiento", 290.00, 300.00, FinTrackRed),
        BudgetCategoryLimit("Servicios Básicos", 150.00, 400.00, FinTrackPurple)
    )

    // Texto fijo de análisis de tendencia mostrado en la pantalla de Predicción.
    const val TREND_ANALYSIS_TEXT =
        "Basado en tus últimos 3 meses, se prevé una tendencia de ahorro ascendente de un 12%. " +
            "Recomendamos mantener los límites actuales."

    // Puntos del gráfico de gasto mensual (los últimos 3 son reales, "Nov*" es proyectado).
    val monthProjections = listOf(
        MonthProjection("Ago", 2100.0, "\$2.1k", isProjected = false),
        MonthProjection("Sep", 2300.0, "\$2.3k", isProjected = false),
        MonthProjection("Oct", 2100.0, "\$2.1k", isProjected = false, isSelected = true),
        MonthProjection("Nov*", 2240.0, "\$2.24k", isProjected = true)
    )

    // Predicciones de gasto para los próximos meses, con su nivel de confianza.
    val futurePredictions = listOf(
        FuturePrediction("Noviembre (Próximo)", 2240.00, "Alta confianza"),
        FuturePrediction("Diciembre", 2610.00, "Confianza Media"),
        FuturePrediction("Enero", 2080.00, "Confianza Media")
    )

    // Credenciales del usuario de ejemplo (Carlos Mendoza), usadas como atajo de
    // prueba en la pantalla de inicio de sesión (no pasan por el backend, por lo
    // que Mi Perfil no tendrá datos reales que mostrar con esta cuenta).
    const val DEMO_EMAIL = "carlos.mendoza@email.com"
    const val DEMO_PASSWORD = "1234"
}
