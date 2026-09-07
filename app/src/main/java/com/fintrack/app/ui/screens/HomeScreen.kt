package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.app.data.FakeData
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.LabeledProgressBar
import com.fintrack.app.ui.components.QuickAccessCard
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy

@Composable
fun HomeScreen(
    onNavigateToAccounts: () -> Unit,
    onAddTransaction: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackTopBar(title = "FinTrack", onMenuClick = onOpenProfile) }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                GreetingCard()

                BalanceCard(modifier = Modifier.padding(top = 16.dp))

                SectionCard(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Resumen Mensual",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LabeledProgressBar(
                        label = "Ingresos",
                        valueText = formatCurrency(FakeData.MONTHLY_INCOME),
                        progress = 1f,
                        progressColor = FinTrackGreen,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    LabeledProgressBar(
                        label = "Gastos",
                        valueText = formatCurrency(FakeData.MONTHLY_EXPENSES),
                        progress = (FakeData.MONTHLY_EXPENSES / FakeData.MONTHLY_INCOME).toFloat(),
                        progressColor = FinTrackNavy,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Text(
                    text = "Accesos Rápidos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessCard(
                        label = "Ver Cuentas",
                        icon = Icons.Filled.AccountBalanceWallet,
                        iconTint = FinTrackNavy,
                        iconBackground = FinTrackNavy.copy(alpha = 0.1f),
                        onClick = onNavigateToAccounts
                    )
                    QuickAccessCard(
                        label = "Transacción",
                        icon = Icons.Filled.Add,
                        iconTint = FinTrackGreen,
                        iconBackground = FinTrackGreen.copy(alpha = 0.15f),
                        onClick = onAddTransaction
                    )
                }
            }
        }

        item {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun GreetingCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(FinTrackNavy.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "CM", color = FinTrackNavy, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = "¡Hola de nuevo!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = FakeData.USER_NAME, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun BalanceCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FinTrackNavy)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Balance Total Disponible",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatCurrency(FakeData.TOTAL_BALANCE),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = FinTrackGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = " +${FakeData.BALANCE_CHANGE_PERCENT}% este mes",
                    color = FinTrackGreen,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
