package com.fintrack.app.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AccountType(val label: String) {
    PRINCIPAL("Principal"),
    INVERSION("Inversión"),
    CREDITO("Crédito"),
    EFECTIVO("Efectivo")
}

data class Account(
    val id: String,
    val name: String,
    val subtitle: String,
    val type: AccountType,
    val balance: Double
)

enum class TransactionDirection { INGRESO, GASTO }

data class Transaction(
    val id: String,
    val title: String,
    val category: String,
    val date: String,
    val amount: Double,
    val direction: TransactionDirection,
    val icon: ImageVector,
    val iconBackground: Color
)

data class BudgetCategoryLimit(
    val name: String,
    val spent: Double,
    val limit: Double,
    val barColor: Color
) {
    val percentage: Int
        get() = ((spent / limit) * 100).toInt().coerceIn(0, 999)
}

data class MonthProjection(
    val label: String,
    val amount: Double,
    val displayValue: String,
    val isProjected: Boolean,
    val isSelected: Boolean = false
)

data class FuturePrediction(
    val month: String,
    val amount: Double,
    val confidence: String
)

data class SpendingCategory(
    val id: String,
    val name: String,
    val subcategoryCount: Int,
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color
)

data class Subcategory(
    val id: String,
    val name: String,
    val limitLabel: String
)

data class UserProfile(
    val name: String,
    val email: String,
    val currency: String,
    val budgetNotificationsEnabled: Boolean
)
