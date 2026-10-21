package com.fintrack.app.data.remote

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// Endpoints de "Analítica FinTrack V1.1". El esquema del backend no documenta
// sus respuestas, así que estos modelos salen de las respuestas reales y solo
// incluyen lo que la app muestra (el backend manda además fichas técnicas,
// cachés y huellas que no se usan). Todo es nullable porque Gson ignora la
// nulabilidad de Kotlin y varios campos llegan en null cuando no hay datos
// suficientes. Los montos llegan a veces como number y a veces como string;
// Gson convierte ambos a String y se parsean con toDoubleOrNull().
//
// Todas las respuestas traen "estado" (OK, HISTORIAL_INSUFICIENTE,
// DATOS_INSUFICIENTES, SIN_SUGERENCIA, ...) y "explicacion_ux", un texto ya
// pensado para mostrarse al usuario tal cual.

// GET /api/v1/analitica/prediccion-semanal: gasto estimado de la semana.
// Necesita 8 semanas completas de historial; mientras no las haya, los
// campos de la predicción llegan en null.
data class PrediccionSemanalResponse(
    val estado: String?,
    val explicacion_ux: String?,
    val gasto_estimado: String?,
    val rango_min_ux: String?,
    val rango_max_ux: String?,
    val nivel_confiabilidad_ux: String?,
    val periodo_inicio_objetivo: String?,
    val periodo_fin_objetivo: String?,
    val semanas_completas_utilizadas: Int?,
    val semanas_requeridas: Int?
)

// GET /api/v1/analitica/clustering-gastos: agrupa los gastos del período por
// monto (K-means) para detectar "gastos hormiga". Necesita al menos 6 gastos
// con 3 montos distintos.
data class ClusteringGastosResponse(
    val estado: String?,
    val explicacion_ux: String?,
    val total_gastos_analizados: Int?,
    // Todavía no se ha visto una respuesta con clusters, así que cada uno se
    // guarda tal cual llega y la pantalla muestra sus campos genéricamente.
    val clusters: List<JsonObject>?
)

// Proyección de cómo cerrará el período si se mantiene el ritmo actual.
data class ProyeccionPeriodoResponse(
    val estado: String?,
    val aplicado_estimado_cierre: String?,
    val ritmo_diario_aplicado: String?,
    val diferencia_vs_presupuesto: String?,
    val dias_observados: Int?,
    val dias_periodo: Int?
)

// GET /api/v1/analitica/salud-periodo/{id}: puntaje 0..100 de qué tan sano
// va el presupuesto (lógica difusa), con su proyección al cierre.
data class SaludPeriodoResponse(
    val estado: String?,
    val explicacion_ux: String?,
    val score_salud_difuso: Double?,
    // "FAVORABLE", "EN_RIESGO", ...
    val etiqueta_salud: String?,
    val proyeccion_periodo: ProyeccionPeriodoResponse?
)

// GET /api/v1/analitica/sugerir-clasificacion: sugiere categoría (y
// subcategoría) para un gasto a partir de su descripción.
data class SugerenciaClasificacionResponse(
    val estado: String?,
    val explicacion_ux: String?,
    val sugerencia_encontrada: Boolean?,
    val categoria_id: String?,
    val nombre_categoria: String?,
    val subcategoria_id: String?,
    val nombre_subcategoria: String?,
    val confianza: String?
)

interface AnaliticaApiService {
    // "grupo_id" es el UUID del grupo G002 (Gastos fijos) o G003 (Gustos);
    // sin él la predicción es global. "forzar_recalculo" va como texto
    // ("true"/"false") porque así lo define el backend.
    @GET("api/v1/analitica/prediccion-semanal")
    suspend fun obtenerPrediccionSemanal(
        @Header("Authorization") authorization: String,
        @Query("grupo_id") grupoId: String? = null,
        @Query("forzar_recalculo") forzarRecalculo: String? = null
    ): PrediccionSemanalResponse

    // Sin "periodo_id" el backend usa el período ACTIVO.
    @GET("api/v1/analitica/clustering-gastos")
    suspend fun obtenerClusteringGastos(
        @Header("Authorization") authorization: String,
        @Query("periodo_id") periodoId: String? = null
    ): ClusteringGastosResponse

    @GET("api/v1/analitica/salud-periodo/{id_periodo}")
    suspend fun obtenerSaludPeriodo(
        @Header("Authorization") authorization: String,
        @Path("id_periodo") idPeriodo: String
    ): SaludPeriodoResponse

    // "detalle" es la descripción del gasto, de 3 a 500 caracteres.
    @GET("api/v1/analitica/sugerir-clasificacion")
    suspend fun sugerirClasificacion(
        @Header("Authorization") authorization: String,
        @Query("detalle") detalle: String
    ): SugerenciaClasificacionResponse
}
