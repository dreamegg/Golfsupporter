package com.golfsupporter.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GreenPrimary = Color(0xFF2E7D32)
private val GreenPrimaryDark = Color(0xFF81C784)
private val Fairway = Color(0xFF66BB6A)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    secondary = Fairway,
    background = Color(0xFFF7F9F7),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = Color(0xFF003910),
    secondary = Fairway,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
)

private val AppTypography = Typography()

@Composable
fun GolfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
