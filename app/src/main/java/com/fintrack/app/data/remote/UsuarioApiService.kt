package com.fintrack.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT

data class CambiarPasswordRequest(
    val password_actual: String,
    val password_nuevo: String
)

interface UsuarioApiService {
    // Requiere sesión activa (Authorization: Bearer <token>). El backend
    // responde 204 sin cuerpo si el cambio fue exitoso.
    @PUT("api/v1/usuarios/me/password")
    suspend fun cambiarPassword(
        @Header("Authorization") authorization: String,
        @Body request: CambiarPasswordRequest
    ): Response<Unit>
}
