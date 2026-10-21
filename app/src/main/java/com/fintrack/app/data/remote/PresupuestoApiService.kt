package com.fintrack.app.data.remote

import com.fintrack.app.data.model.EstadoPresupuesto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Un presupuesto es un "periodo": un monto total para unas fechas, repartido
// por porcentaje entre los 4 grupos fijos del backend (Deudas, Gastos fijos,
// Gustos, Ahorro). Los porcentajes de los 4 grupos deben sumar 100.

// Los 4 tipos de período que acepta el backend al crear un presupuesto.
enum class TipoPeriodo { SEMANAL, QUINCENAL, MENSUAL, PERSONALIZADO }

// Lo que se manda por cada grupo al crear un presupuesto o al cambiar su
// distribución (PUT /api/v1/presupuestos/{id}/distribucion).
// "porcentaje_alerta_grupo" se queda en su valor por defecto (80): el backend
// ya lo aplica si no se manda, no hace falta un control en la UI.
data class GrupoDistribucionRequest(
    val grupo_id: String,
    val porcentaje_asignado_grupo: Int,
    val porcentaje_alerta_grupo: Int = 80
)

// "monto_total_presupuesto" va como String: el schema acepta number o
// string, y es el mismo patrón ya usado en transacciones y metas.
data class CrearPresupuestoRequest(
    val codigo_periodo: String,
    val tipo_periodo: TipoPeriodo,
    // Formato "yyyy-MM-dd" (date, sin hora).
    val fecha_inicio_periodo: String,
    val fecha_fin_periodo: String,
    val monto_total_presupuesto: String,
    // Siempre los 4 grupos (el backend exige exactamente 4).
    val grupos: List<GrupoDistribucionRequest>
)

// PATCH /api/v1/presupuestos/{id}: solo se puede cambiar la fecha fin y el
// monto total. Lo que vaya en null no se modifica (Gson no serializa nulls).
data class ActualizarPresupuestoRequest(
    val fecha_fin_periodo: String? = null,
    val monto_total_presupuesto: String? = null
)

// Los montos llegan como number; Gson los convierte a String sin problema y
// se parsean con toDoubleOrNull(). Todo es nullable porque Gson ignora la
// nulabilidad de Kotlin: un campo que no llega quedaría null y tiraría la app
// al usarlo como no-null.
data class PeriodoResponse(
    val id_periodo: String?,
    val codigo_periodo: String?,
    val tipo_periodo: String?,
    val fecha_inicio_periodo: String?,
    val fecha_fin_periodo: String?,
    val fecha_cierre_periodo: String?,
    val estado_periodo: EstadoPresupuesto?,
    // Lo que el usuario capturó al crear el presupuesto.
    val monto_total_presupuesto: String?,
    // El monto que realmente se puede gastar en el período (lo limitan los
    // ingresos reales): es el "límite" que muestra la app.
    val monto_presupuesto_periodo: String?,
    val aplicado_total_periodo: String?,
    val monto_restante_periodo: String?,
    val monto_excedido_periodo: String?,
    val porcentaje_consumido_periodo: String?,
    // "OK", etc.
    val estado_consumo_periodo: String?,
    val periodo_vencido: Boolean?
)

data class PeriodoListaSalida(
    val presupuestos: List<PeriodoResponse>?
)

// El desglose del presupuesto en uno de los 4 grupos.
data class PeriodoGrupoResponse(
    val grupo_id: String?,
    val nombre_grupo: String?,
    val porcentaje_asignado_grupo: Int?,
    val monto_presupuesto_grupo: String?,
    val aplicado_dinero_grupo: String?,
    val monto_restante_grupo: String?,
    val monto_excedido_grupo: String?,
    // null cuando el grupo tiene 0% asignado (no hay sobre qué calcularlo).
    val porcentaje_utilizado_grupo: String?,
    val estado_consumo_grupo: String?
)

// GET /api/v1/presupuestos/{id}.
data class PeriodoDetalleResponse(
    val periodo: PeriodoResponse?,
    val grupos: List<PeriodoGrupoResponse>?
)

// Catálogo de grupos (GET /api/v1/grupos), para armar el formulario de
// "Nuevo presupuesto" con sus porcentajes sugeridos.
data class GrupoResponse(
    val id_grupo: String,
    // "G001".."G004"; Predicción lo usa para encontrar Gastos fijos y Gustos.
    val codigo_grupo: String,
    val nombre_grupo: String,
    val porcentaje_sugerido_grupo: Int,
    val es_activo_grupo: Boolean
)

data class GrupoListaSalida(
    val grupos: List<GrupoResponse>
)

// Las operaciones que modifican un presupuesto devuelven ResponseBody: el
// esquema del backend no documenta su respuesta, así que después de cada una
// la app vuelve a pedir el detalle o la lista en lugar de depender de ella.
interface PresupuestoApiService {
    // Devuelve los presupuestos de todos los estados: el filtro por estado
    // se hace en la app.
    @GET("api/v1/presupuestos")
    suspend fun obtenerPresupuestos(
        @Header("Authorization") authorization: String
    ): PeriodoListaSalida

    @POST("api/v1/presupuestos")
    suspend fun crearPresupuesto(
        @Header("Authorization") authorization: String,
        @Body request: CrearPresupuestoRequest
    ): ResponseBody

    @GET("api/v1/presupuestos/{id_periodo}")
    suspend fun obtenerPresupuesto(
        @Header("Authorization") authorization: String,
        @Path("id_periodo") idPeriodo: String
    ): PeriodoDetalleResponse

    @PATCH("api/v1/presupuestos/{id_periodo}")
    suspend fun actualizarPresupuesto(
        @Header("Authorization") authorization: String,
        @Path("id_periodo") idPeriodo: String,
        @Body request: ActualizarPresupuestoRequest
    ): ResponseBody

    @PUT("api/v1/presupuestos/{id_periodo}/distribucion")
    suspend fun actualizarDistribucion(
        @Header("Authorization") authorization: String,
        @Path("id_periodo") idPeriodo: String,
        @Body grupos: List<GrupoDistribucionRequest>
    ): ResponseBody

    @POST("api/v1/presupuestos/{id_periodo}/finalizar")
    suspend fun finalizarPresupuesto(
        @Header("Authorization") authorization: String,
        @Path("id_periodo") idPeriodo: String
    ): ResponseBody

    @POST("api/v1/presupuestos/{id_periodo}/cancelar")
    suspend fun cancelarPresupuesto(
        @Header("Authorization") authorization: String,
        @Path("id_periodo") idPeriodo: String
    ): ResponseBody

    @GET("api/v1/grupos")
    suspend fun obtenerGrupos(
        @Header("Authorization") authorization: String
    ): GrupoListaSalida
}
