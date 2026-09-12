package com.fintrack.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Small dark-mode helper tokens kept local to the theme file.
private val DarkBackground = Color(0xFF0F1115)
private val DarkSurface = Color(0xFF1B1E24)

// Mapea la paleta de FinTrack (Color.kt) a los "roles" de color que Material 3
// espera (primary, background, surface, etc). Así, cualquier componente que use
// MaterialTheme.colorScheme.xxx automáticamente respeta los colores de la marca.
private val LightColors = lightColorScheme(
    primary = FinTrackNavy,
    onPrimary = SurfaceWhite,
    secondary = FinTrackGreen,
    onSecondary = SurfaceWhite,
    error = FinTrackRed,
    background = BackgroundGray,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundGray,
    onSurfaceVariant = TextSecondary,
    outline = OutlineGray
)

// Misma idea que LightColors, pero para modo oscuro.
private val DarkColors = darkColorScheme(
    primary = FinTrackNavy,
    onPrimary = SurfaceWhite,
    secondary = FinTrackGreen,
    onSecondary = SurfaceWhite,
    error = FinTrackRed,
    background = DarkBackground,
    onBackground = SurfaceWhite,
    surface = DarkSurface,
    onSurface = SurfaceWhite,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondary,
    outline = OutlineGray
)

// Envoltorio que aplica el tema de FinTrack (colores + tipografía) a todo lo
// que se dibuje adentro de "content". Se usa una sola vez, en MainActivity.
@Composable
fun FinTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // por defecto sigue el tema del sistema (claro/oscuro)
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FinTrackTypography,
        content = content
    )
}
