package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Darktable professional neutral dark-gray workstation palette
val DtDeepDark = Color(0xFF232327)
val DtSurfaceDark = Color(0xFF2B2B30)
val DtPanelDark = Color(0xFF323238)
val DtCardDark = Color(0xFF3A3A42)
val DtElevated = Color(0xFF45454E)
val DtBorder = Color(0xFF52525E)

// Bright Mode daylight workstation palette
val DtBrightBg = Color(0xFFF3F4F8)
val DtBrightSurface = Color(0xFFFFFFFF)
val DtBrightPanel = Color(0xFFE7E9F0)
val DtBrightCard = Color(0xFFFFFFFF)
val DtBrightElevated = Color(0xFFDCE0EA)
val DtBrightBorder = Color(0xFFC5C9D6)

// Darktable classic Amber primary accent
val DtAmberPrimary = Color(0xFFE5A93B)
val DtAmberHover = Color(0xFFF7BD56)
val DtAmberDark = Color(0xFFC48B22)
val DtAmberLightMode = Color(0xFFB5770D)

// Precision Secondary / Tertiary
val DtSecondaryBlue = Color(0xFF4A9DF8)
val DtSecondaryBlueLightMode = Color(0xFF1D6FD1)
val DtTealFilmic = Color(0xFF3CD0B8)
val DtTealFilmicLightMode = Color(0xFF0E8A78)

// Text tokens
val DtTextPrimary = Color(0xFFF0F0F4)
val DtTextSecondary = Color(0xFFB6B6C2)
val DtTextTertiary = Color(0xFF8A8A98)

val DtBrightTextPrimary = Color(0xFF16171D)
val DtBrightTextSecondary = Color(0xFF4D5060)
val DtBrightTextTertiary = Color(0xFF727687)

// Darktable Color Labels (authentic 5-label system)
val DtLabelRed = Color(0xFFE53935)
val DtLabelYellow = Color(0xFFFBC02D)
val DtLabelGreen = Color(0xFF43A047)
val DtLabelBlue = Color(0xFF1E88E5)
val DtLabelPurple = Color(0xFF8E24AA)

// Histogram colors
val DtHistoRed = Color(0xFFFF5252)
val DtHistoGreen = Color(0xFF69F0AE)
val DtHistoBlue = Color(0xFF40C4FF)
val DtHistoLuma = Color(0xFFE2E2E6)
val DtHistoLumaLightMode = Color(0xFF383C4A)

@Immutable
data class DarktablePalette(
    val isDark: Boolean,
    val deepDark: Color,
    val surfaceDark: Color,
    val panelDark: Color,
    val cardDark: Color,
    val elevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentPrimary: Color,
    val accentContainer: Color,
    val onAccent: Color,
    val secondaryBlue: Color,
    val tealAccent: Color,
    val histoBg: Color,
    val histoGrid: Color,
    val histoLuma: Color
)

val DarktableDarkPalette = DarktablePalette(
    isDark = true,
    deepDark = DtDeepDark,
    surfaceDark = DtSurfaceDark,
    panelDark = DtPanelDark,
    cardDark = DtCardDark,
    elevated = DtElevated,
    border = DtBorder,
    textPrimary = DtTextPrimary,
    textSecondary = DtTextSecondary,
    textTertiary = DtTextTertiary,
    accentPrimary = DtAmberPrimary,
    accentContainer = DtAmberDark,
    onAccent = Color(0xFF1C1C20),
    secondaryBlue = DtSecondaryBlue,
    tealAccent = DtTealFilmic,
    histoBg = DtDeepDark,
    histoGrid = Color(0xFF3A3A44),
    histoLuma = DtHistoLuma
)

val DarktableLightPalette = DarktablePalette(
    isDark = false,
    deepDark = DtBrightBg,
    surfaceDark = DtBrightSurface,
    panelDark = DtBrightPanel,
    cardDark = DtBrightCard,
    elevated = DtBrightElevated,
    border = DtBrightBorder,
    textPrimary = DtBrightTextPrimary,
    textSecondary = DtBrightTextSecondary,
    textTertiary = DtBrightTextTertiary,
    accentPrimary = DtAmberLightMode,
    accentContainer = Color(0xFFF6DEB0),
    onAccent = Color.White,
    secondaryBlue = DtSecondaryBlueLightMode,
    tealAccent = DtTealFilmicLightMode,
    histoBg = Color(0xFFFAFAFC),
    histoGrid = Color(0xFFDCDFE8),
    histoLuma = DtHistoLumaLightMode
)
