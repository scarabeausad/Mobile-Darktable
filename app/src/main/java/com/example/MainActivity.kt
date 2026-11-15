package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.PhotoItem
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
    val importMessage by viewModel.importMessage.collectAsState()
    val photoNeedingXmp by viewModel.photoNeedingXmpAttachment.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val palette = DtThemeColors.current
    var pendingAttachPhoto by remember { mutableStateOf<PhotoItem?>(null) }

    val attachXmpLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { xmpUri: Uri? ->
        val target = pendingAttachPhoto
        pendingAttachPhoto = null
        if (target != null && xmpUri != null) {
            viewModel.applyXmpToPhoto(target, xmpUri)
        }
    }

    LaunchedEffect(importMessage, photoNeedingXmp) {
        val msg = importMessage
        if (!msg.isNullOrBlank()) {
            val attachTarget = photoNeedingXmp
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (attachTarget != null) "ATTACH .XMP" else null,
                duration = if (attachTarget != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && attachTarget != null) {
                pendingAttachPhoto = attachTarget
                attachXmpLauncher.launch(arrayOf("*/*"))
            }
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(palette.deepDark)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = palette.deepDark,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
