package com.vibeup.android.ui.theme

import androidx.compose.ui.graphics.Color

enum class VibeTheme(val displayName: String, val description: String) {

    CLASSIC_PURPLE("Classic Purple", "VibeUp original. Vibrant purple gradient."),
    OBSIDIAN("Obsidian", "Linear-inspired. Cold, precise, professional."),
    AURORA("Aurora", "Darkroom-inspired. Deep teal, cinematic."),
    CRIMSON("Crimson", "Pocketcasts-inspired. Bold red, high contrast."),
    SAKURA("Sakura", "Bear-inspired. Warm pink, soft and human."),
    VOID("Void", "Vercel-inspired. Pure black, razor sharp."),
    EMBER("Ember", "Encore-inspired. Warm amber, rich and musical.")
}

data class VibeColorScheme(
    // Backgrounds
    val background: Color,
    val surface: Color,
    val card: Color,
    val elevated: Color,

    // Brand
    val primary: Color,
    val primaryLight: Color,
    val secondary: Color,
    val accent: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    // Nav bar
    val navBar: Color,

    // Gradient pair
    val gradientStart: Color,
    val gradientEnd: Color
)

object VibeThemes {

    // ── Obsidian ── Linear.app design language ────────────────────────────
    // Cold greys, electric indigo accent, surgical precision
    val Obsidian = VibeColorScheme(
        background   = Color(0xFF0F0F13),
        surface      = Color(0xFF16161D),
        card         = Color(0xFF1C1C26),
        elevated     = Color(0xFF252533),
        primary      = Color(0xFF5B5BD6),  // Linear's signature indigo
        primaryLight = Color(0xFF7878E8),
        secondary    = Color(0xFF3E3E52),
        accent       = Color(0xFF00B4D8),
        textPrimary  = Color(0xFFEEEEF6),
        textSecondary= Color(0xFF8888AA),
        textMuted    = Color(0xFF4A4A6A),
        navBar       = Color(0xFF16161D),
        gradientStart= Color(0xFF5B5BD6),
        gradientEnd  = Color(0xFF00B4D8)
    )

    // ── Aurora ── Darkroom / VSCO film aesthetic ──────────────────────────
    // Deep ocean backgrounds, muted teal primaries, subtle warmth
    val Aurora = VibeColorScheme(
        background   = Color(0xFF080E14),
        surface      = Color(0xFF0D1620),
        card         = Color(0xFF111E2A),
        elevated     = Color(0xFF172737),
        primary      = Color(0xFF00C9A7),  // Darkroom's teal
        primaryLight = Color(0xFF33D9BD),
        secondary    = Color(0xFF1A3A4A),
        accent       = Color(0xFF0096C7),
        textPrimary  = Color(0xFFE8F4F8),
        textSecondary= Color(0xFF7EA8BE),
        textMuted    = Color(0xFF3D6070),
        navBar       = Color(0xFF0A1520),
        gradientStart= Color(0xFF00C9A7),
        gradientEnd  = Color(0xFF0096C7)
    )

    // ── Crimson ── Pocketcasts red system ────────────────────────────────
    // Near-black with bold red primary, inspired by Pocketcasts' dark mode
    val Crimson = VibeColorScheme(
        background   = Color(0xFF0E0A0A),
        surface      = Color(0xFF160F0F),
        card         = Color(0xFF1E1313),
        elevated     = Color(0xFF281818),
        primary      = Color(0xFFE8251E),  // Pocketcasts' exact red
        primaryLight = Color(0xFFFF4D46),
        secondary    = Color(0xFF3D1A1A),
        accent       = Color(0xFFFF8500),
        textPrimary  = Color(0xFFF5EEEE),
        textSecondary= Color(0xFFAA8A8A),
        textMuted    = Color(0xFF5A3A3A),
        navBar       = Color(0xFF120D0D),
        gradientStart= Color(0xFFE8251E),
        gradientEnd  = Color(0xFFFF8500)
    )

