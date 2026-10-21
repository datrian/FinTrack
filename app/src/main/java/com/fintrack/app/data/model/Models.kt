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

// Hacia dónde mueve el dinero un tipo de transacción: entra a las cuentas del
// usuario, sale de ellas, o solo se mueve entre cuentas propias.
enum class FlujoDinero { ENTRADA, SALIDA, NEUTRO }

// Los tipos de movimiento que maneja el backend (GET /api/v1/transacciones).
// Los nombres de las constantes deben coincidir con los valores de la API.
// Si el backend agrega un tipo nuevo que no está aquí, Gson lo deja en null:
// por eso el tipo de una transacción recibida es nullable.
enum class TipoTransaccion(val label: String, val flujo: FlujoDinero) {
    INGRESO("Ingreso", FlujoDinero.ENTRADA),
    GASTO("Gasto", FlujoDinero.SALIDA),
    TRANSFERENCIA("Transferencia", FlujoDinero.NEUTRO),
    INGRESO_RESERVADO("Ingreso reservado", FlujoDinero.ENTRADA),
    PAGO_DEUDA("Pago de deuda", FlujoDinero.SALIDA),
    PRESTAMO_RECIBIDO("Préstamo recibido", FlujoDinero.ENTRADA),
    // Dinero que el usuario adelanta y que después le van a reembolsar.
    ANTICIPO_REEMBOLSABLE("Anticipo reembolsable", FlujoDinero.SALIDA),
    REEMBOLSO("Reembolso", FlujoDinero.ENTRADA),
    DEVOLUCION_GASTO("Devolución de gasto", FlujoDinero.ENTRADA);

    companion object {
        // Los tipos que se pueden registrar desde el formulario de la app.
        val creatable = listOf(INGRESO, GASTO, TRANSFERENCIA)
    }
}

// Texto para mostrar el tipo de una transacción, incluso si es uno que la app
// todavía no conoce (null).
fun TipoTransaccion?.labelOrGeneric(): String = this?.label ?: "Movimiento"

// Un movimiento real del usuario, traído de GET /api/v1/transacciones. El
// backend no guarda título ni ícono: "comment" (si existe) hace de título, y
// la UI resuelve categoryId/subcategoryId a un nombre usando las listas ya
// cargadas de categorías/subcategorías.
data class AppTransaction(
    val id: String,
    // null si el backend mandó un tipo que la app todavía no conoce.
    val type: TipoTransaccion?,
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

// Los 3 estados posibles de un presupuesto ("estado_periodo" en
// GET /api/v1/presupuestos).
enum class EstadoPresupuesto { ACTIVO, FINALIZADO, CANCELADO }

// Las 2 formas de cerrar un presupuesto: FINALIZAR (POST
// /api/v1/presupuestos/{id}/finalizar) lo cierra como terminado; CANCELAR
// (POST /api/v1/presupuestos/{id}/cancelar) lo cierra como cancelado.
enum class AccionCierrePresupuesto { FINALIZAR, CANCELAR }

// Un presupuesto real del usuario, traído de GET /api/v1/presupuestos (el
// backend lo llama "periodo"). Es un monto total para unas fechas, repartido
// entre los 4 grupos de gasto; el backend calcula cuánto se ha consumido a
// partir de las transacciones registradas en ese período.
data class AppBudget(
    val id: String,
    val code: String,
    val startDate: String,
    val endDate: String,
    val state: EstadoPresupuesto,
    // Monto que capturó el usuario al crearlo.
    val plannedAmount: Double,
    // Monto que realmente se puede gastar (lo limitan los ingresos reales).
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

// Una categoría real del usuario, traída de GET /api/v1/categorias. El backend
// solo guarda nombre y estado (sin íconos ni colores); la UI le asigna un
// ícono genérico.
data class TransactionCategory(
    val id: String,
    val name: String,
    // Grupo de presupuesto de la categoría (solo lo usa la pantalla de Categorías).
    val groupId: String? = null
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
