package com.fintrack.app.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Canal entre la capa de red (que no tiene Context ni acceso a la navegación)
 * y FinTrackApp: cuando el backend rechaza un token, la red lo publica aquí y
 * la navegación cierra la sesión y manda al usuario al login.
 *
 * Es un StateFlow (y no un evento suelto) para que el aviso no se pierda si
 * llega mientras la UI todavía no está escuchando.
 */
object SessionEvents {
    private val _expiredToken = MutableStateFlow<String?>(null)

    // Token que el backend rechazó, o null si no hay nada pendiente.
    val expiredToken: StateFlow<String?> = _expiredToken.asStateFlow()

    fun notifySessionExpired(token: String) {
        _expiredToken.value = token
    }

    // La UI lo llama después de atender el aviso.
    fun consume() {
        _expiredToken.value = null
    }
}
