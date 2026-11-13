package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.AppColorScheme

val LocalDarktablePalette = staticCompositionLocalOf { DarktableDarkPalette }

object DtThemeColors {
    val current: DarktablePalette
        @Composable
        @ReadOnlyComposable
        get() = LocalDarktablePalette.current
}

private val DarktableDarkColorScheme = darkColorScheme(
    primary = DtAmberPrimary,
    onPrimary = DtDeepDark,
    primaryContainer = DtAmberDark,
    onPrimaryContainer = DtAmberHover,
    secondary = DtSecondaryBlue,
    onSecondary = DtDeepDark,
    secondaryContainer = DtPanelDark,
    onSecondaryContainer = DtSecondaryBlue,
    tertiary = DtTealFilmic,
    onTertiary = DtDeepDark,
    background = DtDeepDark,
    onBackground = DtTextPrimary,
    surface = DtSurfaceDark,
    onSurface = DtTextPrimary,
    surfaceVariant = DtPanelDark,
    onSurfaceVariant = DtTextSecondary,
    surfaceContainer = DtPanelDark,
    surfaceContainerHigh = DtCardDark,
    surfaceContainerHighest = DtElevated,
    outline = DtBorder,
    outlineVariant = DtCardDark
)

private val DarktableLightColorScheme = lightColorScheme(
    primary = DtAmberLightMode,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6DEB0),
    onPrimaryContainer = Color(0xFF4F3100),
    secondary = DtSecondaryBlueLightMode,
    onSecondary = Color.White,
    secondaryContainer = DtBrightPanel,
    onSecondaryContainer = DtSecondaryBlueLightMode,
    tertiary = DtTealFilmicLightMode,
    onTertiary = Color.White,
    background = DtBrightBg,
    onBackground = DtBrightTextPrimary,
    surface = DtBrightSurface,
    onSurface = DtBrightTextPrimary,
    surfaceVariant = DtBrightPanel,
    onSurfaceVariant = DtBrightTextSecondary,
    surfaceContainer = DtBrightPanel,
    surfaceContainerHigh = DtBrightCard,
    surfaceContainerHighest = DtBrightElevated,
    outline = DtBrightBorder,
    outlineVariant = DtBrightElevated
)

@Composable
fun DarktableTheme(
    appColorScheme: AppColorScheme = AppColorScheme.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (appColorScheme) {
        AppColorScheme.DARK -> true
        AppColorScheme.LIGHT -> false
        AppColorScheme.SYSTEM -> isSystemInDarkTheme()
    }

    val palette = if (useDarkTheme) DarktableDarkPalette else DarktableLightPalette
    val materialScheme = if (useDarkTheme) DarktableDarkColorScheme else DarktableLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = palette.deepDark.toArgb()
                window.navigationBarColor = palette.deepDark.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDarkTheme
                controller.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalDarktablePalette provides palette) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography,
            content = content
        )
    }
}
