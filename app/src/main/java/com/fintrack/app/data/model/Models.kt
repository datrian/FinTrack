package com.fintrack.app.data.model

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

// Los 3 tipos de movimiento que acepta el backend (POST/GET /api/v1/transacciones).
enum class TipoTransaccion { INGRESO, GASTO, TRANSFERENCIA }

// Un movimiento real del usuario, traído de GET /api/v1/transacciones. El
// backend no guarda título ni ícono: "comment" (si existe) hace de título, y
// la UI resuelve categoryId/subcategoryId a un nombre usando las listas ya
// cargadas de categorías/subcategorías.
data class AppTransaction(
    val id: String,
    val type: TipoTransaccion,
    val accountId: String,
    val destinationAccountId: String?,
    val categoryId: String?,
    val subcategoryId: String?,
    val amount: Double,
    val date: String,
    val comment: String?
)

// Los 4 estados posibles de una meta de ahorro (GET/POST /api/v1/metas).
enum class EstadoMeta { ACTIVA, CANCELADA, ALCANZADA, NO_ALCANZADA }

// Los 2 tipos de asignación que acepta el backend (GET/POST
// /api/v1/metas/{id}/asignaciones): un aporte suma al monto actual de la
// meta, un retiro lo resta.
enum class TipoAsignacion { APORTE, RETIRO }

// Las 2 acciones que acepta el cierre de una meta (POST
// /api/v1/metas/{id}/cierre): FINALIZAR la cierra como alcanzada o no
// alcanzada (según el monto actual vs el objetivo, lo decide el backend);
// CANCELAR la cierra como cancelada sin evaluar el monto.
enum class AccionCierre { FINALIZAR, CANCELAR }

// Los 3 estados posibles de un presupuesto (GET/POST /api/v1/presupuestos).
// El GET también acepta el valor especial "TODOS" como filtro, que no es un
// estado real y por eso no forma parte de este enum.
enum class EstadoPresupuesto { ACTIVO, FINALIZADO, CANCELADO }

// Las 2 acciones que acepta el cierre de un presupuesto (POST
// /api/v1/presupuestos/{id}/cierre): FINALIZAR lo cierra como terminado;
// CANCELAR lo cierra como cancelado.
enum class AccionCierrePresupuesto { FINALIZAR, CANCELAR }

// Un presupuesto real del usuario, traído de GET /api/v1/presupuestos. Es un
// límite total de gasto para un período (sin cuenta asociada); el backend
// calcula cuánto se ha consumido a partir de las transacciones de tipo GASTO
// registradas en ese período.
data class AppBudget(
    val id: String,
    val startDate: String,
    val endDate: String,
    val state: EstadoPresupuesto,
    val limitAmount: Double,
    val consumedAmount: Double,
    val remainingAmount: Double,
    val exceededAmount: Double,
    val consumedPercent: Double,
    val indicator: String,
    val isOverdue: Boolean
)

// Una meta de ahorro real del usuario, traída de GET /api/v1/metas. El
// backend calcula el avance (monto actual/faltante/porcentaje) a partir de
// las asignaciones hechas a la cuenta, no se calcula en el cliente.
data class AppGoal(
    val id: String,
    val accountId: String,
    val name: String,
    val description: String?,
    val targetAmount: Double,
    val currentAmount: Double,
    val remainingAmount: Double,
    val progressPercent: Double,
    val deadline: String,
    val state: EstadoMeta,
    val isOverdue: Boolean
)

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
// solo guarda nombre y estado (sin íconos ni colores); la UI le asigna un
// ícono genérico.
data class TransactionCategory(
    val id: String,
    val name: String
)

// Una subcategoría real de una categoría del usuario, traída de
// GET /api/v1/categorias/{id}/subcategorias.
data class TransactionSubcategory(
    val id: String,
    val categoryId: String,
    val name: String
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
