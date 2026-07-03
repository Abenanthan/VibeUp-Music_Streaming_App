package com.vibeup.android.ui.theme

import androidx.compose.ui.graphics.Color

// Material baseline (keep as-is)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ── VibeUp Classic Purple brand tokens ───────────────────────────────────
// These are used by hardcoded references in HomeScreen, PlayerScreen etc.
// They always point to Classic Purple values so existing screens compile.
val PurplePrimary    = Color(0xFF8B5CF6)
val PurpleLight      = Color(0xFFAB7BFF)
val PurpleDark       = Color(0xFF6D28D9)
val BluePrimary      = Color(0xFF3B82F6)
val BlueLight        = Color(0xFF60A5FA)
val PinkAccent       = Color(0xFFEC4899)
val MagentaAccent    = Color(0xFFD946EF)
val TextPrimary      = Color(0xFFFFFFFF)
val TextSecondary    = Color(0xFFB0B0D0)

// ── Background tokens (used by Gradients.kt + ProfileScreen etc.) ────────
val DeepDark         = Color(0xFF0A0A1A)
val DarkSurface      = Color(0xFF12122A)
val DarkCard         = Color(0xFF1C1C3A)
val DarkElevated     = Color(0xFF242448)
val DarkBackground   = DeepDark
val VibeUpGreen      = PurplePrimary     // alias used in ProfileScreen

// ── Gradient tokens (used by Gradients.kt) ───────────────────────────────
val GradientStart    = Color(0xFF8B5CF6)
val GradientEnd      = Color(0xFF3B82F6)
val GradientPink     = Color(0xFFEC4899)