package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.app.data.model.EstadoPresupuesto
import com.fintrack.app.data.remote.ClusteringGastosResponse
import com.fintrack.app.data.remote.GrupoResponse
import com.fintrack.app.data.remote.PrediccionSemanalResponse
import com.fintrack.app.data.remote.RetrofitClient
import com.fintrack.app.data.remote.SaludPeriodoResponse
import com.fintrack.app.data.remote.runAuthenticatedCall
import com.fintrack.app.ui.components.FinTrackTopBar
import com.fintrack.app.ui.components.LabeledProgressBar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.components.formatCurrency
import com.fintrack.app.ui.components.formatShortDate
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

// Convierte un código del backend ("EN_RIESGO", "HISTORIAL_INSUFICIENTE") en
// texto legible ("En riesgo", "Historial insuficiente").
internal fun humanizeCode(code: String): String =
    code.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

// Pantalla de Predicción: usa los endpoints de Analítica FinTrack V1.1 para
// mostrar la predicción de gasto semanal, la salud del presupuesto activo y
// la detección de gastos hormiga. Cada sección carga por separado, así que
// si una falla las demás se siguen mostrando.
@Composable
fun PredictionScreen(onOpenProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Grupos con predicción propia: G002 (Gastos fijos) y G003 (Gustos). Si
    // no se pueden traer, la predicción se queda solo en modo global.
    var predictionGroups by remember { mutableStateOf<List<GrupoResponse>>(emptyList()) }
    LaunchedEffect(Unit) {
        runAuthenticatedCall(context, onError = {}) { auth ->
            RetrofitClient.presupuestoApi.obtenerGrupos(auth)
        }?.let { response ->
            predictionGroups = response.grupos.filter { it.codigo_grupo in setOf("G002", "G003") }
                .sortedBy { it.codigo_grupo }
        }
    }

    // null = predicción global.
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var prediction by remember { mutableStateOf<PrediccionSemanalResponse?>(null) }
    var isLoadingPrediction by remember { mutableStateOf(true) }
    var predictionError by remember { mutableStateOf<String?>(null) }

    val loadPrediction: (Boolean) -> Unit = { forceRecalc ->
        scope.launch {
            isLoadingPrediction = true
            predictionError = null
            prediction = runAuthenticatedCall(context, onError = { predictionError = it }) { auth ->
                RetrofitClient.analiticaApi.obtenerPrediccionSemanal(
                    authorization = auth,
                    grupoId = selectedGroupId,
                    forzarRecalculo = if (forceRecalc) "true" else null
                )
            }
            isLoadingPrediction = false
        }
    }

    LaunchedEffect(selectedGroupId) { loadPrediction(false) }

    // La salud se calcula sobre un presupuesto concreto: se usa el activo.
    // "Sin presupuesto activo" no es un error, se muestra como aviso.
    var health by remember { mutableStateOf<SaludPeriodoResponse?>(null) }
    var hasActiveBudget by remember { mutableStateOf(true) }
    var isLoadingHealth by remember { mutableStateOf(true) }
    var healthError by remember { mutableStateOf<String?>(null) }

    var clustering by remember { mutableStateOf<ClusteringGastosResponse?>(null) }
    var isLoadingClustering by remember { mutableStateOf(true) }
    var clusteringError by remember { mutableStateOf<String?>(null) }

    val loadBudgetAnalytics: () -> Unit = {
        scope.launch {
            isLoadingHealth = true
            isLoadingClustering = true
            healthError = null
            clusteringError = null
            val onError: (String) -> Unit = { healthError = it; clusteringError = it }
            val activeBudgetId = runAuthenticatedCall(context, onError) { auth ->
                RetrofitClient.presupuestoApi.obtenerPresupuestos(auth)
            }?.let { response ->
                response.presupuestos.orEmpty().firstOrNull { it.estado_periodo == EstadoPresupuesto.ACTIVO }?.id_periodo
            }
            if (healthError == null) {
                hasActiveBudget = activeBudgetId != null
                if (activeBudgetId != null) {
                    health = runAuthenticatedCall(context, onError = { healthError = it }) { auth ->
                        RetrofitClient.analiticaApi.obtenerSaludPeriodo(auth, activeBudgetId)
                    }
                    clustering = runAuthenticatedCall(context, onError = { clusteringError = it }) { auth ->
                        RetrofitClient.analiticaApi.obtenerClusteringGastos(auth, activeBudgetId)
                    }
                }
            }
            isLoadingHealth = false
            isLoadingClustering = false
        }
    }

    LaunchedEffect(Unit) { loadBudgetAnalytics() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackTopBar(title = "Predicción", onMenuClick = onOpenProfile) }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WeeklyPredictionCard(
                    prediction = prediction,
                    isLoading = isLoadingPrediction,
                    error = predictionError,
                    groups = predictionGroups,
                    selectedGroupId = selectedGroupId,
                    onSelectGroup = { selectedGroupId = it },
                    onRecalculate = { loadPrediction(true) }
                )
                BudgetHealthCard(
                    health = health,
                    hasActiveBudget = hasActiveBudget,
                    isLoading = isLoadingHealth,
                    error = healthError,
                    onRetry = loadBudgetAnalytics
                )
                AntExpensesCard(
                    clustering = clustering,
                    hasActiveBudget = hasActiveBudget,
                    isLoading = isLoadingClustering,
                    error = clusteringError,
                    onRetry = loadBudgetAnalytics
                )
            }
        }

        item { androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp)) }
    }
}

