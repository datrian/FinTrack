package com.fintrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed
import com.fintrack.app.ui.theme.ProgressTrack
import java.text.NumberFormat
import java.util.Locale

// Piezas de UI reutilizadas por varias pantallas (barras superiores, tarjetas,
// barras de progreso, etc), para no repetir el mismo código en cada archivo
// de pantalla.

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)

// Formatea un número como moneda (ej. 1234.5 -> "$1,234.50").
fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

/** Top app bar used on every top-level screen: hamburger menu, screen title and logo. */
@Composable
fun FinTrackTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Menú",
                tint = FinTrackNavy,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable { onMenuClick?.invoke() }
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = FinTrackNavy
            )
        }
        FinTrackLogo()
    }
}

/** Simple back-navigation top bar used on secondary screens (Categorías, Subcategorías). */
@Composable
fun FinTrackDetailTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = FinTrackNavy,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable { onBackClick() }
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = FinTrackNavy
            )
        }
        FinTrackLogo()
    }
}

// Logo circular ("$") que aparece en la esquina de las barras superiores.
@Composable
fun FinTrackLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(FinTrackNavy),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$",
            color = FinTrackGreen,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

// Tarjeta blanca con bordes redondeados y padding interno: es el "bloque"
// visual base que se usa para casi todo el contenido de las pantallas
// (tarjeta de cuenta, tarjeta de presupuesto, etc).
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScopeAlias.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

// Alias to avoid importing ColumnScope explicitly at every call site.
typealias ColumnScopeAlias = androidx.compose.foundation.layout.ColumnScope

// Barra de progreso con una etiqueta a la izquierda (ej. "Alimentos") y un
// valor a la derecha (ej. "82%"), usada en presupuestos.
@Composable
fun LabeledProgressBar(
    label: String,
    valueText: String,
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = valueText, style = MaterialTheme.typography.titleMedium, color = progressColor)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = ProgressTrack
        )
    }
}

// Tarjeta cuadrada de acceso rápido (ícono + texto) que se usa en la grilla
// de "accesos rápidos" de la pantalla de Inicio.
@Composable
fun RowScope.QuickAccessCard(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1.4f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = iconTint)
            }
            Text(
                text = label,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Fila de lista genérica: título + subtítulo opcional a la izquierda, texto
// final opcional y flechita ">" a la derecha (si tiene onClick). Se usa para
// filas de listas navegables, como en Perfil o Categorías.
@Composable
fun NavigationRowItem(
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Color estándar para montos: verde si es positivo (ingreso), rojo si es negativo (gasto).
fun amountColor(isPositive: Boolean): Color = if (isPositive) FinTrackGreen else FinTrackRed
