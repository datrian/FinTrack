package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.app.data.FakeData
import com.fintrack.app.data.model.BudgetCategoryLimit
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.LabeledProgressBar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackOrange
import com.fintrack.app.ui.theme.FinTrackPurple
import com.fintrack.app.ui.theme.FinTrackRed
import com.fintrack.app.ui.theme.FinTrackTeal

private val budgetCategoryColors = listOf(
    FinTrackNavy,
    FinTrackGreen,
    FinTrackRed,
    FinTrackOrange,
    FinTrackPurple,
    FinTrackTeal
)

@Composable
fun BudgetScreen(onOpenProfile: () -> Unit = {}) {
    var budgetCategories by remember { mutableStateOf(FakeData.budgetCategories) }
    var isAddingCategory by remember { mutableStateOf(false) }

    if (isAddingCategory) {
        AddBudgetCategoryDialog(
            onDismiss = { isAddingCategory = false },
            onConfirm = { name, limit, color ->
                budgetCategories = budgetCategories + BudgetCategoryLimit(
                    name = name,
                    spent = 0.0,
                    limit = limit,
                    barColor = color
                )
                isAddingCategory = false
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
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionCard {
                    Text(
                        text = "Gasto Total Presupuestado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.Bottom,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = formatCurrency(FakeData.BUDGET_TOTAL_SPENT),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "  de ${formatCurrency(FakeData.BUDGET_TOTAL_LIMIT)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { (FakeData.BUDGET_TOTAL_SPENT / FakeData.BUDGET_TOTAL_LIMIT).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                        color = FinTrackNavy,
                        trackColor = com.fintrack.app.ui.theme.ProgressTrack
                    )
                }

                Text(
                    text = "Límites por Categoría",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
            }
        }

        items(budgetCategories) { category ->
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                BudgetCategoryCard(category)
            }
        }

        item {
            Button(
                onClick = { isAddingCategory = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackNavy)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Text(text = "  Agregar categoría de presupuesto", modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        item { androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp)) }
    }
}

@Composable
private fun BudgetCategoryCard(category: BudgetCategoryLimit, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        LabeledProgressBar(
            label = category.name,
            valueText = "${category.percentage}%",
            progress = (category.spent / category.limit).toFloat(),
            progressColor = category.barColor
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(
                text = "Gastado: ${formatCurrency(category.spent)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Límite: ${formatCurrency(category.limit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddBudgetCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, limit: Double, color: Color) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(budgetCategoryColors.first()) }

    val limit = limitText.toDoubleOrNull()
    val isValid = name.isNotBlank() && limit != null && limit > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Nueva categoría de presupuesto", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Ingresa el nombre y el límite mensual que quieres asignar a esta categoría.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la categoría") },
                    placeholder = { Text("Ej. Entretenimiento") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Límite mensual") },
                    placeholder = { Text("Ej. 200") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    budgetCategoryColors.forEach { color ->
                        val isSelected = color == selectedColor
                        Column(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), limit ?: 0.0, selectedColor) },
                enabled = isValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackNavy)
            ) {
                Text(text = "Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}
