package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrack.app.data.FakeData
import com.fintrack.app.data.model.Subcategory
import com.fintrack.app.ui.components.FinTrackDetailTopBar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed

@Composable
fun SubcategoriesScreen(categoryId: String, onBack: () -> Unit) {
    val category = FakeData.categories.firstOrNull { it.id == categoryId } ?: FakeData.categories.first()
    val subcategories = remember(categoryId) {
        mutableStateOf(FakeData.subcategoriesByCategory[categoryId] ?: emptyList())
    }
    var newSubcategoryName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackDetailTopBar(title = "Subcategorías", onBackClick = onBack) }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                color = FinTrackNavy,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = category.icon, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Categoría Seleccionada",
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = category.name,
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Lista de subcategorías:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        items(subcategories.value, key = { it.id }) { subcategory ->
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                SubcategoryRow(
                    subcategory = subcategory,
                    onDelete = {
                        subcategories.value = subcategories.value.filterNot { it.id == subcategory.id }
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubcategoryName,
                    onValueChange = { newSubcategoryName = it },
                    placeholder = { Text("Nueva subcategoría...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                IconButton(
                    onClick = {
                        if (newSubcategoryName.isNotBlank()) {
                            subcategories.value = subcategories.value + Subcategory(
                                id = "sub-${System.currentTimeMillis()}",
                                name = newSubcategoryName,
                                limitLabel = "Sin límite"
                            )
                            newSubcategoryName = ""
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(FinTrackNavy, CircleShape)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Agregar", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }

        item { androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp)) }
    }
}

@Composable
private fun SubcategoryRow(subcategory: Subcategory, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = subcategory.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subcategory.limitLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Eliminar", tint = FinTrackRed)
            }
        }
    }
}
