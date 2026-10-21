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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fintrack.app.data.model.AccionCierrePresupuesto
import com.fintrack.app.data.model.AppBudget
import com.fintrack.app.data.model.EstadoPresupuesto
import com.fintrack.app.data.remote.ActualizarPresupuestoRequest
import com.fintrack.app.data.remote.CrearPresupuestoRequest
import com.fintrack.app.data.remote.GrupoDistribucionRequest
import com.fintrack.app.data.remote.GrupoResponse
import com.fintrack.app.data.remote.PeriodoGrupoResponse
import com.fintrack.app.data.remote.PeriodoResponse
import com.fintrack.app.data.remote.RetrofitClient
import com.fintrack.app.data.remote.TipoPeriodo
import com.fintrack.app.data.remote.runAuthenticatedCall
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.LabeledProgressBar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.components.formatShortDate
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

// Los 4 filtros disponibles sobre la lista de presupuestos. El backend
// devuelve todos los estados juntos, así que el filtro se aplica en la app;
// "estado" null significa no filtrar.
private enum class BudgetFilter(val label: String, val estado: EstadoPresupuesto?) {
    ACTIVOS("Activos", EstadoPresupuesto.ACTIVO),
    FINALIZADOS("Finalizados", EstadoPresupuesto.FINALIZADO),
    CANCELADOS("Cancelados", EstadoPresupuesto.CANCELADO),
    TODOS("Todos", null)
}

// Los campos de la respuesta pueden faltar (ver PeriodoResponse): se usan
// valores por defecto en vez de dejar que la app se cierre.
private fun PeriodoResponse.toAppBudget() = AppBudget(
    id = id_periodo.orEmpty(),
    code = codigo_periodo.orEmpty(),
    startDate = fecha_inicio_periodo.orEmpty(),
    endDate = fecha_fin_periodo.orEmpty(),
    state = estado_periodo ?: EstadoPresupuesto.ACTIVO,
    plannedAmount = monto_total_presupuesto?.toDoubleOrNull() ?: 0.0,
    limitAmount = monto_presupuesto_periodo?.toDoubleOrNull() ?: 0.0,
    consumedAmount = aplicado_total_periodo?.toDoubleOrNull() ?: 0.0,
    remainingAmount = monto_restante_periodo?.toDoubleOrNull() ?: 0.0,
    exceededAmount = monto_excedido_periodo?.toDoubleOrNull() ?: 0.0,
    consumedPercent = porcentaje_consumido_periodo?.toDoubleOrNull() ?: 0.0,
    indicator = estado_consumo_periodo.orEmpty(),
    isOverdue = periodo_vencido ?: false
)

private fun EstadoPresupuesto.label() = when (this) {
    EstadoPresupuesto.ACTIVO -> "Activo"
    EstadoPresupuesto.FINALIZADO -> "Finalizado"
    EstadoPresupuesto.CANCELADO -> "Cancelado"
}

private fun TipoPeriodo.label() = when (this) {
    TipoPeriodo.SEMANAL -> "Semanal"
    TipoPeriodo.QUINCENAL -> "Quincenal"
    TipoPeriodo.MENSUAL -> "Mensual"
    TipoPeriodo.PERSONALIZADO -> "Personalizado"
}

// Fecha fin automática según el tipo de período; null en PERSONALIZADO,
// donde la elige el usuario.
internal fun TipoPeriodo.endDateFor(start: LocalDate): LocalDate? = when (this) {
    TipoPeriodo.SEMANAL -> start.plusDays(6)
    TipoPeriodo.QUINCENAL -> start.plusDays(14)
    TipoPeriodo.MENSUAL -> start.plusMonths(1).minusDays(1)
    TipoPeriodo.PERSONALIZADO -> null
}

// Código sugerido para el período, con el formato que ya usa el backend
// ("OCT-2026"). Los períodos que no son mensuales llevan también el día
// ("19-OCT-2026") para no repetirse dentro del mismo mes.
internal fun suggestedPeriodCode(tipo: TipoPeriodo, start: LocalDate): String {
    val month = start.month.getDisplayName(TextStyle.SHORT, Locale("es", "ES"))
        .replace(".", "")
        .uppercase(Locale("es", "ES"))
    val base = "$month-${start.year}"
    return if (tipo == TipoPeriodo.MENSUAL) base else "${start.dayOfMonth}-$base"
}

