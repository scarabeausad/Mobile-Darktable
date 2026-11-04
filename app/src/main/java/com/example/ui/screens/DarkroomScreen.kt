package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.engine.DarktableEngine
import com.example.model.EditParams
import com.example.ui.components.FilmicCurveView
import com.example.ui.components.HistogramView
import com.example.ui.components.ParameterSlider
import com.example.ui.components.SplitCompareView
import com.example.ui.theme.DtAmberDark
import com.example.ui.theme.DtAmberPrimary
import com.example.ui.theme.DtBorder
import com.example.ui.theme.DtCardDark
import com.example.ui.theme.DtDeepDark
import com.example.ui.theme.DtPanelDark
import com.example.ui.theme.DtSurfaceDark
import com.example.ui.theme.DtSecondaryBlue
import com.example.ui.theme.DtTealFilmic
import com.example.ui.theme.DtTextPrimary
import com.example.ui.theme.DtTextSecondary
import com.example.ui.theme.DtTextTertiary
import com.example.viewmodel.DarktableMode
import com.example.viewmodel.DarktableViewModel

enum class DarkroomTab(val title: String, val icon: ImageVector) {
    EXPOSURE("Exposure", Icons.Default.WbSunny),
    COLOR("Color & WB", Icons.Default.Palette),
    FILMIC("Filmic RGB", Icons.Default.ShowChart),
    DETAIL("Detail & FX", Icons.Default.Details),
    MONOCHROME("Monochrome", Icons.Default.FilterBAndW),
    GEOMETRY("Geometry", Icons.Default.Crop)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkroomScreen(
    viewModel: DarktableViewModel,
    modifier: Modifier = Modifier
) {
    val photo by viewModel.selectedPhoto.collectAsState()
    val params by viewModel.activeParams.collectAsState()
    val bitmap by viewModel.currentBitmap.collectAsState()
    val histogram by viewModel.currentHistogram.collectAsState()
    val isSplitCompare by viewModel.isSplitCompareActive.collectAsState()
    val isHoldOriginal by viewModel.isHoldOriginalActive.collectAsState()

    var selectedTab by remember { mutableStateOf(DarkroomTab.EXPOSURE) }
    var showHistogram by remember { mutableStateOf(true) }
    var showHistorySheet by remember { mutableStateOf(false) }

    // Canvas zoom & pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current

    BackHandler {
        viewModel.setMode(DarktableMode.LIGHTTABLE)
    }

    if (photo == null) {
        Box(
            modifier = modifier.fillMaxSize().background(DtDeepDark),
            contentAlignment = Alignment.Center
        ) {
            Text("No photo selected", color = DtTextSecondary)
        }
        return
    }

    val currentPhoto = photo!!

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DtDeepDark,
        topBar = {
            // Darkroom Top Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DtPanelDark)
                    .border(width = 0.5.dp, color = DtBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.setMode(DarktableMode.LIGHTTABLE) },
                            modifier = Modifier.testTag("btn_back_to_lighttable")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Lighttable",
                                tint = DtTextPrimary
                            )
                        }

                        Column {
                            Text(
                                text = currentPhoto.title,
                                color = DtTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentPhoto.cameraModel} • ${currentPhoto.aperture} • ISO ${currentPhoto.iso}",
                                color = DtTextTertiary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Quick Action Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Split Compare Toggle
                        IconButton(
                            onClick = { viewModel.toggleSplitCompare() },
                            modifier = Modifier.testTag("btn_toggle_split_compare")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "Toggle split before/after",
                                tint = if (isSplitCompare) DtAmberPrimary else DtTextSecondary
                            )
                        }

