package com.fintrack.app.data.local

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.fintrack.app.data.model.UserProfile
import com.fintrack.app.data.remote.RetrofitClient

/**
 * Caché en memoria del perfil traído de GET /api/v1/usuarios/me. Inicio y Mi
 * Perfil leen de aquí en vez de datos de ejemplo (FakeData), y comparten una
 * sola llamada de red mientras dure la sesión: quien entra primero llama a
 * refresh(), y el resto reutiliza lo ya cargado.
 */
object UserProfileStore {
    var profile = mutableStateOf<UserProfile?>(null)
        private set

    suspend fun refresh(context: Context): UserProfile {
        val token = SessionManager.getAccessToken(context)
            ?: throw IllegalStateException("No hay sesión activa")
        val response = RetrofitClient.usuarioApi.obtenerMiPerfil("Bearer $token")
        val mapped = UserProfile(
            id = response.id_usuario,
            name = response.nombre_usuario,
            email = response.correo_usuario,
            currency = "MXN (\$)",
            profilePhotoPath = response.ruta_foto_perfil_local_usuario,
            budgetNotificationsEnabled = response.notificaciones_presupuesto,
            periodicNotificationsEnabled = response.notificaciones_periodicas,
            isActive = response.es_activo_usuario,
            registrationDate = response.fecha_registro_usuario,
            deactivationDate = response.fecha_desactivacion_usuario
        )
        profile.value = mapped
        return mapped
    }

    fun clear() {
        profile.value = null
    }
}
