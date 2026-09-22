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
 */
object ProfilePhotoStore {
    private const val FILE_NAME = "profile_photo.jpg"

    // Se incrementa cada vez que se guarda una foto nueva. Los composables que
    // muestran el avatar lo leen como key de "remember" para volver a cargar
    // el archivo desde disco cuando cambia.
    var version = mutableIntStateOf(0)
        private set

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    // Devuelve el archivo de la foto guardada, o null si el usuario todavía no eligió ninguna.
    fun photoFile(context: Context): File? {
        val f = file(context)
        return if (f.exists()) f else null
    }

    // Copia el contenido de "source" (elegido con el selector de fotos del
    // sistema) al almacenamiento privado de la app, reemplazando la foto
    // anterior si existía.
    fun savePhoto(context: Context, source: Uri) {
        context.contentResolver.openInputStream(source)?.use { input ->
            file(context).outputStream().use { output -> input.copyTo(output) }
        }
        version.value++
    }
}
