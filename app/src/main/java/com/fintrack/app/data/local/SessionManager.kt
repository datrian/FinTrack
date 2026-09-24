package com.fintrack.app.data.local

import android.content.Context

/**
 * Guarda el token de sesión devuelto por /api/v1/auth/login para que otras
 * pantallas puedan reutilizarlo en llamadas autenticadas al backend
 * (Authorization: Bearer <token>).
 */
object SessionManager {
    private const val PREFS_NAME = "session_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(context: Context, accessToken: String, userId: String, name: String, email: String) {
        prefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun getAccessToken(context: Context): String? = prefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun clearSession(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
