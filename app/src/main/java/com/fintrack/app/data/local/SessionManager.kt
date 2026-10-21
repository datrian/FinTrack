package com.fintrack.app.data.local

import android.content.Context
import android.util.Base64
import org.json.JSONObject

/**
 * Guarda el token de sesión devuelto por /api/v1/auth/login para que otras
 * pantallas puedan reutilizarlo en llamadas autenticadas al backend
 * (Authorization: Bearer <token>).
 *
 * Si el usuario marca "Recordar mis datos y mantener sesión" al iniciar
 * sesión, la sesión sobrevive a cerrar la app (y el correo queda guardado
 * para precargarlo en el login). Si no lo marca, la sesión solo dura
 * mientras la app está abierta: se borra en el próximo arranque.
 */
object SessionManager {
    private const val PREFS_NAME = "session_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_REMEMBERED_EMAIL = "remembered_email"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(
        context: Context,
        accessToken: String,
        userId: String,
        name: String,
        email: String,
        rememberMe: Boolean
    ) {
        val editor = prefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putBoolean(KEY_REMEMBER_ME, rememberMe)
        if (rememberMe) {
            editor.putString(KEY_REMEMBERED_EMAIL, email)
        } else {
            editor.remove(KEY_REMEMBERED_EMAIL)
        }
        editor.apply()
    }

    fun getAccessToken(context: Context): String? = prefs(context).getString(KEY_ACCESS_TOKEN, null)

    // Correo del último login con "Recordar mis datos" marcado (para precargar el campo).
    fun getRememberedEmail(context: Context): String? = prefs(context).getString(KEY_REMEMBERED_EMAIL, null)

    /**
     * Se llama una sola vez al arrancar la app. Devuelve true si hay una sesión
     * recordada con un token que todavía no venció, para saltar directo a Home.
     * Si no, borra la sesión que haya (no recordada o vencida) y devuelve false.
     */
    fun restoreSessionOnStartup(context: Context): Boolean {
        val prefs = prefs(context)
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false)
        if (token != null && rememberMe && !isTokenExpired(token)) return true
        clearSession(context)
        return false
    }

    // Cierra sesión: borra el token y los datos del usuario, pero conserva el
    // correo recordado para que el login lo siga mostrando precargado.
    fun clearSession(context: Context) {
        val rememberedEmail = getRememberedEmail(context)
        val editor = prefs(context).edit().clear()
        if (rememberedEmail != null) editor.putString(KEY_REMEMBERED_EMAIL, rememberedEmail)
        editor.apply()
    }

    /**
     * Lee el campo "exp" (segundos Unix) del payload del JWT, sin verificar la
     * firma: eso lo hace el backend en cada petición. Si el token no se puede
     * leer se considera vencido. Si no trae "exp", se considera vigente y el
     * backend responderá 401 cuando deje de serlo.
     */
    private fun isTokenExpired(token: String): Boolean = try {
        val payload = token.split(".")[1]
        val json = JSONObject(String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)))
        json.has("exp") && json.getLong("exp") * 1000 <= System.currentTimeMillis()
    } catch (e: Exception) {
        true
    }
}
