package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarktableColorScheme = darkColorScheme(
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

@Composable
fun DarktableTheme(
    darkTheme: Boolean = true, // Darktable is inherently dark workstation styled
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DtDeepDark.toArgb()
                window.navigationBarColor = DtDeepDark.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarktableColorScheme,
        typography = Typography,
        content = content
    )
}
