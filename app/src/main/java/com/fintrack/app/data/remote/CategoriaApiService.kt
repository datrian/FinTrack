package com.fintrack.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class CrearCategoriaRequest(val nombre_categoria: String)

data class RenombrarCategoriaRequest(val nombre_categoria: String)

data class CategoriaResponse(
    val id_categoria: String,
    val nombre_categoria: String,
    val es_activa_categoria: Boolean
)

data class CategoriaListaSalida(
    val categorias: List<CategoriaResponse>,
    val total: Int
)

interface CategoriaApiService {
    // Requiere sesión activa (Authorization: Bearer <token>). Sin parámetros,
    // el backend devuelve solo las categorías activas (estado=ACTIVA por defecto).
    @GET("api/v1/categorias")
    suspend fun obtenerCategorias(@Header("Authorization") authorization: String): CategoriaListaSalida

    @POST("api/v1/categorias")
    suspend fun crearCategoria(
        @Header("Authorization") authorization: String,
        @Body request: CrearCategoriaRequest
    ): CategoriaResponse

    // Requiere sesión activa (Authorization: Bearer <token>).
    @GET("api/v1/categorias/{id_categoria}")
    suspend fun obtenerCategoria(
        @Header("Authorization") authorization: String,
        @Path("id_categoria") idCategoria: String
    ): CategoriaResponse

    // Requiere sesión activa (Authorization: Bearer <token>). Renombra la
    // categoría; es el único campo editable (2-100 caracteres).
    @PATCH("api/v1/categorias/{id_categoria}")
    suspend fun renombrarCategoria(
        @Header("Authorization") authorization: String,
        @Path("id_categoria") idCategoria: String,
        @Body request: RenombrarCategoriaRequest
    ): CategoriaResponse
}
