package com.vibeup.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Core Colors ──
val PurplePrimary    = Color(0xFF8B5CF6)  // Vibrant Purple
val PurpleLight      = Color(0xFFAB7BFF)  // Light Purple
val PurpleDark       = Color(0xFF6D28D9)  // Dark Purple
val BluePrimary      = Color(0xFF3B82F6)  // Vibrant Blue
val BlueLight        = Color(0xFF60A5FA)  // Light Blue
val PinkAccent       = Color(0xFFEC4899)  // Pink (matches flame)
val MagentaAccent    = Color(0xFFD946EF)  // Magenta

// ── Background Colors ──
val DeepDark         = Color(0xFF0A0A1A)  // Deep dark bg
val DarkSurface      = Color(0xFF12122A)  // Dark purple surface
val DarkCard         = Color(0xFF1C1C3A)  // Card background
val DarkElevated     = Color(0xFF242448)  // Elevated surface

// ── Text Colors ──
val TextPrimary      = Color(0xFFFFFFFF)  // White
val TextSecondary    = Color(0xFFB0B0D0)  // Muted purple-white

// ── Gradient Colors ──
val GradientStart    = Color(0xFF8B5CF6)  // Purple
val GradientEnd      = Color(0xFF3B82F6)  // Blue
val GradientPink     = Color(0xFFEC4899)  // Pink

// ── Keep for compatibility ──
val DarkBackground   = DeepDark
val VibeUpGreen      = PurplePrimary

private val ColorScheme = darkColorScheme(
    primary          = PurplePrimary,
    secondary        = BluePrimary,
    tertiary         = PinkAccent,
    background       = DeepDark,
    surface          = DarkSurface,
    surfaceVariant   = DarkCard,
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error            = Color(0xFFFF5252)
)

@Composable
fun VibeUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        content = content
    )
}