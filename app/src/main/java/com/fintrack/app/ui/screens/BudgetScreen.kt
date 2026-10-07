package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fintrack.app.data.local.SessionManager
import com.fintrack.app.data.model.AppBudget
import com.fintrack.app.data.model.EstadoPresupuesto
import com.fintrack.app.data.model.TransactionCategory
import com.fintrack.app.data.remote.CategoriaResponse
import com.fintrack.app.data.remote.CrearPresupuestoRequest
import com.fintrack.app.data.remote.DetallePresupuestoCrearRequest
import com.fintrack.app.data.remote.DetallePresupuestoSalidaResponse
import com.fintrack.app.data.remote.PresupuestoResumenResponse
import com.fintrack.app.data.remote.RetrofitClient
import com.fintrack.app.data.remote.parseApiErrorMessage
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.LabeledProgressBar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.components.formatShortDate
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Los 4 filtros disponibles sobre la lista de presupuestos. "estado" se
// manda tal cual al backend: TODOS no es un estado real, es un valor
// especial que el GET acepta para no filtrar.
private enum class BudgetFilter(val label: String, val estado: String) {
    ACTIVOS("Activos", "ACTIVO"),
    FINALIZADOS("Finalizados", "FINALIZADO"),
    CANCELADOS("Cancelados", "CANCELADO"),
    TODOS("Todos", "TODOS")
}

private fun PresupuestoResumenResponse.toAppBudget() = AppBudget(
    id = id_presupuesto,
    startDate = fecha_inicio_presupuesto,
    endDate = fecha_fin_presupuesto,
    state = estado_presupuesto,
    limitAmount = monto_limite_presupuesto.toDoubleOrNull() ?: 0.0,
    consumedAmount = monto_consumido_presupuesto.toDoubleOrNull() ?: 0.0,
    remainingAmount = monto_restante_presupuesto.toDoubleOrNull() ?: 0.0,
    exceededAmount = monto_excedido_presupuesto.toDoubleOrNull() ?: 0.0,
    consumedPercent = porcentaje_consumido_presupuesto.toDoubleOrNull() ?: 0.0,
    indicator = indicador_presupuesto,
    isOverdue = periodo_vencido
)

private fun CategoriaResponse.toTransactionCategory() = TransactionCategory(
    id = id_categoria,
    name = nombre_categoria
)