// Encabezado común de cada sección: ícono + título.
@Composable
private fun AnalyticsHeader(title: String, color: Color = FinTrackNavy) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = color)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// Carga / error con botón de reintentar, compartido por las 3 secciones.
// Devuelve true si ya dibujó algo (y la sección no debe dibujar sus datos).
@Composable
private fun LoadingOrError(isLoading: Boolean, error: String?, onRetry: () -> Unit): Boolean {
    when {
        isLoading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = FinTrackNavy) }
        error != null -> Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(text = error, color = FinTrackRed, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Reintentar",
                color = FinTrackNavy,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable { onRetry() }
            )
        }
        else -> return false
    }
    return true
}

// Texto explicativo que manda el backend, ya pensado para el usuario.
@Composable
private fun ExplanationText(text: String?, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 12.dp)
    )
}

@Composable
private fun AnalyticsRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Text(text = value, color = valueColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AnalyticsChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) FinTrackNavy else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

private fun String?.asCurrency(): String? = this?.toDoubleOrNull()?.let { formatCurrency(it) }

// Predicción de gasto de la semana (GET /api/v1/analitica/prediccion-semanal),
// global o de un grupo. Sin historial suficiente muestra cuántas semanas
// faltan en lugar de un monto.
@Composable
private fun WeeklyPredictionCard(
    prediction: PrediccionSemanalResponse?,
    isLoading: Boolean,
    error: String?,
    groups: List<GrupoResponse>,
    selectedGroupId: String?,
    onSelectGroup: (String?) -> Unit,
    onRecalculate: () -> Unit
) {
    SectionCard {
        AnalyticsHeader("Predicción semanal")
        if (groups.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsChip("Todos mis gastos", selectedGroupId == null) { onSelectGroup(null) }
                groups.forEach { group ->
                    AnalyticsChip(group.nombre_grupo, selectedGroupId == group.id_grupo) { onSelectGroup(group.id_grupo) }
                }
            }
        }
        if (LoadingOrError(isLoading, error, onRetry = onRecalculate)) return@SectionCard
        val data = prediction ?: return@SectionCard

        val start = data.periodo_inicio_objetivo
        val end = data.periodo_fin_objetivo
        if (start != null && end != null) {
            Text(
                text = "Semana del ${formatShortDate(start)} al ${formatShortDate(end)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        val estimate = data.gasto_estimado.asCurrency()
        if (estimate != null) {
            Text(
                text = estimate,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = FinTrackNavy,
                modifier = Modifier.padding(top = 4.dp)
            )
            val min = data.rango_min_ux.asCurrency()
            val max = data.rango_max_ux.asCurrency()
            if (min != null && max != null) AnalyticsRow("Rango probable", "$min – $max")
            data.nivel_confiabilidad_ux?.let { AnalyticsRow("Confiabilidad", humanizeCode(it)) }
        } else {
            val used = data.semanas_completas_utilizadas ?: 0
            val required = data.semanas_requeridas ?: 0
            Text(
                text = data.estado?.let { humanizeCode(it) } ?: "Sin predicción",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (required > 0) {
                LabeledProgressBar(
                    label = "Historial",
                    valueText = "$used de $required semanas",
                    progress = (used.toFloat() / required).coerceIn(0f, 1f),
                    progressColor = FinTrackNavy,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        ExplanationText(data.explicacion_ux)
        Text(
            text = "Recalcular",
            color = FinTrackNavy,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable { onRecalculate() }
        )
    }
}

// Salud del presupuesto activo (GET /api/v1/analitica/salud-periodo/{id}):
// puntaje 0..100, etiqueta y proyección al cierre del período.
@Composable
private fun BudgetHealthCard(
    health: SaludPeriodoResponse?,
    hasActiveBudget: Boolean,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    SectionCard {
        AnalyticsHeader("Salud del presupuesto", color = FinTrackGreen)
        if (LoadingOrError(isLoading, error, onRetry)) return@SectionCard
        if (!hasActiveBudget) {
            ExplanationText("Crea un presupuesto activo en la pestaña Presupuesto para ver su salud.")
            return@SectionCard
        }
        val data = health ?: return@SectionCard

        val score = data.score_salud_difuso
        val isFavorable = data.etiqueta_salud == "FAVORABLE"
        val scoreColor = if (isFavorable) FinTrackGreen else FinTrackRed
        if (score != null) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
                Text(text = "${score.toInt()}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                Text(
                    text = " / 100",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                data.etiqueta_salud?.let {
                    Text(
                        text = humanizeCode(it),
                        style = MaterialTheme.typography.titleMedium,
                        color = scoreColor,
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                    )
                }
            }
            LabeledProgressBar(
                label = "Puntaje",
                valueText = "${score.toInt()}%",
                progress = (score / 100).toFloat().coerceIn(0f, 1f),
                progressColor = scoreColor,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Text(
                text = data.estado?.let { humanizeCode(it) } ?: "Sin evaluación",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        ExplanationText(data.explicacion_ux)

        val projection = data.proyeccion_periodo
        if (projection != null) {
            Text(
                text = "Si sigues a este ritmo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            projection.aplicado_estimado_cierre.asCurrency()?.let { AnalyticsRow("Gasto estimado al cierre", it) }
            projection.ritmo_diario_aplicado.asCurrency()?.let { AnalyticsRow("Ritmo diario", it) }
            projection.diferencia_vs_presupuesto?.toDoubleOrNull()?.let { difference ->
                // Negativo = el cierre estimado queda por debajo del presupuesto.
                AnalyticsRow(
                    label = if (difference > 0) "Exceso estimado" else "Margen estimado",
                    value = formatCurrency(kotlin.math.abs(difference)),
                    valueColor = if (difference > 0) FinTrackRed else FinTrackGreen
                )
            }
            val observed = projection.dias_observados
            val total = projection.dias_periodo
            if (observed != null && total != null) AnalyticsRow("Días transcurridos", "$observed de $total")
        }
    }
}

// Detección de gastos hormiga (GET /api/v1/analitica/clustering-gastos):
// agrupa los gastos del presupuesto activo por monto.
@Composable
private fun AntExpensesCard(
    clustering: ClusteringGastosResponse?,
    hasActiveBudget: Boolean,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    SectionCard {
        AnalyticsHeader("Gastos hormiga")
        if (LoadingOrError(isLoading, error, onRetry)) return@SectionCard
        if (!hasActiveBudget) {
            ExplanationText("Crea un presupuesto activo en la pestaña Presupuesto para analizar sus gastos.")
            return@SectionCard
        }
        val data = clustering ?: return@SectionCard

        data.total_gastos_analizados?.let {
            Text(
                text = "$it gastos analizados en el presupuesto activo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        val clusters = data.clusters.orEmpty()
        if (clusters.isEmpty()) {
            Text(
                text = data.estado?.let { humanizeCode(it) } ?: "Sin grupos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            clusters.forEachIndexed { index, cluster ->
                ClusterRow(index, cluster)
            }
        }
        ExplanationText(data.explicacion_ux)
    }
}

// Un grupo de gastos. Como aún no se conoce el formato exacto de cada
// cluster, se muestran sus campos simples (texto/número) con el nombre del
// campo hecho legible; los que parecen montos se formatean como moneda.
@Composable
private fun ClusterRow(index: Int, cluster: JsonObject) {
    val fields = cluster.entrySet()
        .filter { (_, value) -> value.isJsonPrimitive }
        .map { (key, value) -> key to value.asJsonPrimitive }
    val title = fields.firstOrNull { (key, value) -> value.isString && ("etiqueta" in key || "nombre" in key) }
        ?.second?.asString?.let { humanizeCode(it) }
        ?: "Grupo ${index + 1}"
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        fields.filterNot { (key, _) -> key.startsWith("id_") || key.endsWith("_id") || "etiqueta" in key || "nombre" in key }
            .forEach { (key, value) ->
                val text = when {
                    value.isNumber && ("monto" in key || "total" in key || "promedio" in key) -> formatCurrency(value.asDouble)
                    value.isBoolean -> if (value.asBoolean) "Sí" else "No"
                    else -> value.asString.let { if (value.isString) humanizeCode(it) else it }
                }
                AnalyticsRow(humanizeCode(key), text)
            }
    }
}
