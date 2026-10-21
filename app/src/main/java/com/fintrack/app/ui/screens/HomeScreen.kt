package com.fintrack.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.app.data.local.ProfilePhotoStore
import com.fintrack.app.data.local.SessionManager
import com.fintrack.app.data.local.UserProfileStore
import com.fintrack.app.data.model.TipoTransaccion
import com.fintrack.app.data.remote.RetrofitClient
import com.fintrack.app.data.remote.TransaccionResponse
import com.fintrack.app.data.remote.parseApiErrorMessage
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.ProfileAvatar
import com.fintrack.app.ui.components.QuickAccessCard
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.components.initialsOf
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed
import com.fintrack.app.ui.theme.ProgressTrack
import java.io.IOException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot
import retrofit2.HttpException

// Máximo que acepta el backend en "limite" para GET /api/v1/transacciones.
private const val TRANSACTIONS_PAGE_SIZE = 100

// Trae todas las transacciones del usuario, página por página (el backend
// devuelve como máximo 100 por petición).
private suspend fun fetchAllTransactions(token: String): List<TransaccionResponse> {
    val all = mutableListOf<TransaccionResponse>()
    while (true) {
        val page = RetrofitClient.transaccionApi.obtenerTransacciones(
            authorization = "Bearer $token",
            limite = TRANSACTIONS_PAGE_SIZE,
            offset = all.size
        )
        all += page.transacciones
        if (page.transacciones.isEmpty() || all.size >= page.total) return all
    }
}

// Mes (en la zona horaria del teléfono) en que se registró una transacción.
// El backend manda ISO 8601 con o sin zona; sin zona se toma tal cual.
internal fun TransaccionResponse.yearMonth(): YearMonth? {
    val date = runCatching {
        OffsetDateTime.parse(fecha_registro_transaccion).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
    }.recoverCatching { LocalDateTime.parse(fecha_registro_transaccion).toLocalDate() }
        .getOrNull() ?: return null
    return YearMonth.from(date)
}

private fun TransaccionResponse.amount(): Double = monto_transaccion.toDoubleOrNull() ?: 0.0

// Totales que muestra Inicio, calculados a partir de las transacciones.
// Las transferencias no cuentan: solo mueven dinero entre cuentas propias.
internal data class HomeTotals(
    val balance: Double,       // todos los ingresos - todos los gastos
    val monthIncome: Double,   // ingresos del mes actual
    val monthExpenses: Double  // gastos del mes actual
)

internal fun List<TransaccionResponse>.toHomeTotals(month: YearMonth): HomeTotals {
    var balance = 0.0
    var monthIncome = 0.0
    var monthExpenses = 0.0
    for (t in this) {
        val inMonth = t.yearMonth() == month
        when (t.tipo_transaccion) {
            TipoTransaccion.INGRESO -> {
                balance += t.amount()
                if (inMonth) monthIncome += t.amount()
            }
            TipoTransaccion.GASTO -> {
                balance -= t.amount()
                if (inMonth) monthExpenses += t.amount()
            }
            TipoTransaccion.TRANSFERENCIA -> Unit
        }
    }
    return HomeTotals(balance, monthIncome, monthExpenses)
}

