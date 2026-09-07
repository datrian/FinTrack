package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.fintrack.app.ui.components.FinTrackDetailTopBar
import com.fintrack.app.ui.components.NavigationRowItem
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val profile = FakeData.userProfile
    var notificationsEnabled by remember { mutableStateOf(profile.budgetNotificationsEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackDetailTopBar(title = "Mi Perfil", onBackClick = onBack) }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(FinTrackNavy.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "CM", color = FinTrackNavy, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = profile.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "PREFERENCIAS GENERALES",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                SectionCard {
                    NavigationRowItem(title = "Moneda principal", trailingText = profile.currency, onClick = {})
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Notificaciones de presupuesto", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = FinTrackGreen)
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    NavigationRowItem(title = "Administrar Categorías", onClick = onNavigateToCategories)
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    NavigationRowItem(title = "Administrar Subcategorías", onClick = onNavigateToCategories)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                        .border(1.dp, FinTrackRed, RoundedCornerShape(16.dp))
                        .clickable { onLogout() }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(text = "Cerrar sesión", color = FinTrackRed, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
