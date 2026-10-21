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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.fintrack.app.data.model.AppTransaction
import com.fintrack.app.data.model.FlujoDinero
import com.fintrack.app.data.model.TipoTransaccion
import com.fintrack.app.data.model.labelOrGeneric
import com.fintrack.app.data.model.TransactionCategory
import com.fintrack.app.data.model.TransactionSubcategory
import com.fintrack.app.data.remote.CategoriaResponse
import com.fintrack.app.data.remote.CrearTransaccionRequest
import com.fintrack.app.data.remote.CuentaListaItem
import com.fintrack.app.data.remote.RetrofitClient
import com.fintrack.app.data.remote.SubcategoriaResponse
import com.fintrack.app.data.remote.TransaccionResponse
import com.fintrack.app.data.remote.parseApiErrorMessage
import com.fintrack.app.data.remote.runAuthenticatedCall
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.LoadMoreButton
import com.fintrack.app.ui.components.amountColor
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.components.formatShortDate
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackPurple
import com.fintrack.app.ui.theme.FinTrackRed
import java.io.IOException
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Los 3 filtros disponibles sobre la lista de movimientos, cada uno atado al
// tipo real que acepta el backend (null = sin filtrar, trae los 3 tipos).
private enum class TransactionFilter(val label: String, val tipo: TipoTransaccion?) {
    TODAS("Todas", null),
    GASTOS("Gastos", TipoTransaccion.GASTO),
    INGRESOS("Ingresos", TipoTransaccion.INGRESO)
}

private fun TransaccionResponse.toAppTransaction() = AppTransaction(
    id = id_transaccion,
    type = tipo_transaccion,
    accountId = cuenta_id,
    destinationAccountId = cuenta_destino_id,
    categoryId = categoria_id,
    subcategoryId = subcategoria_id,
    amount = monto_transaccion.toDoubleOrNull() ?: 0.0,
    date = fecha_registro_transaccion,
    comment = comentario_transaccion
)

private fun CategoriaResponse.toTransactionCategory() = TransactionCategory(
    id = id_categoria,
    name = nombre_categoria
)

private fun SubcategoriaResponse.toTransactionSubcategory() = TransactionSubcategory(
    id = id_subcategoria,
    categoryId = categoria_id,
    name = nombre_subcategoria
)

