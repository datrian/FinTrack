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

@Composable
fun FinTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FinTrackTypography,
        content = content
    )
}
