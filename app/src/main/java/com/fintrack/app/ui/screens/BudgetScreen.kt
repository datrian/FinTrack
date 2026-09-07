package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.fintrack.app.ui.theme.FinTrackNavy

@Composable
fun BudgetScreen(onOpenProfile: () -> Unit = {}) {
    var budgetCategories by remember { mutableStateOf(FakeData.budgetCategories) }
    var isAddingCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryLimit by remember { mutableStateOf("") }

    fun commitNewBudgetCategory() {
        val limit = newCategoryLimit.toDoubleOrNull()
        if (newCategoryName.isNotBlank() && limit != null && limit > 0) {
            budgetCategories = budgetCategories + BudgetCategoryLimit(
                name = newCategoryName.trim(),
                spent = 0.0,
                limit = limit,
                barColor = FinTrackNavy
            )
        }
        newCategoryName = ""
        newCategoryLimit = ""
        isAddingCategory = false
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
            if (isAddingCategory) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("Nombre de la categoría...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newCategoryLimit,
                        onValueChange = { newCategoryLimit = it },
                        placeholder = { Text("Límite mensual...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                newCategoryName = ""
                                newCategoryLimit = ""
                                isAddingCategory = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "Cancelar")
                        }
                        Button(
                            onClick = { commitNewBudgetCategory() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FinTrackNavy)
                        ) {
                            Text(text = "Agregar")
                        }
                    }
                }
            } else {
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
