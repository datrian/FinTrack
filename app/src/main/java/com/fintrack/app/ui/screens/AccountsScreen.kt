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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fintrack.app.data.local.SessionManager
import com.fintrack.app.data.model.Account
import com.fintrack.app.data.model.AccountType
import com.fintrack.app.data.remote.CrearCuentaRequest
import com.fintrack.app.data.remote.CuentaListaItem
import com.fintrack.app.data.remote.CuentaResponse
import com.fintrack.app.data.remote.RetrofitClient
import com.fintrack.app.data.remote.parseApiErrorMessage
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackOrange
import com.fintrack.app.ui.theme.FinTrackRed
import java.io.IOException
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Convierte lo que devuelve el backend (GET, versión liviana) al modelo que usa la UI.
private fun CuentaListaItem.toAccount() = Account(
    id = id_cuenta,
    name = nombre_cuenta,
    institution = institucion_cuenta,
    type = tipo_cuenta,
    balance = saldo_actual_cuenta.toDoubleOrNull() ?: 0.0
)

// Convierte lo que devuelve el backend (POST, versión completa) al modelo que usa la UI.
private fun CuentaResponse.toAccount() = Account(
    id = id_cuenta,
    name = nombre_cuenta,
    institution = institucion_cuenta,
    type = tipo_cuenta,
    balance = saldo_actual_cuenta.toDoubleOrNull() ?: 0.0
)

// Pantalla principal de "Mis Cuentas": trae la lista real del backend
// (GET /api/v1/cuentas) y permite crear cuentas nuevas (POST /api/v1/cuentas).
@Composable
fun AccountsScreen(onAddAccount: () -> Unit = {}, onOpenProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val loadAccounts: () -> Unit = {
        scope.launch {
            isLoading = true
            loadError = null
            try {
                val token = SessionManager.getAccessToken(context)
                    ?: throw IllegalStateException("No hay sesión activa")
                val response = RetrofitClient.cuentaApi.obtenerCuentas("Bearer $token")
                accounts = response.cuentas.map { it.toAccount() }
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

    LaunchedEffect(Unit) { loadAccounts() }

    // Bandera que controla si el popup de "nueva cuenta" está visible o no.
    var isAddingAccount by remember { mutableStateOf(false) }

    // Mientras la bandera esté en true, se muestra el diálogo emergente.
    if (isAddingAccount) {
        AddAccountDialog(
            // Se ejecuta al cancelar o tocar fuera del popup: solo lo cierra.
            onDismiss = { isAddingAccount = false },
            // Se ejecuta cuando el POST al backend tuvo éxito.
            onCreated = { newAccount ->
                accounts = accounts + newAccount
                isAddingAccount = false
            }
        )
    }

    // Lista con scroll vertical que contiene toda la pantalla.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Barra superior con el título de la pantalla.
        item { FinTrackTopBar(title = "Mis Cuentas", onMenuClick = onOpenProfile) }

        if (isLoading && accounts.isEmpty()) {
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
        } else if (loadError != null && accounts.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = loadError.orEmpty(),
                        color = FinTrackRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, FinTrackNavy, RoundedCornerShape(16.dp))
                            .clickable { loadAccounts() }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Reintentar", color = FinTrackNavy, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            // Una tarjeta (AccountCard) por cada cuenta en la lista.
            items(accounts) { account ->
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    AccountCard(account)
                }
            }
        }

        // Botón al final de la lista para abrir el popup de nueva cuenta.
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                AddAccountButton(onClick = {
                    isAddingAccount = true // abre el popup
                    onAddAccount() // avisa también a quien use esta pantalla (navegación, etc.)
                })
            }
        }
    }
}