// Pantalla de Presupuestos: presupuestos reales del usuario, traídos de
// GET /api/v1/presupuestos, con filtro por estado y un botón para crear uno
// nuevo (POST /api/v1/presupuestos). Un presupuesto es un límite total de
// gasto por período, repartido por porcentaje entre categorías reales.
@Composable
fun BudgetScreen(onOpenProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf(BudgetFilter.ACTIVOS) }

    var budgets by remember { mutableStateOf<List<AppBudget>>(emptyList()) }
    var isLoadingBudgets by remember { mutableStateOf(true) }
    var budgetsError by remember { mutableStateOf<String?>(null) }

    val loadBudgets: () -> Unit = {
        scope.launch {
            isLoadingBudgets = true
            budgetsError = null
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                val response = RetrofitClient.presupuestoApi.obtenerPresupuestos(
                    authorization = "Bearer $token",
                    estado = selectedFilter.estado
                )
                budgets = response.presupuestos.map { it.toAppBudget() }
            } catch (e: HttpException) {
                budgetsError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
            } catch (e: IOException) {
                budgetsError = "No se pudo conectar con el servidor. Revisa tu conexión."
            } catch (e: IllegalStateException) {
                budgetsError = "Tu sesión expiró. Vuelve a iniciar sesión."
            } finally {
                isLoadingBudgets = false
            }
        }
    }

    // Se vuelve a pedir cada vez que cambia el filtro (el backend filtra con
    // el query param "estado", no se filtra en el cliente).
    LaunchedEffect(selectedFilter) { loadBudgets() }

    // Categorías del usuario, necesarias para el formulario de "Nuevo
    // presupuesto" (cada detalle del presupuesto asigna un % a una
    // categoría real). Se traen una sola vez al entrar.
    var categories by remember { mutableStateOf<List<TransactionCategory>>(emptyList()) }
    var isLoadingCategories by remember { mutableStateOf(true) }
    var categoriesError by remember { mutableStateOf<String?>(null) }

    val loadCategories: () -> Unit = {
        scope.launch {
            isLoadingCategories = true
            categoriesError = null
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                categories = RetrofitClient.categoriaApi.obtenerCategorias("Bearer $token").categorias.map { it.toTransactionCategory() }
            } catch (e: HttpException) {
                categoriesError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
            } catch (e: IOException) {
                categoriesError = "No se pudo conectar con el servidor. Revisa tu conexión."
            } catch (e: IllegalStateException) {
                categoriesError = "Tu sesión expiró. Vuelve a iniciar sesión."
            } finally {
                isLoadingCategories = false
            }
        }
    }

    LaunchedEffect(Unit) { loadCategories() }

    // Id del presupuesto cuyo detalle se está viendo (tap sobre una
    // tarjeta). El diálogo pide el detalle con GET /api/v1/presupuestos/{id}
    // (no reutiliza los datos ya cargados en la lista), que incluye el
    // desglose por categoría.
    var selectedBudgetId by remember { mutableStateOf<String?>(null) }
    selectedBudgetId?.let { budgetId ->
        BudgetDetailDialog(
            budgetId = budgetId,
            onDismiss = { selectedBudgetId = null }
        )
    }

    var isAddingBudget by remember { mutableStateOf(false) }
    if (isAddingBudget) {
        AddBudgetDialog(
            categories = categories,
            onDismiss = { isAddingBudget = false },
            // Se ejecuta cuando el POST al backend tuvo éxito.
            onCreated = { newBudget ->
                if (selectedFilter.estado == "TODOS" || selectedFilter.estado == newBudget.state.name) {
                    budgets = listOf(newBudget) + budgets
                }
                isAddingBudget = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackTopBar(title = "Presupuestos", onMenuClick = onOpenProfile) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BudgetFilter.entries.forEach { filter ->
                    BudgetFilterChip(
                        label = filter.label,
                        selected = filter == selectedFilter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }
        }

        when {
            isLoadingBudgets && budgets.isEmpty() -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = FinTrackNavy)
                    }
                }
            }
            budgetsError != null && budgets.isEmpty() -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = budgetsError.orEmpty(),
                            color = FinTrackRed,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, FinTrackNavy, RoundedCornerShape(16.dp))
                                .clickable { loadBudgets() }
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "Reintentar", color = FinTrackNavy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            budgets.isEmpty() -> {
                item {
                    Text(
                        text = "No tienes presupuestos en este estado todavía.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
            }
            else -> {
                items(budgets) { budget ->
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        BudgetCard(budget, onClick = { selectedBudgetId = budget.id })
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, FinTrackNavy, RoundedCornerShape(16.dp))
                        .clickable(enabled = !isLoadingCategories) {
                            if (categoriesError != null) loadCategories() else isAddingBudget = true
                        }
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoadingCategories) {
                        CircularProgressIndicator(color = FinTrackNavy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = FinTrackNavy)
                        Text(
                            text = if (categoriesError != null) "Reintentar" else "Agregar presupuesto",
                            color = FinTrackNavy,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (categoriesError != null) {
                    Text(
                        text = categoriesError.orEmpty(),
                        color = FinTrackRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item { androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp)) }
    }
}

// Botoncito tipo "pastilla" para cada filtro de estado.
@Composable
private fun BudgetFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) FinTrackNavy else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

// Tarjeta de un presupuesto: período + estado, barra de progreso (consumido
// sobre límite) y montos. En rojo si está excedido o si el período ya venció.
@Composable
private fun BudgetCard(budget: AppBudget, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val isExceeded = budget.exceededAmount > 0
    SectionCard(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatShortDate(budget.startDate)} – ${formatShortDate(budget.endDate)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when (budget.state) {
                    EstadoPresupuesto.ACTIVO -> "Activo"
                    EstadoPresupuesto.FINALIZADO -> "Finalizado"
                    EstadoPresupuesto.CANCELADO -> "Cancelado"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = budget.indicator,
            style = MaterialTheme.typography.bodySmall,
            color = if (isExceeded) FinTrackRed else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )
        LabeledProgressBar(
            label = formatCurrency(budget.consumedAmount),
            valueText = "${budget.consumedPercent.toInt().coerceIn(0, 999)}%",
            progress = if (budget.limitAmount > 0) (budget.consumedAmount / budget.limitAmount).toFloat() else 0f,
            progressColor = if (isExceeded) FinTrackRed else FinTrackNavy
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Límite: ${formatCurrency(budget.limitAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isExceeded) "Excedido: ${formatCurrency(budget.exceededAmount)}" else "Restante: ${formatCurrency(budget.remainingAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isExceeded) FinTrackRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (budget.isOverdue) {
            Text(
                text = "Período vencido",
                style = MaterialTheme.typography.bodySmall,
                color = FinTrackRed,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// Popup de detalle de un presupuesto. Pide el registro completo con
// GET /api/v1/presupuestos/{id} (no reutiliza los datos ya cargados en la
// lista), que incluye el desglose de consumo por cada categoría asignada.
@Composable
private fun BudgetDetailDialog(budgetId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var budget by remember { mutableStateOf<AppBudget?>(null) }
    var consumidoPresupuestado by remember { mutableStateOf<Double?>(null) }
    var consumidoNoPresupuestado by remember { mutableStateOf<Double?>(null) }
    var detalles by remember { mutableStateOf<List<DetallePresupuestoSalidaResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(budgetId) {
        scope.launch {
            isLoading = true
            loadError = null
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                val response = RetrofitClient.presupuestoApi.obtenerPresupuesto("Bearer $token", budgetId)
                budget = response.presupuesto.toAppBudget()
                consumidoPresupuestado = response.monto_consumido_presupuestado.toDoubleOrNull()
                consumidoNoPresupuestado = response.monto_consumido_no_presupuestado.toDoubleOrNull()
                detalles = response.detalles
            } catch (e: HttpException) {
                loadError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
            } catch (e: IOException) {
                loadError = "No se pudo conectar con el servidor. Revisa tu conexión."
            } catch (e: IllegalStateException) {
                loadError = "Tu sesión expiró. Vuelve a iniciar sesión."
            } finally {
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Detalle del presupuesto", fontWeight = FontWeight.Bold) },
        text = {
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = FinTrackNavy) }
                loadError != null -> Text(text = loadError.orEmpty(), color = FinTrackRed)
                budget != null -> {
                    val current = budget!!
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        BudgetDetailRow("Período", "${formatShortDate(current.startDate)} – ${formatShortDate(current.endDate)}")
                        BudgetDetailRow("Estado", when (current.state) {
                            EstadoPresupuesto.ACTIVO -> "Activo"
                            EstadoPresupuesto.FINALIZADO -> "Finalizado"
                            EstadoPresupuesto.CANCELADO -> "Cancelado"
                        })
                        BudgetDetailRow("Indicador", current.indicator)
                        BudgetDetailRow("Límite", formatCurrency(current.limitAmount))
                        BudgetDetailRow("Consumido", formatCurrency(current.consumedAmount))
                        consumidoPresupuestado?.let { BudgetDetailRow("  En categorías presupuestadas", formatCurrency(it)) }
                        consumidoNoPresupuestado?.let { BudgetDetailRow("  Fuera de presupuesto", formatCurrency(it)) }
                        if (current.exceededAmount > 0) {
                            BudgetDetailRow("Excedido", formatCurrency(current.exceededAmount), valueColor = FinTrackRed)
                        } else {
                            BudgetDetailRow("Restante", formatCurrency(current.remainingAmount))
                        }
                        BudgetDetailRow("Avance", "${current.consumedPercent.toInt()}%")
                        if (current.isOverdue) {
                            BudgetDetailRow("Período vencido", "Sí", valueColor = FinTrackRed)
                        }

                        if (detalles.isNotEmpty()) {
                            Text(
                                text = "Desglose por categoría",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                            detalles.forEach { detalle ->
                                BudgetDetailCategoryRow(detalle)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cerrar") }
        }
    )
}

@Composable
private fun BudgetDetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = valueColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

// Una fila del desglose por categoría: nombre + % asignado, barra de
// progreso de esa categoría, y monto consumido/límite.
@Composable
private fun BudgetDetailCategoryRow(detalle: DetallePresupuestoSalidaResponse, modifier: Modifier = Modifier) {
    val limite = detalle.monto_limite_categoria.toDoubleOrNull() ?: 0.0
    val consumido = detalle.monto_consumido_categoria.toDoubleOrNull() ?: 0.0
    val excedido = detalle.monto_excedido_categoria.toDoubleOrNull() ?: 0.0
    val isExceeded = excedido > 0
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        LabeledProgressBar(
            label = "${detalle.nombre_categoria} (${detalle.porcentaje_asignado_presupuesto}%)",
            valueText = "${detalle.porcentaje_consumido_categoria.toDoubleOrNull()?.toInt() ?: 0}%",
            progress = if (limite > 0) (consumido / limite).toFloat() else 0f,
            progressColor = if (isExceeded) FinTrackRed else FinTrackNavy
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${formatCurrency(consumido)} de ${formatCurrency(limite)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = detalle.indicador_categoria,
                style = MaterialTheme.typography.bodySmall,
                color = if (isExceeded) FinTrackRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Popup para crear un presupuesto nuevo. Manda el POST al backend él mismo
// y, si tuvo éxito, le pasa el presupuesto ya creado a BudgetScreen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetDialog(
    categories: List<TransactionCategory>,
    onDismiss: () -> Unit,
    onCreated: (AppBudget) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var montoLimiteText by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedCategoryIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val percentageTexts = remember { mutableStateMapOf<String, String>() }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val montoLimite = montoLimiteText.toDoubleOrNull()
    val selectedDate = selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    }
    val isValid = montoLimite != null && montoLimite > 0 && selectedDate != null &&
        selectedCategoryIds.isNotEmpty() &&
        selectedCategoryIds.all { id -> percentageTexts[id]?.toIntOrNull()?.let { it in 1..100 } == true }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = "Nuevo presupuesto", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = montoLimiteText,
                    onValueChange = { montoLimiteText = it; errorMessage = null },
                    label = { Text("Monto límite total") },
                    placeholder = { Text("Ej. 2700.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Fecha fin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, FinTrackNavy, RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = null, tint = FinTrackNavy)
                    Text(
                        text = selectedDate?.let { formatShortDate(it.toString()) } ?: "Elegir fecha",
                        color = if (selectedDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                Text(
                    text = "Categorías",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = "Elige a qué categorías asignar un % del límite total.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (categories.isEmpty()) {
                    Text(
                        text = "Necesitas al menos una categoría para crear un presupuesto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinTrackRed
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            BudgetSelectableChip(
                                label = category.name,
                                selected = category.id in selectedCategoryIds,
                                onClick = {
                                    errorMessage = null
                                    if (category.id in selectedCategoryIds) {
                                        selectedCategoryIds = selectedCategoryIds - category.id
                                        percentageTexts.remove(category.id)
                                    } else {
                                        selectedCategoryIds = selectedCategoryIds + category.id
                                        percentageTexts[category.id] = ""
                                    }
                                }
                            )
                        }
                    }

                    selectedCategoryIds.forEach { categoryId ->
                        val categoryName = categories.firstOrNull { it.id == categoryId }?.name ?: categoryId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            OutlinedTextField(
                                value = percentageTexts[categoryId].orEmpty(),
                                onValueChange = { percentageTexts[categoryId] = it; errorMessage = null },
                                label = { Text("%") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(90.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = FinTrackRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val montoValue = montoLimite ?: return@Button
                    val fecha = selectedDate ?: return@Button
                    val detalles = selectedCategoryIds.map { id ->
                        DetallePresupuestoCrearRequest(
                            categoria_id = id,
                            porcentaje_asignado_presupuesto = percentageTexts[id]?.toIntOrNull() ?: 0
                        )
                    }
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val token = SessionManager.getAccessToken(context)
                                ?: throw IllegalStateException("No hay sesión activa")
                            val created = RetrofitClient.presupuestoApi.crearPresupuesto(
                                authorization = "Bearer $token",
                                request = CrearPresupuestoRequest(
                                    fecha_fin_presupuesto = fecha.toString(),
                                    // El backend exige el monto como texto decimal, no como número.
                                    monto_limite_presupuesto = String.format(java.util.Locale.US, "%.2f", montoValue),
                                    detalles = detalles
                                )
                            )
                            onCreated(created.presupuesto.toAppBudget())
                        } catch (e: HttpException) {
                            isSaving = false
                            errorMessage = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
                        } catch (e: IOException) {
                            isSaving = false
                            errorMessage = "No se pudo conectar con el servidor. Revisa tu conexión."
                        } catch (e: IllegalStateException) {
                            isSaving = false
                            errorMessage = "Tu sesión expiró. Vuelve a iniciar sesión."
                        }
                    }
                },
                enabled = isValid && !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackNavy)
            ) {
                Text(text = if (isSaving) "Guardando..." else "Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(text = "Cancelar")
            }
        }
    )
}

@Composable
private fun BudgetSelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) FinTrackNavy else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
