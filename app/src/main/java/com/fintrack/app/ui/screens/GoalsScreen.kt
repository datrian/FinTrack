package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
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
import com.fintrack.app.data.model.AppGoal
import com.fintrack.app.data.model.EstadoMeta
import com.fintrack.app.data.remote.CrearMetaRequest
import com.fintrack.app.data.remote.CuentaListaItem
import com.fintrack.app.data.remote.MetaResponse
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

// Los 4 filtros disponibles sobre la lista de metas, cada uno atado al
// estado real que acepta el backend (query param "estado").
private enum class GoalFilter(val label: String, val estado: EstadoMeta) {
    ACTIVAS("Activas", EstadoMeta.ACTIVA),
    ALCANZADAS("Alcanzadas", EstadoMeta.ALCANZADA),
    NO_ALCANZADAS("No alcanzadas", EstadoMeta.NO_ALCANZADA),
    CANCELADAS("Canceladas", EstadoMeta.CANCELADA)
}

private fun MetaResponse.toAppGoal() = AppGoal(
    id = id_meta,
    accountId = cuenta_id,
    name = nombre_meta,
    description = descripcion_meta,
    targetAmount = monto_objetivo_meta.toDoubleOrNull() ?: 0.0,
    currentAmount = monto_actual_meta.toDoubleOrNull() ?: 0.0,
    remainingAmount = monto_faltante_meta.toDoubleOrNull() ?: 0.0,
    progressPercent = porcentaje_avance_meta.toDoubleOrNull() ?: 0.0,
    deadline = fecha_limite_meta,
    state = estado_meta,
    isOverdue = plazo_vencido
)

