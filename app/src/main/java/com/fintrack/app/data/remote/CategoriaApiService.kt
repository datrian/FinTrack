package com.fintrack.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class CrearCategoriaRequest(val nombre_categoria: String)

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
}
