package com.example.studyfocus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = CoralPrimaryContainer,
    onPrimaryContainer = Color(0xFF921029),
    secondary = CharcoalSecondary,
    onSecondary = Color.White,
    secondaryContainer = WarmSecondaryContainer,
    onSecondaryContainer = Color(0xFF2C2520),
    background = WarmIvoryBackground,
    onBackground = DarkTextPrimary,
    surface = WarmSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedTextSecondary,
    outline = SoftBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF421019),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFE5DFD9),
    onSecondary = Color(0xFF1E1D1B),
    secondaryContainer = Color(0xFF332F2C),
    onSecondaryContainer = Color(0xFFECE6E0),
    background = Color(0xFF181716),
    onBackground = Color(0xFFECE6E0),
    surface = Color(0xFF22201E),
    onSurface = Color(0xFFECE6E0),
    surfaceVariant = Color(0xFF2D2B28),
    onSurfaceVariant = Color(0xFFB5AEA5),
    outline = Color(0xFF45413D)
)

@Composable
fun StudyFocusTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
