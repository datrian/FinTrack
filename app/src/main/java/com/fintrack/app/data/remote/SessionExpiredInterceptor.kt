package com.fintrack.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Detecta cuando el backend rechaza el token de sesión (401 en una petición
 * que llevaba "Authorization: Bearer <token>") y avisa con ese token.
 *
 * Las peticiones sin Authorization (p. ej. POST /api/v1/auth/login con una
 * contraseña incorrecta) también pueden responder 401, pero eso no significa
 * que la sesión venció, así que se ignoran.
 */
class SessionExpiredInterceptor(
    private val onSessionExpired: (token: String) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val token = request.header("Authorization")?.removePrefix("Bearer ")?.trim()
        if (response.code == 401 && !token.isNullOrEmpty()) {
            onSessionExpired(token)
        }
        return response
    }
}
