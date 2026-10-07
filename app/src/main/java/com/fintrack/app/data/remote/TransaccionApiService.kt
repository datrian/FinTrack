package com.fintrack.app.data.remote

import com.fintrack.app.data.model.TipoTransaccion
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// "monto_transaccion" va como String (no Double): aunque el schema admite
// number o string, el validador real del backend rechaza los números y
// exige texto decimal ("Value error, monto_transaccion debe enviarse como
// texto decimal"), confirmado contra el backend real.
data class CrearTransaccionRequest(
    val tipo_transaccion: TipoTransaccion,
    val cuenta_id: String,
    val monto_transaccion: String,
    val cuenta_destino_id: String? = null,
    val categoria_id: String? = null,
    val subcategoria_id: String? = null,
    val comentario_transaccion: String? = null
)

data class TransaccionResponse(
    val id_transaccion: String,
    val cuenta_id: String,
    val tipo_transaccion: TipoTransaccion,
    val cuenta_destino_id: String?,
    val categoria_id: String?,
    val subcategoria_id: String?,
    // El backend serializa el monto como string (no number) para evitar
    // problemas de precisión con decimales.
    val monto_transaccion: String,
    val fecha_registro_transaccion: String,
    val comentario_transaccion: String?
)

data class TransaccionListaSalida(
    val transacciones: List<TransaccionResponse>,
    val total: Int,
    val limite: Int,
    val offset: Int
)

interface TransaccionApiService {
    // Requiere sesión activa (Authorization: Bearer <token>). Sin
    // tipo_transaccion devuelve todos los tipos; "limite" por defecto es 20.
    @GET("api/v1/transacciones")
    suspend fun obtenerTransacciones(
        @Header("Authorization") authorization: String,
        @Query("tipo_transaccion") tipoTransaccion: TipoTransaccion? = null,
        @Query("limite") limite: Int = 20,
        @Query("offset") offset: Int = 0
    ): TransaccionListaSalida

    @POST("api/v1/transacciones")
    suspend fun crearTransaccion(
        @Header("Authorization") authorization: String,
        @Body request: CrearTransaccionRequest
    ): TransaccionResponse

    @GET("api/v1/transacciones/{id_transaccion}")
    suspend fun obtenerTransaccion(
        @Header("Authorization") authorization: String,
        @Path("id_transaccion") idTransaccion: String
    ): TransaccionResponse
}
