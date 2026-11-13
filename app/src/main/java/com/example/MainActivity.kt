package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DarkroomScreen
import com.example.ui.screens.ExifInspectorScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.LighttableScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StylesScreen
import com.example.ui.theme.DarktableTheme
import com.example.ui.theme.DtThemeColors
import com.example.viewmodel.DarktableMode
import com.example.viewmodel.DarktableViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DarktableViewModel = viewModel()
            val colorScheme by viewModel.colorScheme.collectAsState()
            DarktableTheme(appColorScheme = colorScheme) {
                DarktableApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DarktableApp(
    viewModel: DarktableViewModel,
    modifier: Modifier = Modifier
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val palette = DtThemeColors.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(palette.deepDark)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = palette.deepDark
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.deepDark)
        ) {
            AnimatedContent(
                targetState = currentMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "DarktableModeTransition"
            ) { mode ->
                when (mode) {
                    DarktableMode.LIGHTTABLE -> LighttableScreen(viewModel = viewModel)
                    DarktableMode.DARKROOM -> DarkroomScreen(viewModel = viewModel)
                    DarktableMode.STYLES -> StylesScreen(viewModel = viewModel)
                    DarktableMode.EXIF_INFO -> ExifInspectorScreen(viewModel = viewModel)
                    DarktableMode.EXPORT -> ExportScreen(viewModel = viewModel)
                    DarktableMode.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
