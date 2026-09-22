package com.fintrack.app.data.local

import java.security.MessageDigest

// Convierte la contraseña a un hash SHA-256 antes de guardarla: así la base
// de datos nunca contiene la contraseña en texto plano.
fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}