// Pantalla de Metas: metas de ahorro reales del usuario, traídas de
// GET /api/v1/metas, con filtro por estado y un botón para crear una nueva
// (POST /api/v1/metas).
@Composable
fun GoalsScreen(onOpenProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf(GoalFilter.ACTIVAS) }

    var goals by remember { mutableStateOf<List<AppGoal>>(emptyList()) }
    var isLoadingGoals by remember { mutableStateOf(true) }
    var goalsError by remember { mutableStateOf<String?>(null) }

    val loadGoals: () -> Unit = {
        scope.launch {
            isLoadingGoals = true
            goalsError = null
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                val response = RetrofitClient.metaApi.obtenerMetas(
                    authorization = "Bearer $token",
                    estado = selectedFilter.estado
                )
                goals = response.metas.map { it.toAppGoal() }
            } catch (e: HttpException) {
                goalsError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
            } catch (e: IOException) {
                goalsError = "No se pudo conectar con el servidor. Revisa tu conexión."
            } catch (e: IllegalStateException) {
                goalsError = "Tu sesión expiró. Vuelve a iniciar sesión."
            } finally {
                isLoadingGoals = false
            }
        }
    }

    // Se vuelve a pedir cada vez que cambia el filtro (el backend filtra con
    // el query param "estado", no se filtra en el cliente).
    LaunchedEffect(selectedFilter) { loadGoals() }

    // Cuentas del usuario, necesarias para el formulario de "Nueva meta" y
    // para mostrar el nombre de la cuenta en cada tarjeta. Se traen una sola
    // vez al entrar.
    var accounts by remember { mutableStateOf<List<CuentaListaItem>>(emptyList()) }
    var isLoadingAccounts by remember { mutableStateOf(true) }
    var accountsError by remember { mutableStateOf<String?>(null) }

    val loadAccounts: () -> Unit = {
        scope.launch {
            isLoadingAccounts = true
            accountsError = null
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                accounts = RetrofitClient.cuentaApi.obtenerCuentas("Bearer $token").cuentas
            } catch (e: HttpException) {
                accountsError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
            } catch (e: IOException) {
                accountsError = "No se pudo conectar con el servidor. Revisa tu conexión."
            } catch (e: IllegalStateException) {
                accountsError = "Tu sesión expiró. Vuelve a iniciar sesión."
            } finally {
                isLoadingAccounts = false
            }
        }
    }

    LaunchedEffect(Unit) { loadAccounts() }

    var isAddingGoal by remember { mutableStateOf(false) }
    if (isAddingGoal) {
        AddGoalDialog(
            accounts = accounts,
            onDismiss = { isAddingGoal = false },
            // Se ejecuta cuando el POST al backend tuvo éxito.
            onCreated = { newGoal ->
                if (selectedFilter.estado == newGoal.state) {
                    goals = listOf(newGoal) + goals
                }
                isAddingGoal = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackTopBar(title = "Metas", onMenuClick = onOpenProfile) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoalFilter.entries.forEach { filter ->
                    GoalFilterChip(
                        label = filter.label,
                        selected = filter == selectedFilter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }
        }

        when {
            isLoadingGoals && goals.isEmpty() -> {
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
            goalsError != null && goals.isEmpty() -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = goalsError.orEmpty(),
                            color = FinTrackRed,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, FinTrackNavy, RoundedCornerShape(16.dp))
                                .clickable { loadGoals() }
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "Reintentar", color = FinTrackNavy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            goals.isEmpty() -> {
                item {
                    Text(
                        text = "No tienes metas en este estado todavía.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
            }
            else -> {
                items(goals) { goal ->
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        GoalCard(
                            goal = goal,
                            accountName = accounts.firstOrNull { it.id_cuenta == goal.accountId }?.nombre_cuenta
                        )
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
                        .clickable(enabled = !isLoadingAccounts) {
                            if (accountsError != null) loadAccounts() else isAddingGoal = true
                        }
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoadingAccounts) {
                        CircularProgressIndicator(color = FinTrackNavy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = FinTrackNavy)
                        Text(
                            text = if (accountsError != null) "Reintentar" else "Agregar meta",
                            color = FinTrackNavy,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (accountsError != null) {
                    Text(
                        text = accountsError.orEmpty(),
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
private fun GoalFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

// Tarjeta de una meta: nombre + cuenta, barra de progreso (actual/objetivo),
// montos y fecha límite (en rojo si ya venció sin alcanzarse).
@Composable
private fun GoalCard(goal: AppGoal, accountName: String?, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        Text(
            text = goal.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!accountName.isNullOrBlank()) {
            Text(
                text = accountName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
        }
        if (!goal.description.isNullOrBlank()) {
            Text(
                text = goal.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        LabeledProgressBar(
            label = formatCurrency(goal.currentAmount),
            valueText = "${goal.progressPercent.toInt().coerceIn(0, 100)}%",
            progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f,
            progressColor = if (goal.state == EstadoMeta.ALCANZADA) FinTrackGreen else FinTrackNavy
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Objetivo: ${formatCurrency(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Límite: ${formatShortDate(goal.deadline)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (goal.isOverdue) FinTrackRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Popup para crear una meta nueva. Manda el POST al backend él mismo y, si
// tuvo éxito, le pasa la meta ya creada a GoalsScreen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    accounts: List<CuentaListaItem>,
    onDismiss: () -> Unit,
    onCreated: (AppGoal) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedAccountId by remember { mutableStateOf<String?>(accounts.firstOrNull()?.id_cuenta) }
    var nombre by remember { mutableStateOf("") }
    var montoText by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
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

    val monto = montoText.toDoubleOrNull()
    val selectedDate = selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    }
    val isValid = selectedAccountId != null && nombre.isNotBlank() &&
        monto != null && monto > 0 && selectedDate != null

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = "Nueva meta", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Cuenta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (accounts.isEmpty()) {
                    Text(
                        text = "Necesitas al menos una cuenta para crear una meta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinTrackRed
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { account ->
                            GoalSelectableChip(
                                label = account.nombre_cuenta,
                                selected = account.id_cuenta == selectedAccountId,
                                onClick = { selectedAccountId = account.id_cuenta }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la meta") },
                    placeholder = { Text("Ej. Fondo de emergencia") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it; errorMessage = null },
                    label = { Text("Monto objetivo") },
                    placeholder = { Text("Ej. 5000.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Fecha límite",
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

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    placeholder = { Text("Ej. Ahorro para imprevistos") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                )

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
                    val accountId = selectedAccountId ?: return@Button
                    val montoValue = monto ?: return@Button
                    val fecha = selectedDate ?: return@Button
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val token = SessionManager.getAccessToken(context)
                                ?: throw IllegalStateException("No hay sesión activa")
                            val created = RetrofitClient.metaApi.crearMeta(
                                authorization = "Bearer $token",
                                request = CrearMetaRequest(
                                    cuenta_id = accountId,
                                    nombre_meta = nombre.trim(),
                                    // El backend exige el monto como texto decimal, no como número.
                                    monto_objetivo_meta = String.format(java.util.Locale.US, "%.2f", montoValue),
                                    fecha_limite_meta = fecha.toString(),
                                    descripcion_meta = descripcion.trim().ifBlank { null }
                                )
                            )
                            onCreated(created.toAppGoal())
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
private fun GoalSelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
