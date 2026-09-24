package com.fintrack.app.data.remote

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

data class RegisterRequest(
    val nombre_usuario: String,
    val correo_usuario: String,
    val password: String
)

data class RegisterResponse(
    val id_usuario: String,
    val nombre_usuario: String,
    val correo_usuario: String,
    val es_activo_usuario: Boolean,
    val fecha_registro_usuario: String
)

data class UsuarioToken(
    val id_usuario: String,
    val nombre_usuario: String,
    val correo_usuario: String,
    val ruta_foto_perfil_local_usuario: String?
)

data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val usuario: UsuarioToken
)

interface AuthApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    // El backend usa OAuth2PasswordRequestForm (form-urlencoded), no JSON:
    // "username" es el correo.
    @FormUrlEncoded
    @POST("api/v1/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse
}
