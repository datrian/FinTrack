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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fintrack.app.data.FakeData
import com.fintrack.app.data.model.Account
import com.fintrack.app.data.model.AccountType
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.components.amountColor
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackOrange
import com.fintrack.app.ui.theme.FinTrackRed

// Pantalla principal de "Mis Cuentas". Compose la vuelve a dibujar automáticamente
// cada vez que cambia alguno de los "state" (accounts, isAddingAccount) que lee.
@Composable
fun AccountsScreen(onAddAccount: () -> Unit = {}, onOpenProfile: () -> Unit = {}) {
    // Lista de cuentas mostrada en pantalla. Arranca con los datos de ejemplo
    // (FakeData) y "remember" hace que Compose la recuerde entre recomposiciones
    // (si no la guardáramos así, se perdería cada vez que la UI se redibuja).
    var accounts by remember { mutableStateOf(FakeData.accounts) }

    // Bandera que controla si el popup de "nueva cuenta" está visible o no.
    var isAddingAccount by remember { mutableStateOf(false) }

    // Mientras la bandera esté en true, se muestra el diálogo emergente.
    if (isAddingAccount) {
        AddAccountDialog(
            // Se ejecuta al cancelar o tocar fuera del popup: solo lo cierra.
            onDismiss = { isAddingAccount = false },
            // Se ejecuta al presionar "Agregar" con datos válidos.
            onConfirm = { name, subtitle, type, balance ->
                // Agrega la nueva cuenta a la lista existente (no la modifica,
                // crea una lista nueva con "+", que es como se maneja el estado
                // inmutable en Compose).
                accounts = accounts + Account(
                    id = "acc-${System.currentTimeMillis()}", // id simple basado en la hora actual
                    name = name,
                    subtitle = subtitle,
                    type = type,
                    balance = balance
                )
                isAddingAccount = false // cierra el popup tras guardar
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

        // Una tarjeta (AccountCard) por cada cuenta en la lista.
        items(accounts) { account ->
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                AccountCard(account)
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

// Tarjeta visual de una sola cuenta: nombre, descripción, etiqueta de tipo y saldo.
@Composable
private fun AccountCard(account: Account, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) { // SectionCard da el fondo blanco + bordes redondeados reutilizables
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween // nombre a la izquierda, etiqueta de tipo a la derecha
        ) {
            Column {
                Text(text = account.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = account.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// Etiqueta chica y coloreada (Principal / Inversión / Crédito / Efectivo) que se
// muestra dentro de cada AccountCard. Cada tipo tiene su propio color de fondo/texto.
@Composable
private fun AccountTypeTag(type: AccountType, modifier: Modifier = Modifier) {
    val (background, textColor) = when (type) {
        AccountType.PRINCIPAL -> FinTrackNavy.copy(alpha = 0.1f) to FinTrackNavy
        AccountType.INVERSION -> FinTrackGreen.copy(alpha = 0.15f) to FinTrackGreen
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

// Popup (AlertDialog) para crear una cuenta nueva. No guarda nada por sí mismo:
// solo junta los datos que escribe el usuario y, al confirmar, se los pasa al
// callback "onConfirm" para que quien lo llamó (AccountsScreen) decida qué hacer.
@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, subtitle: String, type: AccountType, balance: Double) -> Unit
) {
    // Cada campo del formulario es su propio "state": cuando el usuario escribe,
    // Compose vuelve a dibujar solo lo necesario para reflejar el cambio.
    var name by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") } // texto crudo del campo de saldo
    var selectedType by remember { mutableStateOf(AccountType.PRINCIPAL) } // chip elegido

    // Convierte el texto del saldo a número; queda null si no es un número válido.
    val balance = balanceText.toDoubleOrNull()
    // El botón "Agregar" solo se habilita si nombre y descripción no están vacíos
    // y el saldo es un número válido.
    val isValid = name.isNotBlank() && subtitle.isNotBlank() && balance != null

    AlertDialog(
        onDismissRequest = onDismiss, // se llama al tocar fuera del popup o el botón atrás
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
                    onValueChange = { name = it },
                    label = { Text("Nombre de la cuenta") },
                    placeholder = { Text("Ej. BBVA Nómina") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                // Campo: descripción/subtítulo de la cuenta.
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Ej. Cuenta Corriente") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                // Campo: saldo inicial. keyboardType = Decimal muestra el teclado numérico.
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
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
            }
        },
        confirmButton = {
            // Deshabilitado (enabled = isValid) hasta que el formulario sea válido.
            Button(
                onClick = { onConfirm(name.trim(), subtitle.trim(), selectedType, balance ?: 0.0) },
                enabled = isValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackNavy)
            ) {
                Text(text = "Agregar")
            }
        },
        dismissButton = {
            // Cierra el popup sin guardar nada.
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}
