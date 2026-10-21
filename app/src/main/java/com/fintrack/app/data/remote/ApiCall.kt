package com.fintrack.app.data.remote

import android.content.Context
import com.fintrack.app.data.local.SessionManager
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

const val SESSION_EXPIRED_MESSAGE = "Tu sesión expiró. Vuelve a iniciar sesión."

// Hace una llamada autenticada al backend y convierte los errores en un
// mensaje para la UI (vía onError), devolviendo null si falló. Una respuesta
// con un formato inesperado (RuntimeException de Gson) también se reporta
// como error en vez de cerrar la app.
suspend fun <T> runAuthenticatedCall(
    context: Context,
    onError: (String) -> Unit,
    block: suspend (authorization: String) -> T
): T? {
    val token = SessionManager.getAccessToken(context)
    if (token == null) {
        onError(SESSION_EXPIRED_MESSAGE)
        return null
    }
    return try {
        block("Bearer $token")
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        onError(if (e.code() == 401) SESSION_EXPIRED_MESSAGE else e.parseApiErrorMessage())
        null
    } catch (e: IOException) {
        onError("No se pudo conectar con el servidor. Revisa tu conexión.")
        null
    } catch (e: RuntimeException) {
        onError("El servidor respondió algo inesperado. Intenta de nuevo más tarde.")
        null
    }
}
