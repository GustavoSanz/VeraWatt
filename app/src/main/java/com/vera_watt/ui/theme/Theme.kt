package com.vera_watt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    secondary = SecondaryMint,
    tertiary = TertiaryWarm,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFF3F4F6),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF4B5563),
    error = DangerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenDark,
    onPrimary = Color.White,
    secondary = SecondaryMint,
    tertiary = TertiaryWarm,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF2D2D2D),
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF9CA3AF),
    error = DangerRed
)

@Composable
fun VeraWattTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}