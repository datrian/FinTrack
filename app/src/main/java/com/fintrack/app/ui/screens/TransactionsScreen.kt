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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.fintrack.app.data.remote.CambiarEstadoCategoriaRequest
import com.fintrack.app.data.remote.CategoriaResponse
import com.fintrack.app.data.remote.CrearCategoriaRequest
import com.fintrack.app.data.remote.RenombrarCategoriaRequest
import com.fintrack.app.data.remote.RetrofitClient
import com.fintrack.app.data.remote.parseApiErrorMessage
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.SectionCard
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

    // Categoría cuyo detalle se está trayendo (GET) para abrir el diálogo de
    // renombrar; se usa para mostrar un spinner solo en ese chip.
    var loadingCategoryId by remember { mutableStateOf<String?>(null) }
    // Detalle ya traído: mientras no sea null, se muestra el diálogo de renombrar.
    var categoryToRename by remember { mutableStateOf<CategoriaResponse?>(null) }

    val openRenameDialog: (String) -> Unit = { categoryId ->
        scope.launch {
            loadingCategoryId = categoryId
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                categoryToRename = RetrofitClient.categoriaApi.obtenerCategoria("Bearer $token", categoryId)
            } catch (e: HttpException) {
                val message = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                android.widget.Toast.makeText(context, "No se pudo conectar con el servidor. Revisa tu conexión.", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: IllegalStateException) {
                android.widget.Toast.makeText(context, "Tu sesión expiró. Vuelve a iniciar sesión.", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                loadingCategoryId = null
            }
        }
    }

    val categoryBeingRenamed = categoryToRename
    if (categoryBeingRenamed != null) {
        RenameCategoryDialog(
            category = categoryBeingRenamed,
            onDismiss = { categoryToRename = null },
            // Se ejecuta cuando el PATCH al backend tuvo éxito.
            onRenamed = { renamedCategory ->
                categories = categories.map { if (it.id == renamedCategory.id) renamedCategory else it }
                categoryToRename = null
            }
        )
    }

    // Categoría que se está por desactivar (pendiente de confirmación) y
    // categoría cuyo PATCH de estado está en curso (para el spinner de ese chip).
    var categoryPendingDeactivation by remember { mutableStateOf<TransactionCategory?>(null) }
    var deactivatingCategoryId by remember { mutableStateOf<String?>(null) }

    val deactivateCategory: (TransactionCategory) -> Unit = { category ->
        categoryPendingDeactivation = null
        scope.launch {
            deactivatingCategoryId = category.id
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                RetrofitClient.categoriaApi.cambiarEstadoCategoria(
                    authorization = "Bearer $token",
                    idCategoria = category.id,
                    request = CambiarEstadoCategoriaRequest(es_activa_categoria = false)
                )
                // GET /api/v1/categorias solo trae categorías activas por
                // defecto, así que una vez desactivada ya no pertenece a esta lista.
                categories = categories.filterNot { it.id == category.id }
            } catch (e: HttpException) {
                val message = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                android.widget.Toast.makeText(context, "No se pudo conectar con el servidor. Revisa tu conexión.", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: IllegalStateException) {
                android.widget.Toast.makeText(context, "Tu sesión expiró. Vuelve a iniciar sesión.", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                deactivatingCategoryId = null
            }
        }
    }

    val categoryToConfirmDeactivation = categoryPendingDeactivation
    if (categoryToConfirmDeactivation != null) {
        AlertDialog(
            onDismissRequest = { categoryPendingDeactivation = null },
            title = { Text("Desactivar categoría") },
            text = { Text("\"${categoryToConfirmDeactivation.name}\" se desactivará y dejará de aparecer en tu lista de categorías. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(onClick = { deactivateCategory(categoryToConfirmDeactivation) }) {
                    Text(text = "Desactivar", color = FinTrackRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryPendingDeactivation = null }) { Text("Cancelar") }
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
                        // Mismo contorno compartido (SectionCard + divisores) que usan
                        // las categorías de ejemplo en Administrar Categorías.
                        SectionCard {
                            categories.forEachIndexed { index, category ->
                                CategoryListRow(
                                    category = category,
                                    isLoading = loadingCategoryId == category.id,
                                    isDeactivating = deactivatingCategoryId == category.id,
                                    onEditClick = { openRenameDialog(category.id) },
                                    onDeactivateClick = { categoryPendingDeactivation = category }
                                )
                                if (index != categories.lastIndex) {
                                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                }
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

// Una fila de categoría real del usuario: ícono circular + nombre, y los
// botones de editar (lápiz, trae el GET y abre el diálogo de renombrar) y
// desactivar (bote de basura). Mismo diseño que las categorías de ejemplo en
// Administrar Categorías (CategoryRow de CategoriesScreen.kt): ícono circular
// a la izquierda, dentro de un SectionCard compartido con divisores.
@Composable
private fun CategoryListRow(
    category: TransactionCategory,
    isLoading: Boolean,
    isDeactivating: Boolean,
    onEditClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(FinTrackNavy.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Category, contentDescription = null, tint = FinTrackNavy)
        }
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        )
        IconButton(onClick = onEditClick, enabled = !isLoading && !isDeactivating) {
            if (isLoading) {
                CircularProgressIndicator(color = FinTrackNavy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Editar categoría", tint = FinTrackNavy)
            }
        }
        IconButton(onClick = onDeactivateClick, enabled = !isLoading && !isDeactivating) {
            if (isDeactivating) {
                CircularProgressIndicator(color = FinTrackRed, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Desactivar categoría", tint = FinTrackRed)
            }
        }
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

// Popup para renombrar una categoría existente, precargado con el nombre
// que trajo GET /api/v1/categorias/{id}. Manda el PATCH al backend él mismo
// y, si tuvo éxito, le pasa la categoría actualizada a TransactionsScreen.
@Composable
private fun RenameCategoryDialog(
    category: CategoriaResponse,
    onDismiss: () -> Unit,
    onRenamed: (TransactionCategory) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(category.nombre_categoria) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isValid = name.trim().length in 2..100

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = "Renombrar categoría", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nombre de la categoría") },
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
                            val renamed = RetrofitClient.categoriaApi.renombrarCategoria(
                                authorization = "Bearer $token",
                                idCategoria = category.id_categoria,
                                request = RenombrarCategoriaRequest(nombre_categoria = name.trim())
                            )
                            onRenamed(renamed.toTransactionCategory())
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
