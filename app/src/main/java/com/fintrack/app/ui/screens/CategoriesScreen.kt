package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fintrack.app.data.FakeData
import com.fintrack.app.data.model.SpendingCategory
import com.fintrack.app.ui.components.FinTrackDetailTopBar
import com.fintrack.app.ui.components.NavigationRowItem
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.theme.FinTrackNavy

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    var categories by remember { mutableStateOf(FakeData.categories) }
    var isAddingCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    fun commitNewCategory() {
        if (newCategoryName.isNotBlank()) {
            categories = categories + SpendingCategory(
                id = "cat-${System.currentTimeMillis()}",
                name = newCategoryName.trim(),
                subcategoryCount = 0,
                icon = Icons.Filled.Category,
                iconBackground = FinTrackNavy.copy(alpha = 0.1f),
                iconTint = FinTrackNavy
            )
        }
        newCategoryName = ""
        isAddingCategory = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackDetailTopBar(title = "Categorías", onBackClick = onBack) }

        item {
            Text(
                text = "Administra las categorías de tus movimientos principales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        item {
            SectionCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                categories.forEachIndexed { index, category ->
                    CategoryRow(category = category, onClick = { onCategoryClick(category.id) })
                    if (index != categories.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        item {
            if (isAddingCategory) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("Nueva categoría...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(
                        onClick = { commitNewCategory() },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(FinTrackNavy, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Agregar", tint = androidx.compose.ui.graphics.Color.White)
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
                    Text(text = "  Crear nueva categoría", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: SpendingCategory, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 14.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(category.iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = category.icon, contentDescription = null, tint = category.iconTint)
        }
        NavigationRowItem(
            title = category.name,
            subtitle = "${category.subcategoryCount} subcategorías",
            onClick = onClick,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