// Tarjeta visual de una sola cuenta: nombre, institución, etiqueta de tipo y saldo.
@Composable
private fun AccountCard(account: Account, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) { // SectionCard da el fondo blanco + bordes redondeados reutilizables
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween // nombre a la izquierda, etiqueta de tipo a la derecha
        ) {
            Column {
                Text(text = account.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (!account.institution.isNullOrBlank()) {
                    Text(text = account.institution, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AccountTypeTag(account.type)
        }
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Saldo disponible", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = formatCurrency(account.balance),
                style = MaterialTheme.typography.headlineSmall,
                // si el saldo es negativo se pinta en rojo para que resalte
                color = if (account.balance < 0) FinTrackRed else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Etiqueta chica y coloreada (Efectivo / Débito / Ahorro / Crédito) que se
// muestra dentro de cada AccountCard. Cada tipo tiene su propio color de fondo/texto.
@Composable
private fun AccountTypeTag(type: AccountType, modifier: Modifier = Modifier) {
    val (background, textColor) = when (type) {
        AccountType.DEBITO -> FinTrackNavy.copy(alpha = 0.1f) to FinTrackNavy
        AccountType.AHORRO -> FinTrackGreen.copy(alpha = 0.15f) to FinTrackGreen
        AccountType.CREDITO -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f) to MaterialTheme.colorScheme.onSurfaceVariant
        AccountType.EFECTIVO -> FinTrackOrange.copy(alpha = 0.18f) to FinTrackOrange
    }
    Surface(
        color = background,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = type.label,
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// Botón con borde ("Agregar nueva cuenta") al final de la lista. Solo dibuja el
// botón; quien lo use decide qué pasa al presionarlo mediante "onClick".
@Composable
private fun AddAccountButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, FinTrackNavy, RoundedCornerShape(16.dp))
            .clickable { onClick() } // toda la fila es "clickeable", no solo el ícono o el texto
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = FinTrackNavy)
        Text(
            text = "Agregar nueva cuenta",
            color = FinTrackNavy,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// Popup (AlertDialog) para crear una cuenta nueva. Manda el POST al backend
// él mismo (igual que EditProfileDialog en Mi Perfil) y, si tuvo éxito, le
// pasa la cuenta ya creada a AccountsScreen mediante "onCreated".
@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onCreated: (Account) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Cada campo del formulario es su propio "state": cuando el usuario escribe,
    // Compose vuelve a dibujar solo lo necesario para reflejar el cambio.
    var name by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") } // texto crudo del campo de saldo
    var selectedType by remember { mutableStateOf(AccountType.DEBITO) } // chip elegido
    var creditLimitText by remember { mutableStateOf("") } // solo aplica si selectedType == CREDITO
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Convierte el texto del saldo a número; queda null si no es un número válido.
    val balance = balanceText.toDoubleOrNull()
    val creditLimit = creditLimitText.toDoubleOrNull()
    val isCredito = selectedType == AccountType.CREDITO
    // El botón "Agregar" solo se habilita si el nombre no está vacío, el
    // saldo es un número válido >= 0, y (solo para cuentas de Crédito) hay un
    // límite de crédito > 0: el backend rechaza "CREDITO" sin ese dato.
    val isValid = name.isNotBlank() && balance != null && balance >= 0 &&
        (!isCredito || (creditLimit != null && creditLimit > 0))

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() }, // se llama al tocar fuera del popup o el botón atrás
        title = { Text(text = "Nueva cuenta", fontWeight = FontWeight.Bold) },
        text = {
            // Cuerpo del popup: instrucciones + campos de texto + selector de tipo.
            Column {
                Text(
                    text = "Completa los datos de la cuenta que quieres agregar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Campo: nombre de la cuenta.
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nombre de la cuenta") },
                    placeholder = { Text("Ej. BBVA Nómina") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                // Campo: institución (opcional, el backend no la exige).
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Institución (opcional)") },
                    placeholder = { Text("Ej. BBVA") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                // Campo: saldo inicial. keyboardType = Decimal muestra el teclado numérico.
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it; errorMessage = null },
                    label = { Text("Saldo inicial") },
                    placeholder = { Text("Ej. 1000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "Tipo de cuenta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                // Fila de "chips": uno por cada valor del enum AccountType.
                // Al tocar uno, se marca como seleccionado (fondo navy + texto blanco).
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountType.entries.forEach { type ->
                        val isSelected = type == selectedType
                        Surface(
                            color = if (isSelected) FinTrackNavy else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { selectedType = type }
                        ) {
                            Text(
                                text = type.label,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                // Solo se muestra cuando el tipo elegido es "Crédito": el backend
                // requiere un límite de crédito mayor a 0 para ese tipo de cuenta.
                if (isCredito) {
                    OutlinedTextField(
                        value = creditLimitText,
                        onValueChange = { creditLimitText = it; errorMessage = null },
                        label = { Text("Límite de crédito") },
                        placeholder = { Text("Ej. 5000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(12.dp)
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
            // Deshabilitado (enabled = isValid) hasta que el formulario sea válido.
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val token = SessionManager.getAccessToken(context)
                                ?: throw IllegalStateException("No hay sesión activa")
                            val created = RetrofitClient.cuentaApi.crearCuenta(
                                authorization = "Bearer $token",
                                request = CrearCuentaRequest(
                                    nombre_cuenta = name.trim(),
                                    tipo_cuenta = selectedType,
                                    saldo_inicial_cuenta = balance ?: 0.0,
                                    institucion_cuenta = institution.trim().ifBlank { null },
                                    limite_credito_cuenta = if (isCredito) creditLimit else null
                                )
                            )
                            onCreated(created.toAccount())
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
            // Cierra el popup sin guardar nada.
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(text = "Cancelar")
            }
        }
    )
}
