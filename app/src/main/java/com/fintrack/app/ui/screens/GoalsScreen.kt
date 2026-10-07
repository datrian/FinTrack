package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.fintrack.app.data.model.TipoAsignacion
import com.fintrack.app.data.remote.ActualizarMetaRequest
import com.fintrack.app.data.remote.AsignacionCrearRequest
import com.fintrack.app.data.remote.AsignacionResponse
import com.fintrack.app.data.remote.CrearMetaRequest
import com.fintrack.app.data.remote.CuentaListaItem
import com.fintrack.app.data.remote.MetaResponse
import com.fintrack.app.data.remote.ResumenCuentaMetaResponse
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

    // Id de la meta cuyo detalle se está viendo (tap sobre una tarjeta). El
    // diálogo pide el detalle con GET /api/v1/metas/{id} (no reutiliza los
    // datos ya cargados en la lista) y permite editar nombre/descripción
    // con PATCH /api/v1/metas/{id}.
    var selectedGoalId by remember { mutableStateOf<String?>(null) }
    selectedGoalId?.let { goalId ->
        GoalDetailDialog(
            goalId = goalId,
            onDismiss = { selectedGoalId = null },
            onUpdated = { updated ->
                goals = goals.map { if (it.id == updated.id) updated else it }
            }
        )
    }

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
                            accountName = accounts.firstOrNull { it.id_cuenta == goal.accountId }?.nombre_cuenta,
                            onClick = { selectedGoalId = goal.id }
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
private fun GoalCard(goal: AppGoal, accountName: String?, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier.clickable(onClick = onClick)) {
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

// Popup de detalle de una meta. Pide el registro completo con
// GET /api/v1/metas/{id} (no reutiliza los datos ya cargados en la lista),
// e incluye el resumen de la cuenta asociada. Permite editar nombre y
// descripción (lo único que acepta PATCH /api/v1/metas/{id}; el monto
// objetivo y la fecha límite no se pueden cambiar una vez creada la meta).
@Composable
private fun GoalDetailDialog(
    goalId: String,
    onDismiss: () -> Unit,
    onUpdated: (AppGoal) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var goal by remember { mutableStateOf<AppGoal?>(null) }
    var resumen by remember { mutableStateOf<ResumenCuentaMetaResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var isEditing by remember { mutableStateOf(false) }
    var nombreEdit by remember { mutableStateOf("") }
    var descripcionEdit by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    var asignaciones by remember { mutableStateOf<List<AsignacionResponse>>(emptyList()) }
    var isLoadingAsignaciones by remember { mutableStateOf(true) }
    var asignacionesError by remember { mutableStateOf<String?>(null) }
    var isAddingAsignacion by remember { mutableStateOf(false) }

    // Función normal (no lambda) para poder encadenarla con suspend fun
    // después de crear una asignación (el monto actual/avance/estado de la
    // meta cambian al crear un aporte o retiro).
    suspend fun fetchDetail() {
        isLoading = true
        loadError = null
        try {
            val token = SessionManager.getAccessToken(context)
                ?: throw IllegalStateException("No hay sesión activa")
            val response = RetrofitClient.metaApi.obtenerMeta("Bearer $token", goalId)
            goal = response.meta.toAppGoal()
            resumen = response.resumen_cuenta
            nombreEdit = response.meta.nombre_meta
            descripcionEdit = response.meta.descripcion_meta.orEmpty()
            onUpdated(response.meta.toAppGoal())
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

    suspend fun fetchAsignaciones() {
        isLoadingAsignaciones = true
        asignacionesError = null
        try {
            val token = SessionManager.getAccessToken(context)
                ?: throw IllegalStateException("No hay sesión activa")
            asignaciones = RetrofitClient.metaApi.obtenerAsignaciones("Bearer $token", goalId).asignaciones
        } catch (e: HttpException) {
            asignacionesError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
        } catch (e: IOException) {
            asignacionesError = "No se pudo conectar con el servidor. Revisa tu conexión."
        } catch (e: IllegalStateException) {
            asignacionesError = "Tu sesión expiró. Vuelve a iniciar sesión."
        } finally {
            isLoadingAsignaciones = false
        }
    }

    LaunchedEffect(goalId) {
        fetchDetail()
        fetchAsignaciones()
    }

    if (isAddingAsignacion) {
        AddAsignacionDialog(
            goalId = goalId,
            onDismiss = { isAddingAsignacion = false },
            onCreated = { newAsignacion ->
                asignaciones = listOf(newAsignacion) + asignaciones
                isAddingAsignacion = false
                // El aporte/retiro cambia monto actual, avance y posiblemente
                // el estado de la meta (ej. pasa a ALCANZADA): se vuelve a
                // pedir el detalle para reflejarlo.
                scope.launch { fetchDetail() }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = if (isEditing) "Editar meta" else "Detalle de la meta", fontWeight = FontWeight.Bold) },
        text = {
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = FinTrackNavy) }
                loadError != null -> Text(text = loadError.orEmpty(), color = FinTrackRed)
                goal != null -> {
                    val current = goal!!
                    if (isEditing) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            OutlinedTextField(
                                value = nombreEdit,
                                onValueChange = { nombreEdit = it; saveError = null },
                                label = { Text("Nombre de la meta") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = descripcionEdit,
                                onValueChange = { descripcionEdit = it },
                                label = { Text("Descripción (opcional)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (saveError != null) {
                                Text(
                                    text = saveError.orEmpty(),
                                    color = FinTrackRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            GoalDetailRow("Estado", when (current.state) {
                                EstadoMeta.ACTIVA -> "Activa"
                                EstadoMeta.ALCANZADA -> "Alcanzada"
                                EstadoMeta.NO_ALCANZADA -> "No alcanzada"
                                EstadoMeta.CANCELADA -> "Cancelada"
                            })
                            GoalDetailRow("Monto actual", formatCurrency(current.currentAmount))
                            GoalDetailRow("Objetivo", formatCurrency(current.targetAmount))
                            GoalDetailRow("Faltante", formatCurrency(current.remainingAmount))
                            GoalDetailRow("Avance", "${current.progressPercent.toInt()}%")
                            GoalDetailRow(
                                "Fecha límite",
                                formatShortDate(current.deadline),
                                valueColor = if (current.isOverdue) FinTrackRed else MaterialTheme.colorScheme.onSurface
                            )
                            current.description?.takeIf { it.isNotBlank() }?.let {
                                GoalDetailRow("Descripción", it)
                            }
                            resumen?.let { summary ->
                                Text(
                                    text = "Resumen de la cuenta",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                                GoalDetailRow("Saldo actual", formatCurrency(summary.saldo_actual_cuenta.toDoubleOrNull() ?: 0.0))
                                GoalDetailRow("Reservado en metas", formatCurrency(summary.reserva_total_cuenta.toDoubleOrNull() ?: 0.0))
                                GoalDetailRow("Dinero libre", formatCurrency(summary.dinero_libre_cuenta.toDoubleOrNull() ?: 0.0))
                                GoalDetailRow("Disponible para aportar", formatCurrency(summary.disponible_para_aportar.toDoubleOrNull() ?: 0.0))
                                val deficit = summary.deficit_reservas_cuenta.toDoubleOrNull() ?: 0.0
                                if (deficit > 0) {
                                    GoalDetailRow("Déficit de reservas", formatCurrency(deficit), valueColor = FinTrackRed)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aportes y retiros",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "+ Agregar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinTrackNavy,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { isAddingAsignacion = true }
                                )
                            }
                            when {
                                isLoadingAsignaciones -> Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = FinTrackNavy, strokeWidth = 2.dp, modifier = Modifier.size(20.dp)) }
                                asignacionesError != null -> Text(
                                    text = asignacionesError.orEmpty(),
                                    color = FinTrackRed,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                asignaciones.isEmpty() -> Text(
                                    text = "Todavía no hay aportes ni retiros.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                else -> asignaciones.forEach { asignacion ->
                                    AsignacionRow(asignacion)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(
                    onClick = {
                        val nombre = nombreEdit.trim()
                        if (nombre.isEmpty()) return@Button
                        scope.launch {
                            isSaving = true
                            saveError = null
                            try {
                                val token = SessionManager.getAccessToken(context)
                                    ?: throw IllegalStateException("No hay sesión activa")
                                val updated = RetrofitClient.metaApi.actualizarMeta(
                                    authorization = "Bearer $token",
                                    idMeta = goalId,
                                    request = ActualizarMetaRequest(
                                        nombre_meta = nombre,
                                        descripcion_meta = descripcionEdit.trim().ifBlank { null }
                                    )
                                )
                                goal = updated.toAppGoal()
                                onUpdated(updated.toAppGoal())
                                isEditing = false
                            } catch (e: HttpException) {
                                saveError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
                            } catch (e: IOException) {
                                saveError = "No se pudo conectar con el servidor. Revisa tu conexión."
                            } catch (e: IllegalStateException) {
                                saveError = "Tu sesión expiró. Vuelve a iniciar sesión."
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = nombreEdit.isNotBlank() && !isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinTrackNavy)
                ) {
                    Text(text = if (isSaving) "Guardando..." else "Guardar")
                }
            } else {
                TextButton(onClick = { isEditing = true }, enabled = goal != null) {
                    Text(text = "Editar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (isEditing) isEditing = false else onDismiss() },
                enabled = !isSaving
            ) {
                Text(text = if (isEditing) "Cancelar" else "Cerrar")
            }
        }
    )
}

@Composable
private fun GoalDetailRow(
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

// Una fila de la lista de aportes/retiros: tipo + fecha a la izquierda,
// monto con signo y color a la derecha, comentario (si hay) debajo.
@Composable
private fun AsignacionRow(asignacion: AsignacionResponse, modifier: Modifier = Modifier) {
    val isAporte = asignacion.tipo_asignacion == TipoAsignacion.APORTE
    val monto = asignacion.monto_asignacion.toDoubleOrNull() ?: 0.0
    Column(modifier = modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isAporte) "Aporte" else "Retiro",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatShortDate(asignacion.fecha_registro_asignacion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = (if (isAporte) "+" else "-") + formatCurrency(monto),
                color = if (isAporte) FinTrackGreen else FinTrackRed,
                fontWeight = FontWeight.Bold
            )
        }
        asignacion.comentario_asignacion?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Popup para crear un aporte o retiro sobre una meta. Manda el POST a
// /api/v1/metas/{id}/asignaciones él mismo y, si tuvo éxito, le pasa la
// asignación ya creada a GoalDetailDialog.
@Composable
private fun AddAsignacionDialog(
    goalId: String,
    onDismiss: () -> Unit,
    onCreated: (AsignacionResponse) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTipo by remember { mutableStateOf(TipoAsignacion.APORTE) }
    var montoText by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val monto = montoText.toDoubleOrNull()
    val isValid = monto != null && monto > 0

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = "Nuevo aporte o retiro", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Tipo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalSelectableChip(
                        label = "Aporte",
                        selected = selectedTipo == TipoAsignacion.APORTE,
                        onClick = { selectedTipo = TipoAsignacion.APORTE }
                    )
                    GoalSelectableChip(
                        label = "Retiro",
                        selected = selectedTipo == TipoAsignacion.RETIRO,
                        onClick = { selectedTipo = TipoAsignacion.RETIRO }
                    )
                }

                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it; errorMessage = null },
                    label = { Text("Monto") },
                    placeholder = { Text("Ej. 200.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text("Comentario (opcional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
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
                    val montoValue = monto ?: return@Button
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val token = SessionManager.getAccessToken(context)
                                ?: throw IllegalStateException("No hay sesión activa")
                            val created = RetrofitClient.metaApi.crearAsignacion(
                                authorization = "Bearer $token",
                                idMeta = goalId,
                                request = AsignacionCrearRequest(
                                    tipo_asignacion = selectedTipo,
                                    // El backend exige el monto como texto decimal, no como número.
                                    monto_asignacion = String.format(java.util.Locale.US, "%.2f", montoValue),
                                    comentario_asignacion = comentario.trim().ifBlank { null }
                                )
                            )
                            onCreated(created)
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