private fun formatAmountForApi(amount: Double) = String.format(Locale.US, "%.2f", amount)

// Una fila del editor de porcentajes por grupo.
private data class GroupShare(val id: String, val name: String)

// Pantalla de Presupuestos: presupuestos reales del usuario, traídos de
// GET /api/v1/presupuestos, con filtro por estado y un botón para crear uno
// nuevo (POST /api/v1/presupuestos). Un presupuesto es un monto total por
// período, repartido por porcentaje entre los 4 grupos de gasto.
@Composable
fun BudgetScreen(onOpenProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf(BudgetFilter.ACTIVOS) }

    var allBudgets by remember { mutableStateOf<List<AppBudget>>(emptyList()) }
    var isLoadingBudgets by remember { mutableStateOf(true) }
    var budgetsError by remember { mutableStateOf<String?>(null) }
    val budgets = allBudgets.filter { selectedFilter.estado == null || it.state == selectedFilter.estado }

    val loadBudgets: () -> Unit = {
        scope.launch {
            isLoadingBudgets = true
            budgetsError = null
            runAuthenticatedCall(context, onError = { budgetsError = it }) { auth ->
                RetrofitClient.presupuestoApi.obtenerPresupuestos(auth)
            }?.let { response ->
                allBudgets = response.presupuestos.orEmpty().map { it.toAppBudget() }
            }
            isLoadingBudgets = false
        }
    }

    LaunchedEffect(Unit) { loadBudgets() }

    // Catálogo de grupos (Deudas, Gastos fijos, ...), necesario para el
    // formulario de "Nuevo presupuesto". Se trae una sola vez al entrar.
    var groups by remember { mutableStateOf<List<GrupoResponse>>(emptyList()) }
    var isLoadingGroups by remember { mutableStateOf(true) }
    var groupsError by remember { mutableStateOf<String?>(null) }

    val loadGroups: () -> Unit = {
        scope.launch {
            isLoadingGroups = true
            groupsError = null
            runAuthenticatedCall(context, onError = { groupsError = it }) { auth ->
                RetrofitClient.presupuestoApi.obtenerGrupos(auth)
            }?.let { response ->
                groups = response.grupos.filter { it.es_activo_grupo }
            }
            isLoadingGroups = false
        }
    }

    LaunchedEffect(Unit) { loadGroups() }

    // Id del presupuesto cuyo detalle se está viendo (tap sobre una
    // tarjeta). El diálogo pide el detalle con GET /api/v1/presupuestos/{id},
    // que incluye el desglose por grupo.
    var selectedBudgetId by remember { mutableStateOf<String?>(null) }
    selectedBudgetId?.let { budgetId ->
        BudgetDetailDialog(
            budgetId = budgetId,
            onDismiss = { selectedBudgetId = null },
            // Tras editar, redistribuir o cerrar el presupuesto, la lista se
            // vuelve a pedir para no mostrar datos viejos.
            onChanged = loadBudgets
        )
    }

    var isAddingBudget by remember { mutableStateOf(false) }
    if (isAddingBudget) {
        AddBudgetDialog(
            groups = groups,
            onDismiss = { isAddingBudget = false },
            // Se ejecuta cuando el POST al backend tuvo éxito.
            onCreated = {
                isAddingBudget = false
                loadBudgets()
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
            isLoadingBudgets && allBudgets.isEmpty() -> {
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
            budgetsError != null && allBudgets.isEmpty() -> {
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
                        .clickable(enabled = !isLoadingGroups) {
                            if (groupsError != null) loadGroups() else isAddingBudget = true
                        }
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoadingGroups) {
                        CircularProgressIndicator(color = FinTrackNavy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = FinTrackNavy)
                        Text(
                            text = if (groupsError != null) "Reintentar" else "Agregar presupuesto",
                            color = FinTrackNavy,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (groupsError != null) {
                    Text(
                        text = groupsError.orEmpty(),
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
                text = budget.state.label(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = listOf(budget.code, budget.indicator).filter { it.isNotBlank() }.joinToString(" • "),
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
// lista), que incluye el desglose por grupo. Si el presupuesto está activo,
// permite editarlo, cambiar su distribución, finalizarlo o cancelarlo.
@Composable
private fun BudgetDetailDialog(
    budgetId: String,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var budget by remember { mutableStateOf<AppBudget?>(null) }
    var budgetGroups by remember { mutableStateOf<List<PeriodoGrupoResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var pendingAccionCierre by remember { mutableStateOf<AccionCierrePresupuesto?>(null) }
    var isClosing by remember { mutableStateOf(false) }
    var closeError by remember { mutableStateOf<String?>(null) }

    var isEditing by remember { mutableStateOf(false) }
    var isEditingDistribution by remember { mutableStateOf(false) }

    // Las operaciones del backend no documentan su respuesta, así que después
    // de cada cambio se vuelve a pedir el detalle (y la lista, vía onChanged).
    val loadDetail: () -> Unit = {
        scope.launch {
            isLoading = budget == null
            loadError = null
            runAuthenticatedCall(context, onError = { loadError = it }) { auth ->
                RetrofitClient.presupuestoApi.obtenerPresupuesto(auth, budgetId)
            }?.let { response ->
                val periodo = response.periodo
                if (periodo == null) {
                    loadError = "El servidor respondió algo inesperado. Intenta de nuevo más tarde."
                } else {
                    budget = periodo.toAppBudget()
                    budgetGroups = response.grupos.orEmpty()
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(budgetId) { loadDetail() }

    val afterChange: () -> Unit = {
        loadDetail()
        onChanged()
    }

    val currentBudget = budget
    if (isEditing && currentBudget != null) {
        EditBudgetDialog(
            budget = currentBudget,
            onDismiss = { isEditing = false },
            onSaved = {
                isEditing = false
                afterChange()
            }
        )
    }

    if (isEditingDistribution) {
        EditDistributionDialog(
            budgetId = budgetId,
            groups = budgetGroups,
            onDismiss = { isEditingDistribution = false },
            onSaved = {
                isEditingDistribution = false
                afterChange()
            }
        )
    }

    // Confirmación antes de cerrar el presupuesto (acción irreversible):
    // finalizarlo o cancelarlo.
    val accionPendiente = pendingAccionCierre
    if (accionPendiente != null) {
        AlertDialog(
            onDismissRequest = { if (!isClosing) pendingAccionCierre = null },
            title = {
                Text(
                    text = if (accionPendiente == AccionCierrePresupuesto.FINALIZAR) "Finalizar presupuesto" else "Cancelar presupuesto",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (accionPendiente == AccionCierrePresupuesto.FINALIZAR) {
                        "El presupuesto se cerrará como finalizado y dejará de contabilizar gastos nuevos. ¿Deseas continuar?"
                    } else {
                        "El presupuesto se cancelará y dejará de estar activo. ¿Deseas continuar?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isClosing = true
                            closeError = null
                            val result = runAuthenticatedCall(context, onError = { closeError = it }) { auth ->
                                if (accionPendiente == AccionCierrePresupuesto.FINALIZAR) {
                                    RetrofitClient.presupuestoApi.finalizarPresupuesto(auth, budgetId)
                                } else {
                                    RetrofitClient.presupuestoApi.cancelarPresupuesto(auth, budgetId)
                                }
                            }
                            result?.close()
                            if (result != null) afterChange()
                            pendingAccionCierre = null
                            isClosing = false
                        }
                    },
                    enabled = !isClosing
                ) {
                    Text(
                        text = if (accionPendiente == AccionCierrePresupuesto.FINALIZAR) "Finalizar" else "Cancelar presupuesto",
                        color = FinTrackRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAccionCierre = null }, enabled = !isClosing) {
                    Text(text = "Volver")
                }
            }
        )
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
                currentBudget == null -> Text(text = loadError.orEmpty(), color = FinTrackRed)
                else -> {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (currentBudget.code.isNotBlank()) BudgetDetailRow("Código", currentBudget.code)
                        BudgetDetailRow("Período", "${formatShortDate(currentBudget.startDate)} – ${formatShortDate(currentBudget.endDate)}")
                        BudgetDetailRow("Estado", currentBudget.state.label())
                        BudgetDetailRow("Indicador", currentBudget.indicator)
                        BudgetDetailRow("Monto planeado", formatCurrency(currentBudget.plannedAmount))
                        BudgetDetailRow("Límite disponible", formatCurrency(currentBudget.limitAmount))
                        BudgetDetailRow("Consumido", formatCurrency(currentBudget.consumedAmount))
                        if (currentBudget.exceededAmount > 0) {
                            BudgetDetailRow("Excedido", formatCurrency(currentBudget.exceededAmount), valueColor = FinTrackRed)
                        } else {
                            BudgetDetailRow("Restante", formatCurrency(currentBudget.remainingAmount))
                        }
                        BudgetDetailRow("Avance", "${currentBudget.consumedPercent.toInt()}%")
                        if (currentBudget.isOverdue) {
                            BudgetDetailRow("Período vencido", "Sí", valueColor = FinTrackRed)
                        }
                        Text(
                            text = "El límite disponible es el monto planeado ajustado a tus ingresos reales del período.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (budgetGroups.isNotEmpty()) {
                            Text(
                                text = "Distribución por grupo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                            budgetGroups.forEach { group -> BudgetDetailGroupRow(group) }
                        }

                        if (loadError != null) {
                            Text(
                                text = loadError.orEmpty(),
                                color = FinTrackRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (currentBudget.state == EstadoPresupuesto.ACTIVO) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Editar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FinTrackNavy,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { isEditing = true }
                                )
                                if (budgetGroups.isNotEmpty()) {
                                    Text(
                                        text = "Cambiar distribución",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = FinTrackNavy,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { isEditingDistribution = true }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Cancelar presupuesto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FinTrackRed,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { pendingAccionCierre = AccionCierrePresupuesto.CANCELAR }
                                )
                                Text(
                                    text = "Finalizar presupuesto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FinTrackNavy,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { pendingAccionCierre = AccionCierrePresupuesto.FINALIZAR }
                                )
                            }
                        }
                        if (closeError != null) {
                            Text(
                                text = closeError.orEmpty(),
                                color = FinTrackRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
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

// Una fila del desglose por grupo: nombre + % asignado, barra de progreso
// de ese grupo, y monto consumido/asignado.
@Composable
private fun BudgetDetailGroupRow(group: PeriodoGrupoResponse, modifier: Modifier = Modifier) {
    val limite = group.monto_presupuesto_grupo?.toDoubleOrNull() ?: 0.0
    val consumido = group.aplicado_dinero_grupo?.toDoubleOrNull() ?: 0.0
    val excedido = group.monto_excedido_grupo?.toDoubleOrNull() ?: 0.0
    val isExceeded = excedido > 0
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        LabeledProgressBar(
            label = "${group.nombre_grupo.orEmpty()} (${group.porcentaje_asignado_grupo ?: 0}%)",
            valueText = "${group.porcentaje_utilizado_grupo?.toDoubleOrNull()?.toInt() ?: 0}%",
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
                text = group.estado_consumo_grupo.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = if (isExceeded) FinTrackRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Campo de fecha que abre un DatePicker al tocarlo. El DatePicker trabaja
// en milisegundos UTC, por eso la conversión usa ZoneOffset.UTC.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetDateField(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
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

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, if (enabled) FinTrackNavy else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .clickable(enabled = enabled) { showDatePicker = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = if (enabled) FinTrackNavy else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = date?.let { formatShortDate(it.toString()) } ?: "Elegir fecha",
                color = if (date != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

// Lee los porcentajes capturados; null si alguno no es un entero de 0 a 100.
private fun SnapshotStateMap<String, String>.parsedPercentages(groups: List<GroupShare>): Map<String, Int>? {
    val parsed = groups.associate { it.id to this[it.id]?.toIntOrNull() }
    if (parsed.values.any { it == null || it !in 0..100 }) return null
    return parsed.mapValues { it.value!! }
}

// Editor del % asignado a cada grupo. Muestra el total, que debe sumar 100.
@Composable
private fun GroupPercentagesEditor(
    groups: List<GroupShare>,
    percentageTexts: SnapshotStateMap<String, String>,
    onChange: () -> Unit
) {
    groups.forEach { group ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 12.dp)
            )
            OutlinedTextField(
                value = percentageTexts[group.id].orEmpty(),
                onValueChange = { percentageTexts[group.id] = it; onChange() },
                // Como sufijo (no como label) para que el "%" quede en el mismo
                // renglón que el número en lugar de flotar arriba del campo.
                suffix = { Text("%") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(90.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
    val total = groups.sumOf { percentageTexts[it.id]?.toIntOrNull() ?: 0 }
    Text(
        text = "Total: $total% (debe sumar 100%)",
        style = MaterialTheme.typography.bodySmall,
        color = if (total == 100) MaterialTheme.colorScheme.onSurfaceVariant else FinTrackRed,
        modifier = Modifier.padding(top = 8.dp)
    )
}

// Popup para crear un presupuesto nuevo. Manda el POST al backend él mismo
// y, si tuvo éxito, avisa a BudgetScreen para que recargue la lista.
@Composable
private fun AddBudgetDialog(
    groups: List<GrupoResponse>,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tipo by remember { mutableStateOf(TipoPeriodo.MENSUAL) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var customEndDate by remember { mutableStateOf<LocalDate?>(null) }
    // El código se sugiere a partir del tipo y la fecha de inicio mientras el
    // usuario no lo haya escrito a mano.
    var codigoText by remember { mutableStateOf<String?>(null) }
    var montoText by remember { mutableStateOf("") }
    val shares = remember(groups) { groups.map { GroupShare(it.id_grupo, it.nombre_grupo) } }
    val percentageTexts = remember(groups) {
        mutableStateMapOf<String, String>().apply {
            groups.forEach { put(it.id_grupo, it.porcentaje_sugerido_grupo.toString()) }
        }
    }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val codigo = codigoText ?: suggestedPeriodCode(tipo, startDate)
    val endDate = tipo.endDateFor(startDate) ?: customEndDate
    val monto = montoText.toDoubleOrNull()
    val percentages = percentageTexts.parsedPercentages(shares)
    val isValid = codigo.isNotBlank() && codigo.length <= 50 &&
        monto != null && monto > 0 &&
        endDate != null && !endDate.isBefore(startDate) &&
        shares.isNotEmpty() && percentages != null && percentages.values.sum() == 100

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = "Nuevo presupuesto", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Tipo de período",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TipoPeriodo.entries.forEach { option ->
                        BudgetSelectableChip(
                            label = option.label(),
                            selected = option == tipo,
                            onClick = { tipo = option; errorMessage = null }
                        )
                    }
                }

                BudgetDateField(
                    label = "Fecha inicio",
                    date = startDate,
                    onDateSelected = { startDate = it; errorMessage = null }
                )
                BudgetDateField(
                    label = if (tipo == TipoPeriodo.PERSONALIZADO) "Fecha fin" else "Fecha fin (automática)",
                    date = endDate,
                    onDateSelected = { customEndDate = it; errorMessage = null },
                    enabled = tipo == TipoPeriodo.PERSONALIZADO
                )
                if (endDate != null && endDate.isBefore(startDate)) {
                    Text(
                        text = "La fecha fin no puede ser anterior a la de inicio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinTrackRed,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigoText = it; errorMessage = null },
                    label = { Text("Código del período") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it; errorMessage = null },
                    label = { Text("Monto total") },
                    placeholder = { Text("Ej. 5000.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Distribución por grupo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = "Qué % del monto total le toca a cada grupo. Se precargan los porcentajes sugeridos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (shares.isEmpty()) {
                    Text(
                        text = "No se encontraron grupos para repartir el presupuesto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinTrackRed,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    GroupPercentagesEditor(shares, percentageTexts, onChange = { errorMessage = null })
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
                    val montoValue = monto ?: return@Button
                    val fechaFin = endDate ?: return@Button
                    val grupos = shares.map { share ->
                        GrupoDistribucionRequest(
                            grupo_id = share.id,
                            porcentaje_asignado_grupo = percentages?.get(share.id) ?: 0
                        )
                    }
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        val created = runAuthenticatedCall(context, onError = { errorMessage = it }) { auth ->
                            RetrofitClient.presupuestoApi.crearPresupuesto(
                                authorization = auth,
                                request = CrearPresupuestoRequest(
                                    codigo_periodo = codigo.trim(),
                                    tipo_periodo = tipo,
                                    fecha_inicio_periodo = startDate.toString(),
                                    fecha_fin_periodo = fechaFin.toString(),
                                    monto_total_presupuesto = formatAmountForApi(montoValue),
                                    grupos = grupos
                                )
                            )
                        }
                        created?.close()
                        isSaving = false
                        if (created != null) onCreated()
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

// Popup para editar un presupuesto activo (PATCH /api/v1/presupuestos/{id}):
// el backend solo permite cambiar la fecha fin y el monto total, y solo se
// mandan los campos que cambiaron.
@Composable
private fun EditBudgetDialog(
    budget: AppBudget,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val originalEndDate = remember(budget) { runCatching { LocalDate.parse(budget.endDate) }.getOrNull() }
    val startDate = remember(budget) { runCatching { LocalDate.parse(budget.startDate) }.getOrNull() }
    var endDate by remember { mutableStateOf(originalEndDate) }
    var montoText by remember { mutableStateOf(formatAmountForApi(budget.plannedAmount)) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val monto = montoText.toDoubleOrNull()
    val montoChanged = monto != null && monto != budget.plannedAmount
    val endDateChanged = endDate != null && endDate != originalEndDate
    val endDateValid = endDate == null || startDate == null || !endDate!!.isBefore(startDate)
    val isValid = monto != null && monto > 0 && endDateValid && (montoChanged || endDateChanged)

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = "Editar presupuesto", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it; errorMessage = null },
                    label = { Text("Monto total") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                BudgetDateField(
                    label = "Fecha fin",
                    date = endDate,
                    onDateSelected = { endDate = it; errorMessage = null }
                )
                if (!endDateValid) {
                    Text(
                        text = "La fecha fin no puede ser anterior a la de inicio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinTrackRed,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
                    val request = ActualizarPresupuestoRequest(
                        fecha_fin_periodo = if (endDateChanged) endDate.toString() else null,
                        monto_total_presupuesto = if (montoChanged) formatAmountForApi(monto!!) else null
                    )
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        val saved = runAuthenticatedCall(context, onError = { errorMessage = it }) { auth ->
                            RetrofitClient.presupuestoApi.actualizarPresupuesto(auth, budget.id, request)
                        }
                        saved?.close()
                        isSaving = false
                        if (saved != null) onSaved()
                    }
                },
                enabled = isValid && !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackNavy)
            ) {
                Text(text = if (isSaving) "Guardando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(text = "Cancelar")
            }
        }
    )
}

// Popup para cambiar cuánto % le toca a cada grupo en un presupuesto activo
// (PUT /api/v1/presupuestos/{id}/distribucion).
@Composable
private fun EditDistributionDialog(
    budgetId: String,
    groups: List<PeriodoGrupoResponse>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val shares = remember(groups) {
        groups.mapNotNull { group -> group.grupo_id?.let { GroupShare(it, group.nombre_grupo.orEmpty()) } }
    }
    val percentageTexts = remember(groups) {
        mutableStateMapOf<String, String>().apply {
            groups.forEach { group -> group.grupo_id?.let { put(it, (group.porcentaje_asignado_grupo ?: 0).toString()) } }
        }
    }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val percentages = percentageTexts.parsedPercentages(shares)
    val isValid = shares.isNotEmpty() && percentages != null && percentages.values.sum() == 100

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = "Cambiar distribución", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Qué % del presupuesto le toca a cada grupo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GroupPercentagesEditor(shares, percentageTexts, onChange = { errorMessage = null })
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
                    val grupos = shares.map { share ->
                        GrupoDistribucionRequest(
                            grupo_id = share.id,
                            porcentaje_asignado_grupo = percentages?.get(share.id) ?: 0
                        )
                    }
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        val saved = runAuthenticatedCall(context, onError = { errorMessage = it }) { auth ->
                            RetrofitClient.presupuestoApi.actualizarDistribucion(auth, budgetId, grupos)
                        }
                        saved?.close()
                        isSaving = false
                        if (saved != null) onSaved()
                    }
                },
                enabled = isValid && !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackNavy)
            ) {
                Text(text = if (isSaving) "Guardando..." else "Guardar")
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