    // ── Sakura ── Bear app warm tone palette ─────────────────────────────
    // Warm dark backgrounds, desaturated pinks, cream text
    val Sakura = VibeColorScheme(
        background   = Color(0xFF12090E),
        surface      = Color(0xFF1A0E15),
        card         = Color(0xFF22121B),
        elevated     = Color(0xFF2C1824),
        primary      = Color(0xFFD4688A),  // Bear's muted rose
        primaryLight = Color(0xFFE88FAA),
        secondary    = Color(0xFF3D1E2C),
        accent       = Color(0xFFE8A87C),  // warm amber complement
        textPrimary  = Color(0xFFF5EBF0),
        textSecondary= Color(0xFFB08898),
        textMuted    = Color(0xFF5A3A48),
        navBar       = Color(0xFF150B11),
        gradientStart= Color(0xFFD4688A),
        gradientEnd  = Color(0xFFE8A87C)
    )

    // ── Void ── Vercel design system ─────────────────────────────────────
    // Pure black, white accent, zero noise — Vercel's signature system
    val Void = VibeColorScheme(
        background   = Color(0xFF000000),
        surface      = Color(0xFF0A0A0A),
        card         = Color(0xFF111111),
        elevated     = Color(0xFF1A1A1A),
        primary      = Color(0xFFFFFFFF),  // Vercel's white-on-black
        primaryLight = Color(0xFFE0E0E0),
        secondary    = Color(0xFF333333),
        accent       = Color(0xFF0070F3),  // Vercel's electric blue
        textPrimary  = Color(0xFFFFFFFF),
        textSecondary= Color(0xFF888888),
        textMuted    = Color(0xFF444444),
        navBar       = Color(0xFF000000),
        gradientStart= Color(0xFF666666),
        gradientEnd  = Color(0xFF0070F3)
    )

    // ── Ember ── Spotify Encore warm variant ─────────────────────────────
    // Deep brown-blacks, amber-gold primary, rich and warm
    val Ember = VibeColorScheme(
        background   = Color(0xFF0E0A04),
        surface      = Color(0xFF160E06),
        card         = Color(0xFF1E1409),
        elevated     = Color(0xFF281C0E),
        primary      = Color(0xFFE8A020),  // warm amber gold
        primaryLight = Color(0xFFFFBB44),
        secondary    = Color(0xFF3D2A0A),
        accent       = Color(0xFFFF6B35),  // warm orange
        textPrimary  = Color(0xFFF5EDD8),
        textSecondary= Color(0xFFAA9070),
        textMuted    = Color(0xFF5A4020),
        navBar       = Color(0xFF120C05),
        gradientStart= Color(0xFFE8A020),
        gradientEnd  = Color(0xFFFF6B35)
    )

    // ── Classic Purple ── VibeUp original purple gradient ───────────────────
// Restored from the original app — deep purple-blue, vibrant and musical
    val ClassicPurple = VibeColorScheme(
        background    = Color(0xFF0A0A1A),
        surface       = Color(0xFF12122A),
        card          = Color(0xFF1C1C3A),
        elevated      = Color(0xFF242448),
        primary       = Color(0xFF8B5CF6),
        primaryLight  = Color(0xFFAB7BFF),
        secondary     = Color(0xFF3B82F6),
        accent        = Color(0xFFEC4899),
        textPrimary   = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFB0B0D0),
        textMuted     = Color(0xFF4B5563),
        navBar        = Color(0xFF12122A),
        gradientStart = Color(0xFF8B5CF6),
        gradientEnd   = Color(0xFF3B82F6)
    )

    fun get(theme: VibeTheme) = when (theme) {
        VibeTheme.CLASSIC_PURPLE -> ClassicPurple
        VibeTheme.OBSIDIAN -> Obsidian
        VibeTheme.AURORA   -> Aurora
        VibeTheme.CRIMSON  -> Crimson
        VibeTheme.SAKURA   -> Sakura
        VibeTheme.VOID     -> Void
        VibeTheme.EMBER    -> Ember
    }
}