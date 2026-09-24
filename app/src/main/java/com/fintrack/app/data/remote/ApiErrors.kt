package com.fintrack.app.data.remote

import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException

// Traduce una respuesta de error del backend a un mensaje legible. Los
// endpoints devuelven 422 con {"detail": [{"msg": ...}, ...]} para errores de
// validación, o 400/401/409 con {"detail": "mensaje"} para casos como
// credenciales inválidas o correo ya registrado.
fun HttpException.parseApiErrorMessage(): String {
    val body = response()?.errorBody()?.string()
    if (body.isNullOrBlank()) return "Ocurrió un error (código ${code()})"
    return try {
        when (val detail = JSONObject(body).opt("detail")) {
            is String -> detail
            is JSONArray -> (0 until detail.length())
                .mapNotNull { index -> detail.optJSONObject(index)?.optString("msg") }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .ifBlank { "Datos inválidos" }
            else -> "Ocurrió un error (código ${code()})"
        }
    } catch (e: Exception) {
        "Ocurrió un error (código ${code()})"
    }
}
