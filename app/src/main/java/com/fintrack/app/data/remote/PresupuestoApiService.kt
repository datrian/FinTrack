package com.fintrack.app.data.remote

import com.fintrack.app.data.model.EstadoPresupuesto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// "porcentaje_alerta_presupuesto" se queda en su valor por defecto (80): el
// backend ya lo aplica si no se manda, no hace falta un control en la UI.
data class DetallePresupuestoCrearRequest(
    val categoria_id: String,
    val porcentaje_asignado_presupuesto: Int,
    val porcentaje_alerta_presupuesto: Int = 80
)

// "monto_limite_presupuesto" va como String (no number): mismo patrón ya
// confirmado contra el backend real en transacciones y metas (el schema
// admite number o string, pero el validador exige texto decimal).
data class CrearPresupuestoRequest(
    // Formato "yyyy-MM-dd" (date, sin hora). La fecha de inicio la pone el
    // backend automáticamente.
    val fecha_fin_presupuesto: String,
    val monto_limite_presupuesto: String,
    val detalles: List<DetallePresupuestoCrearRequest>
)

data class PresupuestoResumenResponse(
    val id_presupuesto: String,
    val fecha_inicio_presupuesto: String,
    val fecha_fin_presupuesto: String,
    val fecha_cierre_presupuesto: String?,
    val estado_presupuesto: EstadoPresupuesto,
    // El backend serializa los montos como string (no number) para evitar
    // problemas de precisión con decimales.
    val monto_limite_presupuesto: String,
    val monto_consumido_presupuesto: String,
    val monto_restante_presupuesto: String,
    val monto_excedido_presupuesto: String,
    val porcentaje_consumido_presupuesto: String,
    val indicador_presupuesto: String,
    val periodo_vencido: Boolean
)

data class PresupuestoListaSalida(
    val presupuestos: List<PresupuestoResumenResponse>,
    val total: Int,
    val limite: Int,
    val offset: Int
)

data class DetallePresupuestoSalidaResponse(
    val id_detalle_presupuesto: String,
    val categoria_id: String,
    val nombre_categoria: String,
    val porcentaje_asignado_presupuesto: Int,
    val porcentaje_alerta_presupuesto: Int,
    val monto_limite_categoria: String,
    val monto_consumido_categoria: String,
    val monto_restante_categoria: String,
    val monto_excedido_categoria: String,
    val porcentaje_consumido_categoria: String,
    val indicador_categoria: String
)

data class PresupuestoDetalleResponse(
    val presupuesto: PresupuestoResumenResponse,
    val monto_consumido_presupuestado: String,
    val monto_consumido_no_presupuestado: String,
    val detalles: List<DetallePresupuestoSalidaResponse>
)

interface PresupuestoApiService {
    // "estado" acepta ACTIVO/FINALIZADO/CANCELADO/TODOS (por eso es String y
    // no EstadoPresupuesto: "TODOS" no es un estado real). Sin "estado" el
    // backend filtra por ACTIVO (su valor por defecto).
    @GET("api/v1/presupuestos")
    suspend fun obtenerPresupuestos(
        @Header("Authorization") authorization: String,
        @Query("estado") estado: String? = null,
        @Query("limite") limite: Int = 20,
        @Query("offset") offset: Int = 0
    ): PresupuestoListaSalida

    @POST("api/v1/presupuestos")
    suspend fun crearPresupuesto(
        @Header("Authorization") authorization: String,
        @Body request: CrearPresupuestoRequest
    ): PresupuestoDetalleResponse

    @GET("api/v1/presupuestos/{id_presupuesto}")
    suspend fun obtenerPresupuesto(
        @Header("Authorization") authorization: String,
        @Path("id_presupuesto") idPresupuesto: String
    ): PresupuestoDetalleResponse
}