// Pantalla de Inicio: saludo, balance total, resumen mensual y accesos rápidos.
// Es la primera pantalla que ve el usuario al abrir la app.
@Composable
fun HomeScreen(
    onNavigateToAccounts: () -> Unit,
    onAddTransaction: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentMonth = remember { YearMonth.now() }
    // null mientras carga o si la petición falló.
    var totals by remember { mutableStateOf<HomeTotals?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Una sola carga de transacciones alimenta el balance y el resumen mensual.
    LaunchedEffect(Unit) {
        try {
            val token = SessionManager.getAccessToken(context)
                ?: throw IllegalStateException("No hay sesión activa")
            totals = fetchAllTransactions(token).toHomeTotals(currentMonth)
        } catch (e: HttpException) {
            loadError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
        } catch (e: IOException) {
            loadError = "No se pudo conectar con el servidor. Revisa tu conexión."
        } catch (e: IllegalStateException) {
            loadError = "Tu sesión expiró. Vuelve a iniciar sesión."
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackTopBar(title = "FinTrack", onMenuClick = onOpenProfile) }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                GreetingCard()

                BalanceCard(
                    balance = totals?.balance,
                    loadError = loadError,
                    modifier = Modifier.padding(top = 16.dp)
                )

                MonthlySummaryCard(
                    month = currentMonth,
                    totals = totals,
                    loadError = loadError,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "Accesos Rápidos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
                // Dos atajos: ir a Cuentas o ir a Transacciones (para agregar una).
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

// Tarjeta con la foto (o iniciales) del usuario y un saludo ("¡Hola de nuevo!").
// Tocar la foto abre el selector de imágenes del sistema para elegir una nueva.
@Composable
private fun GreetingCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val profile by UserProfileStore.profile

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val userId = profile?.id
        if (uri != null && userId != null) {
            ProfilePhotoStore.savePhoto(context, userId, uri)
        }
    }
    // Si Mi Perfil ya lo cargó antes, se reutiliza; si no, se trae aquí.
    LaunchedEffect(Unit) {
        if (profile == null) {
            runCatching { UserProfileStore.refresh(context) }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                initials = profile?.name?.let { initialsOf(it) } ?: "",
                userId = profile?.id,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(enabled = profile != null) {
                        pickPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = "¡Hola de nuevo!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = profile?.name ?: "Usuario", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// Tarjeta azul grande con el balance total: la suma de todos los ingresos
// menos la suma de todos los gastos registrados en transacciones.
@Composable
private fun BalanceCard(balance: Double?, loadError: String?, modifier: Modifier = Modifier) {
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
            when {
                balance != null -> Text(
                    text = formatCurrency(balance),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                loadError != null -> Text(
                    text = loadError,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                else -> CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(32.dp)
                )
            }
        }
    }
}

// Una rebanada de la gráfica de pastel del resumen mensual.
private data class PieSlice(val label: String, val value: Double, val color: Color)

// Resumen del mes actual como gráfica de pastel: cuánto entró (ingresos) y
// cuánto salió (gastos). Tocar una rebanada muestra su monto en el centro.
@Composable
private fun MonthlySummaryCard(
    month: YearMonth,
    totals: HomeTotals?,
    loadError: String?,
    modifier: Modifier = Modifier
) {
    val monthName = month.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        .replaceFirstChar { it.uppercase() }

    SectionCard(modifier = modifier) {
        Text(
            text = "Resumen Mensual",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Ingresos y gastos de $monthName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            totals != null -> {
                val slices = listOf(
                    PieSlice("Ingresos", totals.monthIncome, FinTrackGreen),
                    PieSlice("Gastos", totals.monthExpenses, FinTrackRed)
                )
                MonthlyPieChart(
                    slices = slices,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp)
                )
                PieLegend(slices = slices, modifier = Modifier.padding(top = 16.dp))
            }
            loadError != null -> Text(
                text = loadError,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            else -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FinTrackNavy)
            }
        }
    }
}

// Gráfica de pastel en forma de dona. Sin movimientos en el mes se dibuja el
// anillo vacío en gris. El centro muestra el neto del mes, o el monto de la
// rebanada que se tocó (tocarla de nuevo, o tocar fuera, vuelve al neto).
@Composable
private fun MonthlyPieChart(slices: List<PieSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }
    var selectedIndex by remember(slices) { mutableStateOf<Int?>(null) }
    val selected = selectedIndex?.let { slices[it] }
    val surface = MaterialTheme.colorScheme.surface

    val description = if (total == 0.0) {
        "Gráfica de pastel: sin ingresos ni gastos este mes"
    } else {
        "Gráfica de pastel: " + slices.joinToString(", ") {
            "${it.label} ${formatCurrency(it.value)}, ${percentOf(it.value, total)}"
        }
    }

    Box(
        modifier = modifier
            .size(200.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slices) {
                    detectTapGestures { tap ->
                        if (total == 0.0) return@detectTapGestures
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outer = minOf(size.width, size.height) / 2f
                        val distance = hypot(tap.x - center.x, tap.y - center.y)
                        // Solo cuenta el toque sobre el anillo (zona un poco más
                        // generosa que el trazo para que sea fácil de tocar).
                        if (distance < outer * 0.45f || distance > outer) {
                            selectedIndex = null
                            return@detectTapGestures
                        }
                        // Ángulo medido desde las 12 en punto, en sentido horario.
                        val degrees = (Math.toDegrees(
                            atan2((tap.y - center.y).toDouble(), (tap.x - center.x).toDouble())
                        ) + 90.0 + 360.0) % 360.0
                        var start = 0.0
                        for ((i, slice) in slices.withIndex()) {
                            val sweep = slice.value / total * 360.0
                            if (degrees < start + sweep) {
                                selectedIndex = if (selectedIndex == i) null else i
                                break
                            }
                            start += sweep
                        }
                    }
                }
        ) {
            val strokeWidth = size.minDimension * 0.18f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            if (total == 0.0) {
                drawArc(ProgressTrack, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))
                return@Canvas
            }

            var start = -90f
            slices.forEach { slice ->
                val sweep = (slice.value / total * 360.0).toFloat()
                if (sweep > 0f) {
                    drawArc(slice.color, start, sweep, false, topLeft, arcSize, style = Stroke(strokeWidth))
                }
                start += sweep
            }
            // Separación de 2dp (color de la tarjeta) entre rebanadas, si hay más de una.
            if (slices.count { it.value > 0 } > 1) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val inner = diameter / 2f - strokeWidth / 2f - 1f
                val outer = diameter / 2f + strokeWidth / 2f + 1f
                var angle = -90.0
                slices.forEach { slice ->
                    val rad = Math.toRadians(angle)
                    val dir = Offset(kotlin.math.cos(rad).toFloat(), kotlin.math.sin(rad).toFloat())
                    drawLine(surface, center + dir * inner, center + dir * outer, strokeWidth = 2.dp.toPx())
                    angle += slice.value / total * 360.0
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val net = slices.getOrNull(0)?.value.orZero() - slices.getOrNull(1)?.value.orZero()
            Text(
                text = when {
                    total == 0.0 -> "Sin movimientos"
                    selected != null -> selected.label
                    else -> "Neto del mes"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (total > 0.0) {
                Text(
                    text = formatCurrency(selected?.value ?: net),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selected != null) {
                    Text(
                        text = percentOf(selected.value, total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun Double?.orZero() = this ?: 0.0

private fun percentOf(value: Double, total: Double): String =
    if (total == 0.0) "0%" else "${Math.round(value / total * 100)}%"

// Leyenda de la gráfica: punto de color + nombre a la izquierda; monto y
// porcentaje en texto a la derecha (para no depender solo del color).
@Composable
private fun PieLegend(slices: List<PieSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slices.forEach { slice ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(slice.color, CircleShape)
                )
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                )
                Text(
                    text = formatCurrency(slice.value),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = percentOf(slice.value, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .widthIn(min = 40.dp)
                )
            }
        }
    }
}
