package com.fintrack.app.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Este archivo define los "modelos": las clases de datos que representan la
// información que se muestra en la app (cuentas, movimientos, presupuestos, etc).
// Son objetos simples, sin lógica de UI, que las pantallas leen para dibujar.

// Los tipos de cuenta que acepta el backend (GET/POST /api/v1/cuentas). Los
// nombres de las constantes deben coincidir exactamente con los valores que
// manda/devuelve la API ("EFECTIVO", "DEBITO", etc.); "label" es solo el
// texto que se muestra en pantalla.
enum class AccountType(val label: String) {
    EFECTIVO("Efectivo"),
    DEBITO("Débito"),
    AHORRO("Ahorro"),
    CREDITO("Crédito")
}

// Una cuenta bancaria/financiera del usuario (la que se ve en la pantalla de
// Cuentas), traída de GET /api/v1/cuentas.
data class Account(
    val id: String,
    val name: String,
    val institution: String?,
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

// Una categoría real del usuario, traída de GET /api/v1/categorias. El backend
// solo guarda nombre y estado: no tiene íconos ni colores (a diferencia de
// SpendingCategory, que es el modelo de ejemplo de la pantalla de Categorías).
data class TransactionCategory(
    val id: String,
    val name: String
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

// Datos del usuario que se muestran/editan en la pantalla de Perfil, traídos
// de GET /api/v1/usuarios/me. "currency" es un valor de interfaz fijo: el
// backend todavía no expone una moneda principal configurable.
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val currency: String,
    val profilePhotoPath: String?,
    val budgetNotificationsEnabled: Boolean,
    val periodicNotificationsEnabled: Boolean,
    val isActive: Boolean,
    val registrationDate: String,
    val deactivationDate: String?
)
