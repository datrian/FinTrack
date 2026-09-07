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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrack.app.data.FakeData
import com.fintrack.app.data.model.FuturePrediction
import com.fintrack.app.data.model.MonthProjection
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.ProgressTrack

@Composable
fun PredictionScreen(onOpenProfile: () -> Unit = {}) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackTopBar(title = "Predicción", onMenuClick = onOpenProfile) }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                TrendAnalysisCard()

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    SectionCard {
                        Text(
                            text = "Proyección a 3 Meses",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                        ProjectionBarChart(FakeData.monthProjections)
                    }
                }
            }
        }

        items(FakeData.futurePredictions) { prediction ->
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                PredictionRow(prediction)
            }
        }

        item { androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp)) }
    }
}

@Composable
private fun TrendAnalysisCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = FinTrackGreen.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = FinTrackGreen)
                Text(
                    text = "Análisis de Tendencia Inteligente",
                    style = MaterialTheme.typography.titleMedium,
                    color = FinTrackGreen,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = FakeData.TREND_ANALYSIS_TEXT,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ProjectionBarChart(projections: List<MonthProjection>, modifier: Modifier = Modifier) {
    val maxAmount = projections.maxOf { it.amount }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        projections.forEach { projection ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = projection.displayValue,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        projection.isProjected -> FinTrackGreen
                        projection.isSelected -> FinTrackNavy
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(36.dp)
                        .height((110 * (projection.amount / maxAmount)).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                projection.isProjected -> FinTrackGreen
                                projection.isSelected -> FinTrackNavy
                                else -> ProgressTrack
                            }
                        )
                ) {}
                Text(
                    text = projection.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (projection.isSelected || projection.isProjected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        projection.isProjected -> FinTrackGreen
                        projection.isSelected -> FinTrackNavy
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PredictionRow(prediction: FuturePrediction, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = prediction.month, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = prediction.confidence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = formatCurrency(prediction.amount),
                style = MaterialTheme.typography.titleLarge,
                color = FinTrackNavy
            )
        }
    }
}
