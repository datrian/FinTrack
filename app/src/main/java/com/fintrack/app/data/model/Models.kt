package com.fintrack.app.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Este archivo define los "modelos": las clases de datos que representan la
// información que se muestra en la app (cuentas, movimientos, presupuestos, etc).
// Son objetos simples, sin lógica de UI, que las pantallas leen para dibujar.

// Los tipos de cuenta que existen en la app. "label" es el texto que se
// muestra en pantalla (por ejemplo, en la etiqueta de color de cada cuenta).
enum class AccountType(val label: String) {
    PRINCIPAL("Principal"),
    INVERSION("Inversión"),
    CREDITO("Crédito"),
    EFECTIVO("Efectivo")
}

// Una cuenta bancaria/financiera del usuario (la que se ve en la pantalla de Cuentas).
data class Account(
    val id: String,
    val name: String,
    val subtitle: String,
    val type: AccountType,
    val balance: Double
)

// Indica si un movimiento es dinero que entra (INGRESO) o que sale (GASTO).
enum class TransactionDirection { INGRESO, GASTO }

// Un movimiento/transacción individual (por ejemplo, "Supermercado -$50").
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

// Límite de gasto mensual para una categoría de presupuesto.
data class BudgetCategoryLimit(
    val name: String,
    val spent: Double,
    val limit: Double,
    val barColor: Color
) {
    // Propiedad calculada (no se guarda, se recalcula cada vez que se lee):
    // porcentaje gastado respecto al límite, acotado entre 0 y 999 para que
    // nunca se muestre un número absurdo en la barra de progreso.
    val percentage: Int
        get() = ((spent / limit) * 100).toInt().coerceIn(0, 999)
}

// Un punto del gráfico de proyección mensual (pantalla de Predicción).
data class MonthProjection(
    val label: String,
    val amount: Double,
    val displayValue: String,
    val isProjected: Boolean, // true si es un mes futuro estimado, no real
    val isSelected: Boolean = false
)

// Una predicción de gasto futuro con su nivel de confianza (ej. "Alta").
data class FuturePrediction(
    val month: String,
    val amount: Double,
    val confidence: String
)

// Una categoría de gasto (ej. "Comida"), con su ícono y cuántas subcategorías tiene.
data class SpendingCategory(
    val id: String,
    val name: String,
    val subcategoryCount: Int,
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color
)

// Una subcategoría dentro de una categoría (ej. "Restaurantes" dentro de "Comida").
data class Subcategory(
    val id: String,
    val name: String,
    val limitLabel: String
)

// Datos del usuario que se muestran/editan en la pantalla de Perfil.
data class UserProfile(
    val name: String,
    val email: String,
    val currency: String,
    val budgetNotificationsEnabled: Boolean
)
