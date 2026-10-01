package com.fintrack.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

data class CambiarPasswordRequest(
    val password_actual: String,
    val password_nuevo: String
)

data class UsuarioPerfilResponse(
    val id_usuario: String,
    val nombre_usuario: String,
    val correo_usuario: String,
    val ruta_foto_perfil_local_usuario: String?,
    val notificaciones_presupuesto: Boolean,
    val notificaciones_periodicas: Boolean,
    val es_activo_usuario: Boolean,
    val fecha_registro_usuario: String,
    val fecha_desactivacion_usuario: String?
)

interface UsuarioApiService {
    // Requiere sesión activa (Authorization: Bearer <token>).
    @GET("api/v1/usuarios/me")
    suspend fun obtenerMiPerfil(@Header("Authorization") authorization: String): UsuarioPerfilResponse

    // Requiere sesión activa (Authorization: Bearer <token>). El backend
    // responde 204 sin cuerpo si el cambio fue exitoso.
    @PUT("api/v1/usuarios/me/password")
    suspend fun cambiarPassword(
        @Header("Authorization") authorization: String,
        @Body request: CambiarPasswordRequest
    ): Response<Unit>
}
