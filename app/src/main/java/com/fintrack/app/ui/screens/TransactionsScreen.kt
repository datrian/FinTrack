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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import com.fintrack.app.data.FakeData
import com.fintrack.app.data.local.SessionManager
import com.fintrack.app.data.model.Transaction
import com.fintrack.app.data.model.TransactionCategory
import com.fintrack.app.data.model.TransactionDirection
import com.fintrack.app.data.remote.CategoriaResponse
import com.fintrack.app.data.remote.CrearCategoriaRequest
import com.fintrack.app.data.remote.RetrofitClient
import com.fintrack.app.data.remote.parseApiErrorMessage
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.amountColor
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed
import java.io.IOException
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Los 3 filtros disponibles sobre la lista de movimientos.
private enum class TransactionFilter(val label: String) { TODAS("Todas"), GASTOS("Gastos"), INGRESOS("Ingresos") }

// Convierte lo que devuelve el backend al modelo que usa la UI.
private fun CategoriaResponse.toTransactionCategory() = TransactionCategory(
    id = id_categoria,
    name = nombre_categoria
)

// Pantalla de Transacciones: lista de movimientos con filtro por tipo
// (todas/gastos/ingresos) y las categorías reales del usuario, traídas de
// GET /api/v1/categorias, con un botón para crear nuevas (POST).
@Composable
fun TransactionsScreen(onOpenProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Filtro actualmente seleccionado; al cambiar, la lista se recalcula sola.
    var selectedFilter by remember { mutableStateOf(TransactionFilter.TODAS) }

    var categories by remember { mutableStateOf<List<TransactionCategory>>(emptyList()) }
    var isLoadingCategories by remember { mutableStateOf(true) }
    var categoriesError by remember { mutableStateOf<String?>(null) }
    var isAddingCategory by remember { mutableStateOf(false) }

    val loadCategories: () -> Unit = {
        scope.launch {
            isLoadingCategories = true
            categoriesError = null
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                val response = RetrofitClient.categoriaApi.obtenerCategorias("Bearer $token")
                categories = response.categorias.map { it.toTransactionCategory() }
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

    if (isAddingCategory) {
        AddCategoryDialog(
            onDismiss = { isAddingCategory = false },
            // Se ejecuta cuando el POST al backend tuvo éxito.
            onCreated = { newCategory ->
                categories = categories + newCategory
                isAddingCategory = false
            }
        )
    }

    // Se recalcula en cada recomposición según el filtro elegido (no se guarda
    // una copia filtrada por separado, se deriva directo de los datos).
    val filtered = when (selectedFilter) {
        TransactionFilter.TODAS -> FakeData.transactions
        TransactionFilter.GASTOS -> FakeData.transactions.filter { it.direction == TransactionDirection.GASTO }
        TransactionFilter.INGRESOS -> FakeData.transactions.filter { it.direction == TransactionDirection.INGRESO }
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

        // Sección de categorías: las del usuario (backend) + botón para crear una nueva.
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "CATEGORÍAS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                when {
                    isLoadingCategories -> {
                        CircularProgressIndicator(
                            color = FinTrackNavy,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .size(24.dp)
                        )
                    }
                    categoriesError != null -> {
                        Text(
                            text = categoriesError.orEmpty(),
                            color = FinTrackRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Reintentar",
                            color = FinTrackNavy,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable { loadCategories() }
                        )
                    }
                    categories.isEmpty() -> {
                        Text(
                            text = "Todavía no tienes categorías.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                CategoryChip(name = category.name)
                            }
                        }
                    }
                }

                AddCategoryButton(
                    onClick = { isAddingCategory = true },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        items(filtered) { transaction ->
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                TransactionRow(transaction)
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

// Pastilla con el nombre de una categoría del usuario.
@Composable
private fun CategoryChip(name: String, modifier: Modifier = Modifier) {
    Surface(
        color = FinTrackNavy.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = name,
            color = FinTrackNavy,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// Botón con borde ("Agregar categoría"), mismo estilo que el de nueva cuenta.
@Composable
private fun AddCategoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, FinTrackNavy, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = FinTrackNavy)
        Text(
            text = "Agregar categoría",
            color = FinTrackNavy,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// Popup para crear una categoría nueva. Manda el POST al backend él mismo y,
// si tuvo éxito, le pasa la categoría ya creada a TransactionsScreen.
@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onCreated: (TransactionCategory) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // El backend exige entre 2 y 100 caracteres.
    val isValid = name.trim().length in 2..100

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = "Nueva categoría", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Las categorías te sirven para clasificar tus transacciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nombre de la categoría") },
                    placeholder = { Text("Ej. Alimentos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val token = SessionManager.getAccessToken(context)
                                ?: throw IllegalStateException("No hay sesión activa")
                            val created = RetrofitClient.categoriaApi.crearCategoria(
                                authorization = "Bearer $token",
                                request = CrearCategoriaRequest(nombre_categoria = name.trim())
                            )
                            onCreated(created.toTransactionCategory())
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

// Una fila de la lista: ícono, título, categoría/fecha, y el monto (verde si
// es un ingreso, rojo si es un gasto, gracias a amountColor).
@Composable
private fun TransactionRow(transaction: Transaction, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(transaction.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = transaction.icon, contentDescription = null, tint = FinTrackNavy)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = transaction.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${transaction.category} • ${transaction.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = (if (transaction.amount >= 0) "+" else "") + formatCurrency(transaction.amount),
            color = amountColor(transaction.amount >= 0),
            fontWeight = FontWeight.Bold
        )
    }
}
