package com.fintrack.app.data.remote

import com.fintrack.app.data.model.EstadoMeta
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// "monto_objetivo_meta" va como String (no number): mismo patrón ya
// confirmado contra el backend real en transacciones (el schema admite
// number o string, pero el validador exige texto decimal).
data class CrearMetaRequest(
    val cuenta_id: String,
    val nombre_meta: String,
    val monto_objetivo_meta: String,
    // Formato "yyyy-MM-dd" (date, sin hora).
    val fecha_limite_meta: String,
    val descripcion_meta: String? = null
)

data class MetaResponse(
    val id_meta: String,
    val cuenta_id: String,
    val nombre_meta: String,
    val descripcion_meta: String?,
    val monto_objetivo_meta: String,
    val fecha_limite_meta: String,
    val fecha_creacion_meta: String,
    val fecha_cierre_meta: String?,
    val estado_meta: EstadoMeta,
    val monto_actual_meta: String,
    val monto_faltante_meta: String,
    val porcentaje_avance_meta: String,
    val reserva_vigente_meta: String,
    val plazo_vencido: Boolean
)

data class MetaListaSalida(
    val metas: List<MetaResponse>,
    val total: Int,
    val limite: Int,
    val offset: Int
)

// Resumen de la cuenta asociada a la meta: cuánto tiene reservado para otras
// metas, cuánto está libre y cuánto se le puede aportar todavía.
data class ResumenCuentaMetaResponse(
    val saldo_actual_cuenta: String,
    val reserva_total_cuenta: String,
    val dinero_libre_cuenta: String,
    val disponible_para_aportar: String,
    val deficit_reservas_cuenta: String
)

data class MetaDetalleResponse(
    val meta: MetaResponse,
    val resumen_cuenta: ResumenCuentaMetaResponse
)

// El backend solo permite editar nombre y descripción por esta vía: el
// monto objetivo y la fecha límite no se pueden cambiar una vez creada la meta.
data class ActualizarMetaRequest(
    val nombre_meta: String? = null,
    val descripcion_meta: String? = null
)

interface MetaApiService {
    // Requiere sesión activa. Sin "estado" el backend filtra por ACTIVA
    // (su valor por defecto), no devuelve los 4 estados a la vez.
    @GET("api/v1/metas")
    suspend fun obtenerMetas(
        @Header("Authorization") authorization: String,
        @Query("estado") estado: EstadoMeta? = null,
        @Query("limite") limite: Int = 20,
        @Query("offset") offset: Int = 0
    ): MetaListaSalida

    @POST("api/v1/metas")
    suspend fun crearMeta(
        @Header("Authorization") authorization: String,
        @Body request: CrearMetaRequest
    ): MetaResponse

    @GET("api/v1/metas/{id_meta}")
    suspend fun obtenerMeta(
        @Header("Authorization") authorization: String,
        @Path("id_meta") idMeta: String
    ): MetaDetalleResponse

    @PATCH("api/v1/metas/{id_meta}")
    suspend fun actualizarMeta(
        @Header("Authorization") authorization: String,
        @Path("id_meta") idMeta: String,
        @Body request: ActualizarMetaRequest
    ): MetaResponse
}