                        // History Stack
                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.testTag("btn_open_history")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Edit history stack",
                                tint = DtTextSecondary
                            )
                        }

                        // Presets / Styles
                        IconButton(
                            onClick = { viewModel.setMode(DarktableMode.STYLES) },
                            modifier = Modifier.testTag("btn_open_styles")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Styles & presets",
                                tint = DtAmberPrimary
                            )
                        }

                        // EXIF Inspector
                        IconButton(
                            onClick = { viewModel.setMode(DarktableMode.EXIF_INFO) },
                            modifier = Modifier.testTag("btn_open_exif")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Image information and EXIF",
                                tint = DtTextSecondary
                            )
                        }

                        // Export Button
                        IconButton(
                            onClick = { viewModel.setMode(DarktableMode.EXPORT) },
                            modifier = Modifier.testTag("btn_open_export")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Export image",
                                tint = DtSecondaryBlue
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // VIEWPORT AREA (Top Half: Image Canvas + Histogram)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.15f)
                    .background(Color.Black)
                    .clipToBounds()
            ) {
                // Image Canvas View
                @Composable
                fun RenderPhoto(showOriginal: Boolean) {
                    val colorFilter = if (showOriginal) null else DarktableEngine.createColorFilter(params)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.8f, 5.0f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale = if (scale > 1.2f) 1f else 2.5f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                )
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                                rotationZ = if (!showOriginal) params.rotationAngle else 0f
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = currentPhoto.title,
                                contentScale = ContentScale.Fit,
                                colorFilter = colorFilter,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (currentPhoto.uriString.startsWith("res://")) {
                            val resName = currentPhoto.uriString.removePrefix("res://")
                            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                            if (resId != 0) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = currentPhoto.title,
                                    contentScale = ContentScale.Fit,
                                    colorFilter = colorFilter,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            AsyncImage(
                                model = currentPhoto.uriString,
                                contentDescription = currentPhoto.title,
                                contentScale = ContentScale.Fit,
                                colorFilter = colorFilter,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (isSplitCompare) {
                    SplitCompareView(
                        beforeContent = { RenderPhoto(showOriginal = true) },
                        afterContent = { RenderPhoto(showOriginal = false) }
                    )
                } else {
                    RenderPhoto(showOriginal = isHoldOriginal)
                }

                // Top Floating Overlays: Live Histogram & Viewport Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle Histogram Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable { showHistogram = !showHistogram }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (showHistogram) "HIDE HISTOGRAM" else "SHOW HISTOGRAM",
                                color = DtAmberPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Hold to Compare Original Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isHoldOriginal) DtAmberPrimary else Color.Black.copy(alpha = 0.7f))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            viewModel.setHoldOriginal(true)
                                            tryAwaitRelease()
                                            viewModel.setHoldOriginal(false)
                                        }
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("btn_hold_original")
                        ) {
                            Text(
                                text = "HOLD FOR RAW",
                                color = if (isHoldOriginal) DtDeepDark else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (showHistogram) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HistogramView(histogram = histogram)
                    }
                }
            }

            // BOTTOM CONTROL PANELS (Module Tabs & Parameter Sliders)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
                    .background(DtPanelDark)
                    .border(width = 0.5.dp, color = DtBorder)
            ) {
                // Module Tabs Selector (Horizontal scroll)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DtSurfaceDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(DarkroomTab.values()) { tab ->
                        val isSelected = selectedTab == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) DtDeepDark else DtTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DtAmberPrimary,
                                selectedLabelColor = DtDeepDark,
                                containerColor = DtCardDark,
                                labelColor = DtTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = DtBorder,
                                selectedBorderColor = DtAmberPrimary
                            ),
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
                    }
                }

                // Active Module Parameters Content Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    when (selectedTab) {
                        DarkroomTab.EXPOSURE -> {
                            ParameterSlider(
                                label = "Exposure Compensation",
                                value = params.exposure,
                                range = -3.0f..3.0f,
                                defaultValue = 0f,
                                unit = " EV",
                                displayDecimals = 2,
                                onValueChange = {
                                    viewModel.updateParamLive(params.copy(exposure = it))
                                    viewModel.commitParamChange(params.copy(exposure = it), "Exposure", "${"%.2f".format(it)} EV")
                                }
                            )

                            ParameterSlider(
                                label = "Contrast",
                                value = params.contrast,
                                range = -100f..100f,
                                defaultValue = 0f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.updateParamLive(params.copy(contrast = it))
                                    viewModel.commitParamChange(params.copy(contrast = it), "Contrast", "${it.toInt()}%")
                                }
                            )

                            ParameterSlider(
                                label = "Highlights Recovery",
                                value = params.highlights,
                                range = -100f..100f,
                                defaultValue = 0f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.updateParamLive(params.copy(highlights = it))
                                    viewModel.commitParamChange(params.copy(highlights = it), "Highlights", "${it.toInt()}%")
                                }
                            )

                            ParameterSlider(
                                label = "Shadows Lift",
                                value = params.shadows,
                                range = -100f..100f,
                                defaultValue = 0f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.updateParamLive(params.copy(shadows = it))
                                    viewModel.commitParamChange(params.copy(shadows = it), "Shadows", "${it.toInt()}%")
                                }
                            )

                            ParameterSlider(
                                label = "Black Point Level",
                                value = params.blackPoint,
                                range = -50f..50f,
                                defaultValue = 0f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.updateParamLive(params.copy(blackPoint = it))
                                    viewModel.commitParamChange(params.copy(blackPoint = it), "Black Point", "${it.toInt()}%")
                                }
                            )
                        }

                        DarkroomTab.COLOR -> {
                            ParameterSlider(
                                label = "Color Temperature",
                                value = params.temperature,
                                range = 2500f..9500f,
                                defaultValue = 5500f,
                                unit = " K",
                                displayDecimals = 0,
                                onValueChange = {
                                    viewModel.updateParamLive(params.copy(temperature = it))
                                    viewModel.commitParamChange(params.copy(temperature = it), "White Balance", "${it.toInt()} K")
                                }
                            )

                            ParameterSlider(
                                label = "Tint (Green / Magenta)",
                                value = params.tint,
                                range = -100f..100f,
                                defaultValue = 0f,
                                unit = "",
                                onValueChange = {
                                    viewModel.updateParamLive(params.copy(tint = it))
                                    viewModel.commitParamChange(params.copy(tint = it), "Tint", "${it.toInt()}")
                                }
                            )

                            ParameterSlider(
                                label = "Vibrance (Smart Saturation)",
                                value = params.vibrance,
                                range = -100f..100f,
                                defaultValue = 0f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.updateParamLive(params.copy(vibrance = it))
                                    viewModel.commitParamChange(params.copy(vibrance = it), "Vibrance", "${it.toInt()}%")
                                }
                            )

                            ParameterSlider(
                                label = "Overall Saturation",
                                value = params.saturation,
                                range = -100f..100f,
                                defaultValue = 0f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.updateParamLive(params.copy(saturation = it))
                                    viewModel.commitParamChange(params.copy(saturation = it), "Saturation", "${it.toInt()}%")
                                }
                            )
                        }

                        DarkroomTab.FILMIC -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enable Filmic RGB Tone Mapping",
                                    color = DtTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = params.filmicEnabled,
                                    onCheckedChange = {
                                        viewModel.commitParamChange(params.copy(filmicEnabled = it), "Filmic RGB", if (it) "Enabled" else "Disabled")
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DtAmberPrimary,
                                        checkedTrackColor = DtAmberDark
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            FilmicCurveView(
                                toe = params.filmicToe,
                                shoulder = params.filmicShoulder,
                                contrast = params.filmicContrast
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            ParameterSlider(
                                label = "Toe (Shadow Roll-Off)",
                                value = params.filmicToe,
                                range = 10f..90f,
                                defaultValue = 35f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.commitParamChange(params.copy(filmicToe = it), "Filmic Toe", "${it.toInt()}%")
                                }
                            )

                            ParameterSlider(
                                label = "Shoulder (Highlight Compression)",
                                value = params.filmicShoulder,
                                range = 10f..90f,
                                defaultValue = 40f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.commitParamChange(params.copy(filmicShoulder = it), "Filmic Shoulder", "${it.toInt()}%")
                                }
                            )

                            ParameterSlider(
                                label = "Midtone Dynamic Contrast",
                                value = params.filmicContrast,
                                range = 0.6f..2.2f,
                                defaultValue = 1.15f,
                                displayDecimals = 2,
                                onValueChange = {
                                    viewModel.commitParamChange(params.copy(filmicContrast = it), "Filmic Contrast", "%.2f".format(it))
                                }
                            )
                        }

                        DarkroomTab.DETAIL -> {
                            ParameterSlider(
                                label = "Sharpness & Micro-Contrast",
                                value = params.sharpness,
                                range = 0f..100f,
                                defaultValue = 10f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.commitParamChange(params.copy(sharpness = it), "Sharpness", "${it.toInt()}%")
                                }
                            )

                            ParameterSlider(
                                label = "Film Grain Emulation",
                                value = params.grain,
                                range = 0f..100f,
                                defaultValue = 0f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.commitParamChange(params.copy(grain = it), "Grain", "${it.toInt()}%")
                                }
                            )

                            ParameterSlider(
                                label = "Vignette Amount",
                                value = params.vignette,
                                range = -100f..100f,
                                defaultValue = 0f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.commitParamChange(params.copy(vignette = it), "Vignette", "${it.toInt()}%")
                                }
                            )

                            ParameterSlider(
                                label = "Vignette Radius",
                                value = params.vignetteRadius,
                                range = 20f..100f,
                                defaultValue = 60f,
                                unit = "%",
                                onValueChange = {
                                    viewModel.commitParamChange(params.copy(vignetteRadius = it), "Vignette Radius", "${it.toInt()}%")
                                }
                            )
                        }

                        DarkroomTab.MONOCHROME -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Monochrome / B&W Conversion",
                                    color = DtTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = params.isMonochrome,
                                    onCheckedChange = {
                                        viewModel.commitParamChange(params.copy(isMonochrome = it), "Monochrome", if (it) "B&W Active" else "Color Active")
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DtAmberPrimary,
                                        checkedTrackColor = DtAmberDark
                                    )
                                )
                            }

                            if (params.isMonochrome) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("SPECTRAL SENSITIVITY CHANNEL MIXER", color = DtTextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                                ParameterSlider(
                                    label = "Red Channel Weight",
                                    value = params.redWeight,
                                    range = 0f..100f,
                                    defaultValue = 30f,
                                    unit = "%",
                                    onValueChange = {
                                        viewModel.commitParamChange(params.copy(redWeight = it), "Channel Mixer Red", "${it.toInt()}%")
                                    }
                                )

                                ParameterSlider(
                                    label = "Green Channel Weight",
                                    value = params.greenWeight,
                                    range = 0f..100f,
                                    defaultValue = 59f,
                                    unit = "%",
                                    onValueChange = {
                                        viewModel.commitParamChange(params.copy(greenWeight = it), "Channel Mixer Green", "${it.toInt()}%")
                                    }
                                )

                                ParameterSlider(
                                    label = "Blue Channel Weight",
                                    value = params.blueWeight,
                                    range = 0f..100f,
                                    defaultValue = 11f,
                                    unit = "%",
                                    onValueChange = {
                                        viewModel.commitParamChange(params.copy(blueWeight = it), "Channel Mixer Blue", "${it.toInt()}%")
                                    }
                                )
                            }
                        }

                        DarkroomTab.GEOMETRY -> {
                            ParameterSlider(
                                label = "Rotation / Straighten Angle",
                                value = params.rotationAngle,
                                range = -45f..45f,
                                defaultValue = 0f,
                                unit = "°",
                                displayDecimals = 1,
                                onValueChange = {
                                    viewModel.commitParamChange(params.copy(rotationAngle = it), "Rotate", "${"%.1f".format(it)}°")
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Flip Horizontal", color = DtTextPrimary, fontSize = 13.sp)
                                Switch(
                                    checked = params.flipHorizontal,
                                    onCheckedChange = {
                                        viewModel.commitParamChange(params.copy(flipHorizontal = it), "Flip Horizontal", "$it")
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DtAmberPrimary,
                                        checkedTrackColor = DtAmberDark
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("CROP ASPECT RATIO", color = DtTextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))

                            val ratios = listOf("Original", "1:1", "4:3", "3:2", "16:9")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ratios.forEach { ratio ->
                                    val isSelected = params.cropAspectRatio == ratio
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) DtAmberPrimary else DtCardDark)
                                            .clickable {
                                                viewModel.commitParamChange(params.copy(cropAspectRatio = ratio), "Aspect Crop", ratio)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = ratio,
                                            color = if (isSelected) DtDeepDark else DtTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    // Module Reset All button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { viewModel.resetActiveParams() },
                            modifier = Modifier.testTag("btn_reset_all_params")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset all edits", tint = DtAmberPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RESET ALL MODULES", color = DtAmberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // HISTORY STACK & SNAPSHOTS MODAL BOTTOM SHEET
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = DtPanelDark,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle(color = DtBorder) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HISTORY STACK",
                            color = DtTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${currentPhoto.history.size} non-destructive modules applied",
                            color = DtTextTertiary,
                            fontSize = 11.sp
                        )
                    }

                    // Snapshot A & B Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.saveSnapshotA() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentPhoto.snapshotA != null) DtAmberDark else DtCardDark
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("SNAP A", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.saveSnapshotB() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentPhoto.snapshotB != null) DtAmberDark else DtCardDark
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("SNAP B", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Restore snapshots if available
                if (currentPhoto.snapshotA != null || currentPhoto.snapshotB != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DtCardDark, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Snapshots:", color = DtTextSecondary, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            currentPhoto.snapshotA?.let { snapA ->
                                TextButton(onClick = { viewModel.restoreSnapshot(snapA) }) {
                                    Text("RESTORE A", color = DtAmberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            currentPhoto.snapshotB?.let { snapB ->
                                TextButton(onClick = { viewModel.restoreSnapshot(snapB) }) {
                                    Text("RESTORE B", color = DtTealFilmic, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // History Steps List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(currentPhoto.history) { index, step ->
                        val isLatest = index == currentPhoto.history.size - 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLatest) DtAmberDark.copy(alpha = 0.25f) else DtCardDark)
                                .clickable {
                                    viewModel.restoreHistoryStep(index)
                                    showHistorySheet = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = String.format("%02d", index),
                                    color = if (isLatest) DtAmberPrimary else DtTextTertiary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Column {
                                    Text(
                                        text = step.moduleName,
                                        color = if (isLatest) DtTextPrimary else DtTextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = step.description,
                                        color = DtTextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (isLatest) {
                                Box(
                                    modifier = Modifier
                                        .background(DtAmberPrimary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CURRENT",
                                        color = DtDeepDark,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
