package com.fintrack.app.data.remote

import com.fintrack.app.data.model.AccountType
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class CrearCuentaRequest(
    val nombre_cuenta: String,
    val tipo_cuenta: AccountType,
    val saldo_inicial_cuenta: Double,
    val institucion_cuenta: String? = null,
    val numero_cuenta: String? = null,
    val limite_credito_cuenta: Double? = null
)

// Actualización parcial (PATCH): el tipo de cuenta no se puede cambiar una
// vez creada, por eso no aparece aquí.
data class ActualizarCuentaRequest(
    val nombre_cuenta: String? = null,
    val institucion_cuenta: String? = null,
    val numero_cuenta: String? = null,
    val saldo_inicial_cuenta: Double? = null,
    val limite_credito_cuenta: Double? = null
)

// Versión liviana de una cuenta, tal como la devuelve el listado (GET).
data class CuentaListaItem(
    val id_cuenta: String,
    val nombre_cuenta: String,
    val tipo_cuenta: AccountType,
    val institucion_cuenta: String?,
    val numero_cuenta_enmascarado: String?,
    val es_activa_cuenta: Boolean,
    // El backend serializa los montos como string (no number) para evitar
    // problemas de precisión con decimales.
    val saldo_actual_cuenta: String
)

data class CuentaListaSalida(
    val cuentas: List<CuentaListaItem>,
    val total: Int
)

data class CambiarEstadoCuentaRequest(val es_activa_cuenta: Boolean)

data class CuentaEstadoResponse(val id_cuenta: String, val es_activa_cuenta: Boolean)

// Versión completa de una cuenta, tal como la devuelve la creación (POST).
data class CuentaResponse(
    val id_cuenta: String,
    val nombre_cuenta: String,
    val tipo_cuenta: AccountType,
    val institucion_cuenta: String?,
    val numero_cuenta_enmascarado: String?,
    val es_activa_cuenta: Boolean,
    val fecha_creacion_cuenta: String,
    val saldo_inicial_cuenta: String,
    val saldo_actual_cuenta: String,
    val limite_credito_cuenta: String?,
    val credito_utilizado_cuenta: String?,
    val saldo_disponible_credito_cuenta: String?,
    val porcentaje_utilizacion_credito_cuenta: String?
)

interface CuentaApiService {
    // Requiere sesión activa (Authorization: Bearer <token>). Sin parámetros,
    // el backend devuelve solo las cuentas activas (estado=ACTIVA por defecto).
    @GET("api/v1/cuentas")
    suspend fun obtenerCuentas(@Header("Authorization") authorization: String): CuentaListaSalida

    @POST("api/v1/cuentas")
    suspend fun crearCuenta(
        @Header("Authorization") authorization: String,
        @Body request: CrearCuentaRequest
    ): CuentaResponse

    // Requiere sesión activa (Authorization: Bearer <token>). Devuelve el
    // detalle completo de una cuenta (incluye saldo inicial, límite de
    // crédito, etc., que el listado no trae).
    @GET("api/v1/cuentas/{id_cuenta}")
    suspend fun obtenerCuenta(
        @Header("Authorization") authorization: String,
        @Path("id_cuenta") idCuenta: String
    ): CuentaResponse

    // Requiere sesión activa (Authorization: Bearer <token>). Actualización
    // parcial, igual que ActualizarPerfilRequest: los campos en null se
    // omiten del JSON y el backend no los toca.
    @PATCH("api/v1/cuentas/{id_cuenta}")
    suspend fun actualizarCuenta(
        @Header("Authorization") authorization: String,
        @Path("id_cuenta") idCuenta: String,
        @Body request: ActualizarCuentaRequest
    ): CuentaResponse

    // Requiere sesión activa (Authorization: Bearer <token>). Activa o
    // desactiva una cuenta; GET /api/v1/cuentas solo devuelve las activas
    // por defecto, así que desactivar una la saca de ese listado.
    @PATCH("api/v1/cuentas/{id_cuenta}/estado")
    suspend fun cambiarEstadoCuenta(
        @Header("Authorization") authorization: String,
        @Path("id_cuenta") idCuenta: String,
        @Body request: CambiarEstadoCuentaRequest
    ): CuentaEstadoResponse
}