// Pantalla de Transacciones: lista de movimientos reales del usuario, traídos
// de GET /api/v1/transacciones, con filtro por tipo (todas/gastos/ingresos) y
// un botón para crear uno nuevo (POST /api/v1/transacciones).
@Composable
fun TransactionsScreen(onOpenProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf(TransactionFilter.TODAS) }

    var transactions by remember { mutableStateOf<List<AppTransaction>>(emptyList()) }
    var isLoadingTransactions by remember { mutableStateOf(true) }
    var transactionsError by remember { mutableStateOf<String?>(null) }
    // El backend pagina de 20 en 20: "total" dice cuántas hay en total para
    // saber si mostrar "Cargar más".
    var totalTransactions by remember { mutableStateOf(0) }
    var isLoadingMoreTransactions by remember { mutableStateOf(false) }

    val loadTransactions: () -> Unit = {
        scope.launch {
            isLoadingTransactions = true
            transactionsError = null
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                val response = RetrofitClient.transaccionApi.obtenerTransacciones(
                    authorization = "Bearer $token",
                    tipoTransaccion = selectedFilter.tipo
                )
                transactions = response.transacciones.map { it.toAppTransaction() }
                totalTransactions = response.total
            } catch (e: HttpException) {
                transactionsError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
            } catch (e: IOException) {
                transactionsError = "No se pudo conectar con el servidor. Revisa tu conexión."
            } catch (e: IllegalStateException) {
                transactionsError = "Tu sesión expiró. Vuelve a iniciar sesión."
            } finally {
                isLoadingTransactions = false
            }
        }
    }

    // Trae la siguiente página y la agrega al final. distinctBy evita
    // duplicados si se creó una transacción nueva desde que se cargó la lista
    // (eso recorre los offsets del backend).
    val loadMoreTransactions: () -> Unit = {
        scope.launch {
            isLoadingMoreTransactions = true
            runAuthenticatedCall(context, onError = { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() }) { auth ->
                RetrofitClient.transaccionApi.obtenerTransacciones(
                    authorization = auth,
                    tipoTransaccion = selectedFilter.tipo,
                    offset = transactions.size
                )
            }?.let { response ->
                transactions = (transactions + response.transacciones.map { it.toAppTransaction() }).distinctBy { it.id }
                totalTransactions = response.total
            }
            isLoadingMoreTransactions = false
        }
    }

    // Se vuelve a pedir cada vez que cambia el filtro (el backend filtra con
    // el query param tipo_transaccion, no se filtra en el cliente).
    LaunchedEffect(selectedFilter) { loadTransactions() }

    // Cuentas y categorías del usuario, necesarias para el formulario de
    // "Nueva transacción" (y las categorías también para mostrar el nombre
    // de cada movimiento en la lista). Se traen una sola vez al entrar.
    var accounts by remember { mutableStateOf<List<CuentaListaItem>>(emptyList()) }
    var categories by remember { mutableStateOf<List<TransactionCategory>>(emptyList()) }
    var isLoadingReferenceData by remember { mutableStateOf(true) }
    var referenceDataError by remember { mutableStateOf<String?>(null) }

    val loadReferenceData: () -> Unit = {
        scope.launch {
            isLoadingReferenceData = true
            referenceDataError = null
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                val authorization = "Bearer $token"
                accounts = RetrofitClient.cuentaApi.obtenerCuentas(authorization).cuentas
                categories = RetrofitClient.categoriaApi.obtenerCategorias(authorization).categorias.map { it.toTransactionCategory() }
            } catch (e: HttpException) {
                referenceDataError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
            } catch (e: IOException) {
                referenceDataError = "No se pudo conectar con el servidor. Revisa tu conexión."
            } catch (e: IllegalStateException) {
                referenceDataError = "Tu sesión expiró. Vuelve a iniciar sesión."
            } finally {
                isLoadingReferenceData = false
            }
        }
    }

    LaunchedEffect(Unit) { loadReferenceData() }

    // Id de la transacción cuyo detalle se está viendo (tap sobre una fila).
    // El diálogo pide el detalle con GET /api/v1/transacciones/{id}, no
    // reutiliza los datos ya cargados en la lista.
    var selectedTransactionId by remember { mutableStateOf<String?>(null) }
    selectedTransactionId?.let { transactionId ->
        TransactionDetailDialog(
            transactionId = transactionId,
            accounts = accounts,
            categories = categories,
            onDismiss = { selectedTransactionId = null }
        )
    }

    var isAddingTransaction by remember { mutableStateOf(false) }
    if (isAddingTransaction) {
        AddTransactionDialog(
            accounts = accounts,
            categories = categories,
            onDismiss = { isAddingTransaction = false },
            // Se ejecuta cuando el POST al backend tuvo éxito.
            onCreated = { newTransaction ->
                if (selectedFilter.tipo == null || selectedFilter.tipo == newTransaction.type) {
                    transactions = listOf(newTransaction) + transactions
                    totalTransactions += 1
                }
                isAddingTransaction = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackTopBar(title = "Transacciones", onMenuClick = onOpenProfile) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionFilter.entries.forEach { filter ->
                    FilterChip(
                        label = filter.label,
                        selected = filter == selectedFilter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }
        }

        when {
            isLoadingTransactions && transactions.isEmpty() -> {
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
            transactionsError != null && transactions.isEmpty() -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = transactionsError.orEmpty(),
                            color = FinTrackRed,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, FinTrackNavy, RoundedCornerShape(16.dp))
                                .clickable { loadTransactions() }
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "Reintentar", color = FinTrackNavy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            transactions.isEmpty() -> {
                item {
                    Text(
                        text = "No tienes transacciones todavía.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
            }
            else -> {
                items(transactions) { transaction ->
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        TransactionRow(
                            transaction = transaction,
                            categoryName = categories.firstOrNull { it.id == transaction.categoryId }?.name,
                            onClick = { selectedTransactionId = transaction.id }
                        )
                    }
                }
                if (transactions.size < totalTransactions) {
                    item {
                        LoadMoreButton(
                            remaining = totalTransactions - transactions.size,
                            isLoading = isLoadingMoreTransactions,
                            onClick = loadMoreTransactions,
                            modifier = Modifier.padding(horizontal = 20.dp)
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
                        .clickable(enabled = !isLoadingReferenceData) {
                            if (referenceDataError != null) loadReferenceData() else isAddingTransaction = true
                        }
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoadingReferenceData) {
                        CircularProgressIndicator(color = FinTrackNavy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = FinTrackNavy)
                        Text(
                            text = if (referenceDataError != null) "Reintentar" else "Agregar transacción",
                            color = FinTrackNavy,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (referenceDataError != null) {
                    Text(
                        text = referenceDataError.orEmpty(),
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

// Botoncito tipo "pastilla" para cada filtro (Todas/Gastos/Ingresos).
@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

// Pastilla simple de selección, reutilizada para tipo/cuenta/categoría/subcategoría.
@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = if (selected) FinTrackNavy else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.clickable { onClick() }
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

// Una fila de la lista: ícono según el tipo (gasto/ingreso/transferencia),
// categoría (si tiene) + fecha, comentario como título (o un nombre genérico
// si no escribieron uno), y el monto con signo y color según el tipo.
@Composable
private fun TransactionRow(
    transaction: AppTransaction,
    categoryName: String?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Ícono, signo y color dependen de si el dinero entra, sale o solo se
    // mueve entre cuentas propias (o el tipo es desconocido: sin signo).
    val flujo = transaction.type?.flujo ?: FlujoDinero.NEUTRO
    val (icon, iconBackground) = when (flujo) {
        FlujoDinero.ENTRADA -> Icons.Filled.ArrowDownward to FinTrackGreen.copy(alpha = 0.15f)
        FlujoDinero.SALIDA -> Icons.Filled.ArrowUpward to FinTrackNavy.copy(alpha = 0.1f)
        FlujoDinero.NEUTRO -> Icons.Filled.SwapHoriz to FinTrackPurple.copy(alpha = 0.12f)
    }
    val title = transaction.comment?.takeIf { it.isNotBlank() } ?: transaction.type.labelOrGeneric()
    val subtitle = if (categoryName != null) "$categoryName • ${formatShortDate(transaction.date)}" else formatShortDate(transaction.date)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = FinTrackNavy)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when (flujo) {
            FlujoDinero.NEUTRO -> Text(
                text = formatCurrency(transaction.amount),
                color = FinTrackNavy,
                fontWeight = FontWeight.Bold
            )
            FlujoDinero.ENTRADA -> Text(
                text = "+" + formatCurrency(transaction.amount),
                color = amountColor(true),
                fontWeight = FontWeight.Bold
            )
            FlujoDinero.SALIDA -> Text(
                text = "-" + formatCurrency(transaction.amount),
                color = amountColor(false),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Popup de detalle de una transacción. Pide el registro completo con
// GET /api/v1/transacciones/{id} (no reutiliza los datos ya cargados en la
// lista), y resuelve nombres de cuenta/categoría/subcategoría para mostrarlos.
@Composable
private fun TransactionDetailDialog(
    transactionId: String,
    accounts: List<CuentaListaItem>,
    categories: List<TransactionCategory>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var transaction by remember { mutableStateOf<AppTransaction?>(null) }
    var subcategoryName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(transactionId) {
        isLoading = true
        errorMessage = null
        try {
            val token = SessionManager.getAccessToken(context)
                ?: throw IllegalStateException("No hay sesión activa")
            val authorization = "Bearer $token"
            val response = RetrofitClient.transaccionApi.obtenerTransaccion(authorization, transactionId)
            transaction = response.toAppTransaction()
            val categoriaId = response.categoria_id
            val subcategoriaId = response.subcategoria_id
            subcategoryName = if (categoriaId != null && subcategoriaId != null) {
                RetrofitClient.categoriaApi.obtenerSubcategorias(authorization, categoriaId)
                    .subcategorias.firstOrNull { it.id_subcategoria == subcategoriaId }?.nombre_subcategoria
            } else null
        } catch (e: HttpException) {
            errorMessage = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
        } catch (e: IOException) {
            errorMessage = "No se pudo conectar con el servidor. Revisa tu conexión."
        } catch (e: IllegalStateException) {
            errorMessage = "Tu sesión expiró. Vuelve a iniciar sesión."
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Detalle de la transacción", fontWeight = FontWeight.Bold) },
        text = {
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = FinTrackNavy) }
                errorMessage != null -> Text(text = errorMessage.orEmpty(), color = FinTrackRed)
                transaction != null -> {
                    val current = transaction!!
                    Column {
                        DetailRow("Tipo", current.type.labelOrGeneric())
                        DetailRow("Monto", formatCurrency(current.amount))
                        DetailRow("Cuenta", accounts.firstOrNull { it.id_cuenta == current.accountId }?.nombre_cuenta ?: current.accountId)
                        current.destinationAccountId?.let { destinationId ->
                            DetailRow("Cuenta destino", accounts.firstOrNull { it.id_cuenta == destinationId }?.nombre_cuenta ?: destinationId)
                        }
                        current.categoryId?.let { categoryId ->
                            DetailRow("Categoría", categories.firstOrNull { it.id == categoryId }?.name ?: categoryId)
                        }
                        subcategoryName?.let { DetailRow("Subcategoría", it) }
                        DetailRow("Fecha", formatShortDate(current.date))
                        current.comment?.takeIf { it.isNotBlank() }?.let { DetailRow("Comentario", it) }
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
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

// Popup para crear una transacción nueva. Manda el POST al backend él mismo
// y, si tuvo éxito, le pasa la transacción ya creada a TransactionsScreen.
@Composable
private fun AddTransactionDialog(
    accounts: List<CuentaListaItem>,
    categories: List<TransactionCategory>,
    onDismiss: () -> Unit,
    onCreated: (AppTransaction) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(TipoTransaccion.GASTO) }
    var selectedAccountId by remember { mutableStateOf<String?>(accounts.firstOrNull()?.id_cuenta) }
    var selectedDestinationAccountId by remember { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedSubcategoryId by remember { mutableStateOf<String?>(null) }
    var subcategories by remember { mutableStateOf<List<TransactionSubcategory>>(emptyList()) }
    var isLoadingSubcategories by remember { mutableStateOf(false) }
    var montoText by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Sugerencia de categoría a partir del comentario
    // (GET /api/v1/analitica/sugerir-clasificacion). La subcategoría sugerida
    // queda pendiente hasta que se carguen las subcategorías de la categoría.
    var isSuggesting by remember { mutableStateOf(false) }
    var suggestionMessage by remember { mutableStateOf<String?>(null) }
    var pendingSuggestedSubcategoryId by remember { mutableStateOf<String?>(null) }

    // Al elegir una categoría se traen sus subcategorías (si ya había una
    // subcategoría elegida de otra categoría, se descarta).
    LaunchedEffect(selectedCategoryId) {
        selectedSubcategoryId = null
        val categoryId = selectedCategoryId
        if (categoryId == null) {
            subcategories = emptyList()
            return@LaunchedEffect
        }
        isLoadingSubcategories = true
        try {
            val token = SessionManager.getAccessToken(context)
            if (token != null) {
                subcategories = RetrofitClient.categoriaApi.obtenerSubcategorias("Bearer $token", categoryId)
                    .subcategorias.map { it.toTransactionSubcategory() }
            }
        } catch (e: Exception) {
            subcategories = emptyList()
        } finally {
            isLoadingSubcategories = false
        }
        pendingSuggestedSubcategoryId?.let { suggested ->
            if (subcategories.any { it.id == suggested }) selectedSubcategoryId = suggested
            pendingSuggestedSubcategoryId = null
        }
    }

    val suggestCategory: () -> Unit = {
        val detalle = comentario.trim()
        scope.launch {
            isSuggesting = true
            suggestionMessage = null
            val suggestion = runAuthenticatedCall(context, onError = { suggestionMessage = it }) { auth ->
                RetrofitClient.analiticaApi.sugerirClasificacion(auth, detalle)
            }
            val suggestedCategory = categories.firstOrNull { it.id == suggestion?.categoria_id }
            when {
                suggestion == null -> Unit
                suggestion.sugerencia_encontrada == true && suggestedCategory != null -> {
                    pendingSuggestedSubcategoryId = suggestion.subcategoria_id
                    if (selectedCategoryId == suggestedCategory.id) {
                        // La categoría ya estaba elegida: sus subcategorías ya
                        // están cargadas, se aplica la sugerida directamente.
                        if (subcategories.any { it.id == suggestion.subcategoria_id }) {
                            selectedSubcategoryId = suggestion.subcategoria_id
                        }
                        pendingSuggestedSubcategoryId = null
                    } else {
                        selectedCategoryId = suggestedCategory.id
                    }
                    val name = listOfNotNull(suggestedCategory.name, suggestion.nombre_subcategoria).joinToString(" › ")
                    suggestionMessage = "Sugerencia aplicada: $name. Puedes cambiarla si no es correcta."
                }
                else -> suggestionMessage = suggestion.explicacion_ux ?: "No hay una sugerencia para este gasto."
            }
            isSuggesting = false
        }
    }

    val monto = montoText.toDoubleOrNull()
    val isTransferencia = selectedType == TipoTransaccion.TRANSFERENCIA
    // El backend exige categoría en los gastos (sin ella responde
    // DATOS_INVALIDOS) y la rechaza en ingresos y transferencias, así que
    // solo se pide, y se manda, para los gastos.
    val isGasto = selectedType == TipoTransaccion.GASTO
    val isValid = selectedAccountId != null &&
        monto != null && monto > 0 &&
        (!isTransferencia || (selectedDestinationAccountId != null && selectedDestinationAccountId != selectedAccountId)) &&
        (!isGasto || selectedCategoryId != null)

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = "Nueva transacción", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Tipo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoTransaccion.creatable.forEach { tipo ->
                        SelectableChip(
                            label = tipo.label,
                            selected = tipo == selectedType,
                            onClick = {
                                selectedType = tipo
                                if (tipo != TipoTransaccion.TRANSFERENCIA) selectedDestinationAccountId = null
                                if (tipo != TipoTransaccion.GASTO) selectedCategoryId = null
                            }
                        )
                    }
                }

                Text(
                    text = "Cuenta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (accounts.isEmpty()) {
                    Text(
                        text = "Necesitas al menos una cuenta para registrar transacciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinTrackRed
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { account ->
                            SelectableChip(
                                label = account.nombre_cuenta,
                                selected = account.id_cuenta == selectedAccountId,
                                onClick = { selectedAccountId = account.id_cuenta }
                            )
                        }
                    }
                }

                if (isTransferencia) {
                    Text(
                        text = "Cuenta destino",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.filter { it.id_cuenta != selectedAccountId }.forEach { account ->
                            SelectableChip(
                                label = account.nombre_cuenta,
                                selected = account.id_cuenta == selectedDestinationAccountId,
                                onClick = { selectedDestinationAccountId = account.id_cuenta }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it; errorMessage = null },
                    label = { Text("Monto") },
                    placeholder = { Text("Ej. 150.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                if (isGasto) {
                    Text(
                        text = "Categoría",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    if (categories.isEmpty()) {
                        Text(
                            text = "Los gastos necesitan una categoría. Crea una en Mi Perfil > Categorías.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinTrackRed
                        )
                    } else if (selectedCategoryId == null) {
                        Text(
                            text = "Elige una categoría para el gasto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            SelectableChip(
                                label = category.name,
                                selected = category.id == selectedCategoryId,
                                onClick = { selectedCategoryId = category.id }
                            )
                        }
                    }
                }

                if (selectedCategoryId != null) {
                    Text(
                        text = "Subcategoría (opcional)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    if (isLoadingSubcategories) {
                        CircularProgressIndicator(color = FinTrackNavy, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else if (subcategories.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectableChip(
                                label = "Ninguna",
                                selected = selectedSubcategoryId == null,
                                onClick = { selectedSubcategoryId = null }
                            )
                            subcategories.forEach { subcategory ->
                                SelectableChip(
                                    label = subcategory.name,
                                    selected = subcategory.id == selectedSubcategoryId,
                                    onClick = { selectedSubcategoryId = subcategory.id }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it; suggestionMessage = null },
                    label = { Text("Comentario (opcional)") },
                    placeholder = { Text("Ej. Supermercado") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // El backend pide una descripción de 3 a 500 caracteres.
                if (isGasto && categories.isNotEmpty() && comentario.trim().length in 3..500) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sugerir categoría con el comentario",
                            color = FinTrackNavy,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable(enabled = !isSuggesting) { suggestCategory() }
                        )
                        if (isSuggesting) {
                            CircularProgressIndicator(
                                color = FinTrackNavy,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                suggestionMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    val accountId = selectedAccountId ?: return@Button
                    val montoValue = monto ?: return@Button
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val token = SessionManager.getAccessToken(context)
                                ?: throw IllegalStateException("No hay sesión activa")
                            val created = RetrofitClient.transaccionApi.crearTransaccion(
                                authorization = "Bearer $token",
                                request = CrearTransaccionRequest(
                                    tipo_transaccion = selectedType,
                                    cuenta_id = accountId,
                                    // El backend exige el monto como texto decimal, no como número.
                                    monto_transaccion = String.format(java.util.Locale.US, "%.2f", montoValue),
                                    cuenta_destino_id = if (isTransferencia) selectedDestinationAccountId else null,
                                    categoria_id = if (isGasto) selectedCategoryId else null,
                                    subcategoria_id = if (isGasto) selectedSubcategoryId else null,
                                    comentario_transaccion = comentario.trim().ifBlank { null }
                                )
                            )
                            onCreated(created.toAppTransaction())
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
