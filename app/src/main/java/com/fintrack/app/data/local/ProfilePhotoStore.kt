package com.fintrack.app.data.local

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import java.io.File

/**
 * Guarda y expone la foto de perfil elegida por el usuario. La imagen se
 * copia al almacenamiento privado de la app (filesDir) para no depender de
 * permisos de almacenamiento ni de que el archivo/URI original siga siendo
 * accesible más adelante.
 *
 * El archivo se nombra con el id del usuario (profile_photo_<id>.jpg) en vez
 * de un nombre fijo: así, si varias cuentas inician sesión en el mismo
 * dispositivo, cada una guarda y ve su propia foto en vez de pisar la de
 * las demás.
 */
object ProfilePhotoStore {
    // Se incrementa cada vez que se guarda una foto nueva. Los composables que
    // muestran el avatar lo leen como key de "remember" para volver a cargar
    // el archivo desde disco cuando cambia.
    var version = mutableIntStateOf(0)
        private set

    private fun file(context: Context, userId: String): File = File(context.filesDir, "profile_photo_$userId.jpg")

    // Devuelve el archivo de la foto guardada para ese usuario, o null si no
    // eligió ninguna (o si todavía no se conoce el usuario actual).
    fun photoFile(context: Context, userId: String?): File? {
        if (userId == null) return null
        val f = file(context, userId)
        return if (f.exists()) f else null
    }

    // Copia el contenido de "source" (elegido con el selector de fotos del
    // sistema) al almacenamiento privado de la app, reemplazando la foto
    // anterior de ese usuario si existía.
    fun savePhoto(context: Context, userId: String, source: Uri) {
        context.contentResolver.openInputStream(source)?.use { input ->
            file(context, userId).outputStream().use { output -> input.copyTo(output) }
        }
        version.value++
    }
}
