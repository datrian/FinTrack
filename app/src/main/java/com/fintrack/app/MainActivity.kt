package com.fintrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fintrack.app.navigation.FinTrackApp
import com.fintrack.app.ui.theme.FinTrackTheme

// Punto de entrada de la app: es la "Activity" que Android abre cuando el
// usuario toca el ícono de la app. Una app Compose normalmente tiene una
// sola Activity, y toda la UI (pantallas, navegación) vive dentro de ella.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Permite que la UI se dibuje detrás de la barra de estado/navegación
        // del sistema (pantalla completa "edge to edge").
        enableEdgeToEdge()
        // setContent reemplaza los layouts XML tradicionales de Android:
        // acá adentro se declara toda la UI usando Compose.
        setContent {
            FinTrackRoot()
        }
    }
}

// Composable raíz de toda la app. Envuelve todo con el tema visual
// (colores, tipografías) y luego dibuja el grafo de navegación (FinTrackApp),
// que es quien decide qué pantalla mostrar en cada momento.
@Composable
private fun FinTrackRoot() {
    FinTrackTheme {
        // Surface pinta el fondo con el color del tema y ocupa toda la pantalla.
        Surface(modifier = Modifier.fillMaxSize()) {
            FinTrackApp()
        }
    }
}
