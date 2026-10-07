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

data class CambiarEstadoCategoriaRequest(val es_activa_categoria: Boolean)

data class CategoriaEstadoResponse(val id_categoria: String, val es_activa_categoria: Boolean)

data class CrearSubcategoriaRequest(val nombre_subcategoria: String)

data class RenombrarSubcategoriaRequest(val nombre_subcategoria: String)

data class SubcategoriaResponse(
    val id_subcategoria: String,
    val categoria_id: String,
    val nombre_subcategoria: String,
    val es_activa_subcategoria: Boolean
)

data class SubcategoriaListaSalida(
    val subcategorias: List<SubcategoriaResponse>,
    val total: Int
)

data class CambiarEstadoSubcategoriaRequest(val es_activa_subcategoria: Boolean)

data class SubcategoriaEstadoResponse(val id_subcategoria: String, val es_activa_subcategoria: Boolean)

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

    // Requiere sesión activa (Authorization: Bearer <token>). Activa o
    // desactiva una categoría; GET /api/v1/categorias solo devuelve las
    // activas por defecto, así que desactivarla la saca de ese listado.
    @PATCH("api/v1/categorias/{id_categoria}/estado")
    suspend fun cambiarEstadoCategoria(
        @Header("Authorization") authorization: String,
        @Path("id_categoria") idCategoria: String,
        @Body request: CambiarEstadoCategoriaRequest
    ): CategoriaEstadoResponse

    // Requiere sesión activa (Authorization: Bearer <token>). Sin parámetros,
    // el backend devuelve solo las subcategorías activas (estado=ACTIVA por defecto).
    @GET("api/v1/categorias/{id_categoria}/subcategorias")
    suspend fun obtenerSubcategorias(
        @Header("Authorization") authorization: String,
        @Path("id_categoria") idCategoria: String
    ): SubcategoriaListaSalida

    @POST("api/v1/categorias/{id_categoria}/subcategorias")
    suspend fun crearSubcategoria(
        @Header("Authorization") authorization: String,
        @Path("id_categoria") idCategoria: String,
        @Body request: CrearSubcategoriaRequest
    ): SubcategoriaResponse

    // Requiere sesión activa (Authorization: Bearer <token>). Nota: a
    // diferencia de las categorías, las subcategorías viven en su propia
    // ruta de nivel superior (/api/v1/subcategorias/{id}), no anidada bajo
    // /categorias/{id}.
    @GET("api/v1/subcategorias/{id_subcategoria}")
    suspend fun obtenerSubcategoria(
        @Header("Authorization") authorization: String,
        @Path("id_subcategoria") idSubcategoria: String
    ): SubcategoriaResponse

    // Renombra la subcategoría; es el único campo editable (2-100 caracteres).
    @PATCH("api/v1/subcategorias/{id_subcategoria}")
    suspend fun renombrarSubcategoria(
        @Header("Authorization") authorization: String,
        @Path("id_subcategoria") idSubcategoria: String,
        @Body request: RenombrarSubcategoriaRequest
    ): SubcategoriaResponse

    // Requiere sesión activa (Authorization: Bearer <token>). Activa o
    // desactiva una subcategoría; GET .../subcategorias solo devuelve las
    // activas por defecto, así que desactivarla la saca de ese listado.
    @PATCH("api/v1/subcategorias/{id_subcategoria}/estado")
    suspend fun cambiarEstadoSubcategoria(
        @Header("Authorization") authorization: String,
        @Path("id_subcategoria") idSubcategoria: String,
        @Body request: CambiarEstadoSubcategoriaRequest
    ): SubcategoriaEstadoResponse
}
