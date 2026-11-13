package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.components.FilmicCurveView
import com.example.ui.components.HistogramView
import com.example.ui.components.ParameterSlider
import com.example.ui.components.SplitCompareView
import com.example.ui.theme.DtAmberPrimary
import com.example.ui.theme.DtThemeColors
import com.example.viewmodel.DarktableMode
import com.example.viewmodel.DarktableViewModel

enum class DarkroomTab(val title: String, val shortTitle: String, val icon: ImageVector) {
    EXPOSURE("Exposure", "Expo", Icons.Default.WbSunny),
    COLOR("Color & WB", "Color", Icons.Default.Palette),
    FILMIC("Filmic RGB", "Filmic", Icons.Default.ShowChart),
    DETAIL("Detail & FX", "Detail", Icons.Default.Details),
    MONOCHROME("Monochrome", "B&W", Icons.Default.FilterBAndW),
    GEOMETRY("Geometry", "Crop", Icons.Default.Crop)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkroomScreen(
    viewModel: DarktableViewModel,
    modifier: Modifier = Modifier
) {
    val palette = DtThemeColors.current
    val photo by viewModel.selectedPhoto.collectAsState()
    val params by viewModel.activeParams.collectAsState()
    val rawBitmap by viewModel.currentBitmap.collectAsState()
    val proxyBitmap by viewModel.proxyBitmap.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val histogram by viewModel.currentHistogram.collectAsState()
    val histogramScale by viewModel.histogramScale.collectAsState()
    val isSplitCompare by viewModel.isSplitCompareActive.collectAsState()
    val isHoldOriginal by viewModel.isHoldOriginalActive.collectAsState()
    val editPanelOnLeft by viewModel.editPanelOnLeft.collectAsState()

    var selectedTab by remember { mutableStateOf(DarkroomTab.EXPOSURE) }
    var showHistogram by remember { mutableStateOf(true) }
    var showHistorySheet by remember { mutableStateOf(false) }

    // Canvas zoom & pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current

    LaunchedEffect(selectedTab) {
        viewModel.setGeometryTabActive(selectedTab == DarkroomTab.GEOMETRY)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setGeometryTabActive(false)
        }
    }

    BackHandler {
        viewModel.setMode(DarktableMode.LIGHTTABLE)
    }

    if (photo == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(palette.deepDark),
            contentAlignment = Alignment.Center
        ) {
            Text("No photo selected", color = palette.textSecondary)
        }
        return
    }

    val currentPhoto = photo!!

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = palette.deepDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.panelDark)
                    .border(width = 0.5.dp, color = palette.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { viewModel.setMode(DarktableMode.LIGHTTABLE) },
                            modifier = Modifier.testTag("btn_back_to_lighttable")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Lighttable",
                                tint = palette.textPrimary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentPhoto.title,
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentPhoto.cameraModel} • ${currentPhoto.aperture} • ISO ${currentPhoto.iso}",
                                color = palette.textTertiary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleSplitCompare() },
                            modifier = Modifier.testTag("btn_toggle_split_compare")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "Toggle split before/after",
                                tint = if (isSplitCompare) palette.accentPrimary else palette.textSecondary
                            )
                        }

                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.testTag("btn_open_history")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Edit history stack",
                                tint = palette.textSecondary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setMode(DarktableMode.STYLES) },
                            modifier = Modifier.testTag("btn_open_styles")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Styles & presets",
                                tint = palette.accentPrimary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setMode(DarktableMode.EXIF_INFO) },
                            modifier = Modifier.testTag("btn_open_exif")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Image information and EXIF",
                                tint = palette.textSecondary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setMode(DarktableMode.EXPORT) },
                            modifier = Modifier.testTag("btn_open_export")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Export image",
                                tint = palette.secondaryBlue
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > maxHeight

            @Composable
            fun ViewportPane(paneModifier: Modifier) {
                Box(
                    modifier = paneModifier
                        .background(palette.deepDark)
                        .clipToBounds()
                ) {
                    @Composable
                    fun RenderPhoto(showOriginal: Boolean) {
                        val displayBitmap: Bitmap? = if (showOriginal) {
                            proxyBitmap ?: rawBitmap
                        } else {
                            previewBitmap ?: proxyBitmap ?: rawBitmap
                        }

                        val isCropEditing = !showOriginal && selectedTab == DarkroomTab.GEOMETRY

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (!isCropEditing) {
                                        Modifier
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
                                    } else {
                                        Modifier
                                    }
                                )
                                .graphicsLayer(
                                    scaleX = if (isCropEditing) 1f else scale,
                                    scaleY = if (isCropEditing) 1f else scale,
                                    translationX = if (isCropEditing) 0f else offsetX,
                                    translationY = if (isCropEditing) 0f else offsetY
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val boxW = constraints.maxWidth.toFloat()
                            val boxH = constraints.maxHeight.toFloat()

                            if (displayBitmap != null) {
                                Image(
                                    bitmap = displayBitmap.asImageBitmap(),
                                    contentDescription = currentPhoto.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Interactive Resizable & Movable Crop Frame Overlay
                                if (isCropEditing) {
                                    CropFrameOverlay(
                                        boxWidth = boxW,
                                        boxHeight = boxH,
                                        imageWidth = displayBitmap.width.toFloat(),
                                        imageHeight = displayBitmap.height.toFloat(),
                                        params = params,
                                        onCropChange = { newAspect, l, t, r, b ->
                                            viewModel.updateParamLive(
                                                params.copy(
                                                    cropAspectRatio = newAspect,
                                                    cropLeft = l,
                                                    cropTop = t,
                                                    cropRight = r,
                                                    cropBottom = b
                                                )
                                            )
                                        },
                                        onCropCommit = {
                                            viewModel.commitCurrentParams("Crop & Frame") {
                                                val wPct = ((it.cropRight - it.cropLeft) * 100).toInt()
                                                val hPct = ((it.cropBottom - it.cropTop) * 100).toInt()
                                                "${it.cropAspectRatio} ($wPct% × $hPct%)"
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else if (currentPhoto.uriString.startsWith("res://")) {
                                val resName = currentPhoto.uriString.removePrefix("res://")
                                val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                                if (resId != 0) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = currentPhoto.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = currentPhoto.uriString,
                                    contentDescription = currentPhoto.title,
                                    contentScale = ContentScale.Fit,
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

                    // Top Floating Overlays: Live Histogram & Hold-for-RAW
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .clickable { showHistogram = !showHistogram }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (showHistogram) "HIDE HISTO" else "HISTOGRAM",
                                    color = DtAmberPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isHoldOriginal) palette.accentPrimary else Color.Black.copy(alpha = 0.72f))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                viewModel.setHoldOriginal(true)
                                                tryAwaitRelease()
                                                viewModel.setHoldOriginal(false)
                                            }
                                        )
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("btn_hold_original")
                            ) {
                                Text(
                                    text = "HOLD RAW",
                                    color = if (isHoldOriginal) palette.onAccent else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (showHistogram) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HistogramView(histogram = histogram, histogramScale = histogramScale)
                        }
                    }

                    // Bottom Geometry Crop Dimension Badge
                    if (selectedTab == DarkroomTab.GEOMETRY && !isHoldOriginal && !isSplitCompare) {
                        val bmpW = (previewBitmap ?: proxyBitmap ?: rawBitmap)?.width?.toFloat() ?: 4f
                        val bmpH = (previewBitmap ?: proxyBitmap ?: rawBitmap)?.height?.toFloat() ?: 3f
                        val activeRect = DarktableEngine.resolveCropRect(bmpW, bmpH, params)
                        val wPct = ((activeRect[2] - activeRect[0]) * 100).toInt()
                        val hPct = ((activeRect[3] - activeRect[1]) * 100).toInt()
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, DtAmberPrimary.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CROP: ${params.cropAspectRatio.uppercase()} • $wPct%×$hPct%",
                                color = DtAmberPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            @Composable
            fun EditPanelPane(paneModifier: Modifier, isSideDocked: Boolean) {
                Column(
                    modifier = paneModifier
                        .background(palette.panelDark)
                        .border(width = 0.5.dp, color = palette.border)
                ) {
                    // Panel Header (with Left/Right Side Dock Switcher in landscape)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.surfaceDark)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedTab.title.uppercase(),
                            color = palette.accentPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isSideDocked) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.cardDark)
                                    .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                                    .clickable { viewModel.toggleEditPanelSide() }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .testTag("btn_toggle_panel_side")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Switch edit panel between left and right side",
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (editPanelOnLeft) "L→R" else "R→L",
                                        color = palette.textSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Module Selector Tabs: 2x3 grid in landscape side-panel, single 6-tab row in portrait bottom panel
                    val tabs = DarkroomTab.values()
                    if (isSideDocked) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.surfaceDark)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (rowIndex in 0..1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (colIndex in 0..2) {
                                        val tab = tabs[rowIndex * 3 + colIndex]
                                        val isSelected = selectedTab == tab
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) palette.accentPrimary else palette.cardDark)
                                                .border(
                                                    width = 0.5.dp,
                                                    color = if (isSelected) palette.accentPrimary else palette.border,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable { selectedTab = tab }
                                                .padding(vertical = 6.dp, horizontal = 2.dp)
                                                .testTag("tab_${tab.name.lowercase()}"),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = if (isSelected) palette.onAccent else palette.textSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = tab.shortTitle,
                                                color = if (isSelected) palette.onAccent else palette.textSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.surfaceDark)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tabs.forEach { tab ->
                                val isSelected = selectedTab == tab
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) palette.accentPrimary else palette.cardDark)
                                        .border(
                                            width = 0.5.dp,
                                            color = if (isSelected) palette.accentPrimary else palette.border,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedTab = tab }
                                        .padding(vertical = 5.dp, horizontal = 2.dp)
                                        .testTag("tab_${tab.name.lowercase()}"),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) palette.onAccent else palette.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = tab.shortTitle,
                                        color = if (isSelected) palette.onAccent else palette.textSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Active Module Parameters Scrollable Column
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        when (selectedTab) {
                            DarkroomTab.EXPOSURE -> {
                                ParameterSlider(
                                    label = "Exposure",
                                    value = params.exposure,
                                    range = -3.0f..3.0f,
                                    defaultValue = 0f,
                                    unit = " EV",
                                    displayDecimals = 2,
                                    testTagKey = "slider_exposure_compensation",
                                    onValueChange = { viewModel.updateParamLive(params.copy(exposure = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Exposure") { "${"%.2f".format(it.exposure)} EV" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Contrast",
                                    value = params.contrast,
                                    range = -100f..100f,
                                    defaultValue = 0f,
                                    unit = "%",
                                    onValueChange = { viewModel.updateParamLive(params.copy(contrast = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Contrast") { "${it.contrast.toInt()}%" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Highlights",
                                    value = params.highlights,
                                    range = -100f..100f,
                                    defaultValue = 0f,
                                    unit = "%",
                                    testTagKey = "slider_highlights_recovery",
                                    onValueChange = { viewModel.updateParamLive(params.copy(highlights = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Highlights") { "${it.highlights.toInt()}%" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Shadows",
                                    value = params.shadows,
                                    range = -100f..100f,
                                    defaultValue = 0f,
                                    unit = "%",
                                    testTagKey = "slider_shadows_lift",
                                    onValueChange = { viewModel.updateParamLive(params.copy(shadows = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Shadows") { "${it.shadows.toInt()}%" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Black Point",
                                    value = params.blackPoint,
                                    range = -50f..50f,
                                    defaultValue = 0f,
                                    unit = "%",
                                    testTagKey = "slider_black_point_level",
                                    onValueChange = { viewModel.updateParamLive(params.copy(blackPoint = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Black Point") { "${it.blackPoint.toInt()}%" }
                                    }
                                )
                            }

                            DarkroomTab.COLOR -> {
                                ParameterSlider(
                                    label = "Temp",
                                    value = params.temperature,
                                    range = 2500f..9500f,
                                    defaultValue = 5500f,
                                    unit = "K",
                                    displayDecimals = 0,
                                    testTagKey = "slider_color_temperature",
                                    onValueChange = { viewModel.updateParamLive(params.copy(temperature = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("White Balance") { "${it.temperature.toInt()} K" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Tint",
                                    value = params.tint,
                                    range = -100f..100f,
                                    defaultValue = 0f,
                                    unit = "",
                                    onValueChange = { viewModel.updateParamLive(params.copy(tint = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Tint") { "${it.tint.toInt()}" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Vibrance",
                                    value = params.vibrance,
                                    range = -100f..100f,
                                    defaultValue = 0f,
                                    unit = "%",
                                    onValueChange = { viewModel.updateParamLive(params.copy(vibrance = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Vibrance") { "${it.vibrance.toInt()}%" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Saturation",
                                    value = params.saturation,
                                    range = -100f..100f,
                                    defaultValue = 0f,
                                    unit = "%",
                                    onValueChange = { viewModel.updateParamLive(params.copy(saturation = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Saturation") { "${it.saturation.toInt()}%" }
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
                                        text = "Filmic RGB",
                                        color = palette.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Switch(
                                        checked = params.filmicEnabled,
                                        onCheckedChange = {
                                            viewModel.commitParamChange(
                                                params.copy(filmicEnabled = it),
                                                "Filmic RGB",
                                                if (it) "Enabled" else "Disabled"
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = palette.accentPrimary,
                                            checkedTrackColor = palette.accentContainer
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                FilmicCurveView(
                                    toe = params.filmicToe,
                                    shoulder = params.filmicShoulder,
                                    contrast = params.filmicContrast
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                ParameterSlider(
                                    label = "Shadow Toe",
                                    value = params.filmicToe,
                                    range = 10f..90f,
                                    defaultValue = 35f,
                                    unit = "%",
                                    onValueChange = { viewModel.updateParamLive(params.copy(filmicToe = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Filmic Toe") { "${it.filmicToe.toInt()}%" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Highlight Shoulder",
                                    value = params.filmicShoulder,
                                    range = 10f..90f,
                                    defaultValue = 40f,
                                    unit = "%",
                                    onValueChange = { viewModel.updateParamLive(params.copy(filmicShoulder = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Filmic Shoulder") { "${it.filmicShoulder.toInt()}%" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Dynamic Contrast",
                                    value = params.filmicContrast,
                                    range = 0.6f..2.2f,
                                    defaultValue = 1.15f,
                                    displayDecimals = 2,
                                    onValueChange = { viewModel.updateParamLive(params.copy(filmicContrast = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Filmic Contrast") { "%.2f".format(it.filmicContrast) }
                                    }
                                )
                            }

                            DarkroomTab.DETAIL -> {
                                ParameterSlider(
                                    label = "Sharpness",
                                    value = params.sharpness,
                                    range = 0f..100f,
                                    defaultValue = 10f,
                                    unit = "%",
                                    onValueChange = { viewModel.updateParamLive(params.copy(sharpness = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Sharpness") { "${it.sharpness.toInt()}%" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Film Grain",
                                    value = params.grain,
                                    range = 0f..100f,
                                    defaultValue = 0f,
                                    unit = "%",
                                    onValueChange = { viewModel.updateParamLive(params.copy(grain = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Grain") { "${it.grain.toInt()}%" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Vignette",
                                    value = params.vignette,
                                    range = -100f..100f,
                                    defaultValue = 0f,
                                    unit = "%",
                                    onValueChange = { viewModel.updateParamLive(params.copy(vignette = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Vignette") { "${it.vignette.toInt()}%" }
                                    }
                                )

                                ParameterSlider(
                                    label = "Vignette Radius",
                                    value = params.vignetteRadius,
                                    range = 20f..100f,
                                    defaultValue = 60f,
                                    unit = "%",
                                    onValueChange = { viewModel.updateParamLive(params.copy(vignetteRadius = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Vignette Radius") { "${it.vignetteRadius.toInt()}%" }
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
                                        text = "B&W Mode",
                                        color = palette.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Switch(
                                        checked = params.isMonochrome,
                                        onCheckedChange = {
                                            viewModel.commitParamChange(
                                                params.copy(isMonochrome = it),
                                                "Monochrome",
                                                if (it) "B&W Active" else "Color Active"
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = palette.accentPrimary,
                                            checkedTrackColor = palette.accentContainer
                                        )
                                    )
                                }

                                if (params.isMonochrome) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "CHANNEL MIXER",
                                        color = palette.textTertiary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    ParameterSlider(
                                        label = "Red Weight",
                                        value = params.redWeight,
                                        range = 0f..100f,
                                        defaultValue = 30f,
                                        unit = "%",
                                        onValueChange = { viewModel.updateParamLive(params.copy(redWeight = it)) },
                                        onValueChangeFinished = {
                                            viewModel.commitCurrentParams("Channel Mixer Red") { "${it.redWeight.toInt()}%" }
                                        }
                                    )

                                    ParameterSlider(
                                        label = "Green Weight",
                                        value = params.greenWeight,
                                        range = 0f..100f,
                                        defaultValue = 59f,
                                        unit = "%",
                                        onValueChange = { viewModel.updateParamLive(params.copy(greenWeight = it)) },
                                        onValueChangeFinished = {
                                            viewModel.commitCurrentParams("Channel Mixer Green") { "${it.greenWeight.toInt()}%" }
                                        }
                                    )

                                    ParameterSlider(
                                        label = "Blue Weight",
                                        value = params.blueWeight,
                                        range = 0f..100f,
                                        defaultValue = 11f,
                                        unit = "%",
                                        onValueChange = { viewModel.updateParamLive(params.copy(blueWeight = it)) },
                                        onValueChangeFinished = {
                                            viewModel.commitCurrentParams("Channel Mixer Blue") { "${it.blueWeight.toInt()}%" }
                                        }
                                    )
                                }
                            }

                            DarkroomTab.GEOMETRY -> {
                                ParameterSlider(
                                    label = "Rotate",
                                    value = params.rotationAngle,
                                    range = -45f..45f,
                                    defaultValue = 0f,
                                    unit = "°",
                                    displayDecimals = 1,
                                    onValueChange = { viewModel.updateParamLive(params.copy(rotationAngle = it)) },
                                    onValueChangeFinished = {
                                        viewModel.commitCurrentParams("Rotate") { "${"%.1f".format(it.rotationAngle)}°" }
                                    }
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Flip Horizontal", color = palette.textPrimary, fontSize = 12.sp)
                                    Switch(
                                        checked = params.flipHorizontal,
                                        onCheckedChange = {
                                            viewModel.commitParamChange(
                                                params.copy(flipHorizontal = it),
                                                "Flip Horizontal",
                                                "$it"
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = palette.accentPrimary,
                                            checkedTrackColor = palette.accentContainer
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "CROP ASPECT RATIO",
                                        color = palette.textTertiary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "RESET CROP",
                                        color = palette.accentPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { viewModel.selectCropAspectRatio("Original") }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .testTag("btn_reset_crop")
                                    )
                                }
                                Text(
                                    text = "Drag frame to move • Drag corners/edges to resize",
                                    color = palette.textTertiary,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                                )

                                val ratios = listOf("Original", "Free", "1:1", "4:3", "3:2", "16:9")
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    ratios.chunked(3).forEach { rowRatios ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            rowRatios.forEach { ratio ->
                                                val isSelected = params.cropAspectRatio == ratio
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSelected) palette.accentPrimary else palette.cardDark)
                                                        .border(
                                                            0.5.dp,
                                                            if (isSelected) palette.accentPrimary else palette.border,
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .clickable {
                                                            viewModel.selectCropAspectRatio(ratio)
                                                        }
                                                        .padding(vertical = 7.dp)
                                                        .testTag("crop_ratio_${ratio.lowercase().replace(":", "_")}"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = ratio,
                                                        color = if (isSelected) palette.onAccent else palette.textPrimary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { viewModel.resetActiveParams() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_reset_all_params")
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Reset all edits",
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "RESET ALL",
                                    color = palette.accentPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Side-by-Side Row ONLY in Landscape; Top Viewport + Bottom Edit Panel Column in Portrait
            if (isLandscape) {
                val panelWidth = (maxWidth * 0.36f).coerceIn(240.dp, 340.dp)
                Row(modifier = Modifier.fillMaxSize()) {
                    if (editPanelOnLeft) {
                        EditPanelPane(Modifier.width(panelWidth).fillMaxHeight(), isSideDocked = true)
                        ViewportPane(Modifier.weight(1f).fillMaxHeight())
                    } else {
                        ViewportPane(Modifier.weight(1f).fillMaxHeight())
                        EditPanelPane(Modifier.width(panelWidth).fillMaxHeight(), isSideDocked = true)
                    }
                }
            } else {
                val panelHeight = (maxHeight * 0.44f).coerceIn(250.dp, 360.dp)
                Column(modifier = Modifier.fillMaxSize()) {
                    ViewportPane(Modifier.weight(1f).fillMaxWidth())
                    EditPanelPane(Modifier.fillMaxWidth().height(panelHeight), isSideDocked = false)
                }
            }
        }
    }

    // HISTORY STACK & SNAPSHOTS MODAL BOTTOM SHEET
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = palette.panelDark,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle(color = palette.border) }
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
                            color = palette.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${currentPhoto.history.size} non-destructive modules applied (XMP synced)",
                            color = palette.textTertiary,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.saveSnapshotA() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentPhoto.snapshotA != null) palette.accentContainer else palette.cardDark,
                                contentColor = palette.textPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("SNAP A", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.saveSnapshotB() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentPhoto.snapshotB != null) palette.accentContainer else palette.cardDark,
                                contentColor = palette.textPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("SNAP B", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (currentPhoto.snapshotA != null || currentPhoto.snapshotB != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.cardDark, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Snapshots:", color = palette.textSecondary, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            currentPhoto.snapshotA?.let { snapA ->
                                TextButton(onClick = { viewModel.restoreSnapshot(snapA) }) {
                                    Text("RESTORE A", color = palette.accentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            currentPhoto.snapshotB?.let { snapB ->
                                TextButton(onClick = { viewModel.restoreSnapshot(snapB) }) {
                                    Text("RESTORE B", color = palette.tealAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

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
                                .background(if (isLatest) palette.accentContainer.copy(alpha = 0.25f) else palette.cardDark)
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
                                    color = if (isLatest) palette.accentPrimary else palette.textTertiary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Column {
                                    Text(
                                        text = step.moduleName,
                                        color = if (isLatest) palette.textPrimary else palette.textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = step.description,
                                        color = palette.textTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (isLatest) {
                                Box(
                                    modifier = Modifier
                                        .background(palette.accentPrimary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CURRENT",
                                        color = palette.onAccent,
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

@Composable
fun CropFrameOverlay(
    boxWidth: Float,
    boxHeight: Float,
    imageWidth: Float,
    imageHeight: Float,
    params: com.example.model.EditParams,
    onCropChange: (aspectRatio: String, left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onCropCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeHandle by remember { mutableStateOf(DarktableEngine.CropHandle.NONE) }

    val latestBoxW by rememberUpdatedState(boxWidth)
    val latestBoxH by rememberUpdatedState(boxHeight)
    val latestImgW by rememberUpdatedState(imageWidth)
    val latestImgH by rememberUpdatedState(imageHeight)
    val latestParams by rememberUpdatedState(params)
    val latestOnCropChange by rememberUpdatedState(onCropChange)
    val latestOnCropCommit by rememberUpdatedState(onCropCommit)

    val normRect = DarktableEngine.resolveCropRect(imageWidth, imageHeight, params)

    Canvas(
        modifier = modifier
            .testTag("crop_frame_overlay")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startPos ->
                        val bw = latestBoxW
                        val bh = latestBoxH
                        val iw = latestImgW
                        val ih = latestImgH
                        if (bw <= 0f || bh <= 0f || iw <= 0f || ih <= 0f) {
                            activeHandle = DarktableEngine.CropHandle.NONE
                            return@detectDragGestures
                        }

                        val fitScale = minOf(bw / iw, bh / ih)
                        val renderedW = iw * fitScale
                        val renderedH = ih * fitScale
                        val imgLeft = (bw - renderedW) / 2f
                        val imgTop = (bh - renderedH) / 2f

                        val rect = DarktableEngine.resolveCropRect(iw, ih, latestParams)
                        val cropLeft = imgLeft + rect[0] * renderedW
                        val cropTop = imgTop + rect[1] * renderedH
                        val cropRight = imgLeft + rect[2] * renderedW
                        val cropBottom = imgTop + rect[3] * renderedH

                        val cornerHitPx = 30.dp.toPx()
                        val edgeHitPx = 22.dp.toPx()

                        fun dist(ax: Float, ay: Float): Float =
                            kotlin.math.hypot(startPos.x - ax, startPos.y - ay)

                        val detected = when {
                            dist(cropLeft, cropTop) <= cornerHitPx -> DarktableEngine.CropHandle.TOP_LEFT
                            dist(cropRight, cropTop) <= cornerHitPx -> DarktableEngine.CropHandle.TOP_RIGHT
                            dist(cropLeft, cropBottom) <= cornerHitPx -> DarktableEngine.CropHandle.BOTTOM_LEFT
                            dist(cropRight, cropBottom) <= cornerHitPx -> DarktableEngine.CropHandle.BOTTOM_RIGHT
                            kotlin.math.abs(startPos.x - cropLeft) <= edgeHitPx &&
                                startPos.y in (cropTop - edgeHitPx)..(cropBottom + edgeHitPx) -> DarktableEngine.CropHandle.LEFT
                            kotlin.math.abs(startPos.x - cropRight) <= edgeHitPx &&
                                startPos.y in (cropTop - edgeHitPx)..(cropBottom + edgeHitPx) -> DarktableEngine.CropHandle.RIGHT
                            kotlin.math.abs(startPos.y - cropTop) <= edgeHitPx &&
                                startPos.x in (cropLeft - edgeHitPx)..(cropRight + edgeHitPx) -> DarktableEngine.CropHandle.TOP
                            kotlin.math.abs(startPos.y - cropBottom) <= edgeHitPx &&
                                startPos.x in (cropLeft - edgeHitPx)..(cropRight + edgeHitPx) -> DarktableEngine.CropHandle.BOTTOM
                            startPos.x in cropLeft..cropRight &&
                                startPos.y in cropTop..cropBottom -> DarktableEngine.CropHandle.MOVE
                            else -> DarktableEngine.CropHandle.NONE
                        }
                        activeHandle = detected
                    },
                    onDragEnd = {
                        if (activeHandle != DarktableEngine.CropHandle.NONE) {
                            activeHandle = DarktableEngine.CropHandle.NONE
                            latestOnCropCommit()
                        }
                    },
                    onDragCancel = {
                        if (activeHandle != DarktableEngine.CropHandle.NONE) {
                            activeHandle = DarktableEngine.CropHandle.NONE
                            latestOnCropCommit()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (activeHandle == DarktableEngine.CropHandle.NONE) return@detectDragGestures
                        change.consume()

                        val bw = latestBoxW
                        val bh = latestBoxH
                        val iw = latestImgW
                        val ih = latestImgH
                        if (bw <= 0f || bh <= 0f || iw <= 0f || ih <= 0f) return@detectDragGestures

                        val fitScale = minOf(bw / iw, bh / ih)
                        val renderedW = (iw * fitScale).coerceAtLeast(1f)
                        val renderedH = (ih * fitScale).coerceAtLeast(1f)
                        val dxNorm = dragAmount.x / renderedW
                        val dyNorm = dragAmount.y / renderedH

                        val curParams = latestParams
                        val curRect = DarktableEngine.resolveCropRect(iw, ih, curParams)

                        // If user resizes from "Original", unlock into "Free" mode automatically
                        val effectiveAspect = if (
                            activeHandle != DarktableEngine.CropHandle.MOVE &&
                            curParams.cropAspectRatio == "Original"
                        ) {
                            "Free"
                        } else {
                            curParams.cropAspectRatio
                        }

                        val nextRect = DarktableEngine.resizeCropRect(
                            rect = curRect,
                            handle = activeHandle,
                            dxNorm = dxNorm,
                            dyNorm = dyNorm,
                            imageWidth = iw,
                            imageHeight = ih,
                            cropAspectRatio = effectiveAspect
                        )

                        latestOnCropChange(
                            effectiveAspect,
                            nextRect[0],
                            nextRect[1],
                            nextRect[2],
                            nextRect[3]
                        )
                    }
                )
            }
    ) {
        if (boxWidth <= 0f || boxHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) return@Canvas

        // Compute exact ContentScale.Fit bounds of the image inside the viewport box
        val fitScale = minOf(boxWidth / imageWidth, boxHeight / imageHeight)
        val renderedW = imageWidth * fitScale
        val renderedH = imageHeight * fitScale
        val imgLeft = (boxWidth - renderedW) / 2f
        val imgTop = (boxHeight - renderedH) / 2f

        val cropLeft = imgLeft + normRect[0] * renderedW
        val cropTop = imgTop + normRect[1] * renderedH
        val cropRight = imgLeft + normRect[2] * renderedW
        val cropBottom = imgTop + normRect[3] * renderedH
        val cropW = (cropRight - cropLeft).coerceAtLeast(1f)
        val cropH = (cropBottom - cropTop).coerceAtLeast(1f)

        // Darken cropped-out regions outside the crop frame
        val maskColor = Color.Black.copy(alpha = 0.65f)
        if (cropTop > imgTop) {
            drawRect(maskColor, Offset(imgLeft, imgTop), Size(renderedW, cropTop - imgTop))
        }
        if (cropBottom < imgTop + renderedH) {
            drawRect(maskColor, Offset(imgLeft, cropBottom), Size(renderedW, (imgTop + renderedH) - cropBottom))
        }
        if (cropLeft > imgLeft) {
            drawRect(maskColor, Offset(imgLeft, cropTop), Size(cropLeft - imgLeft, cropH))
        }
        if (cropRight < imgLeft + renderedW) {
            drawRect(maskColor, Offset(cropRight, cropTop), Size((imgLeft + renderedW) - cropRight, cropH))
        }

        // Rule-of-thirds grid inside crop frame
        val gridAlpha = if (activeHandle != DarktableEngine.CropHandle.NONE) 0.55f else 0.35f
        val gridColor = Color.White.copy(alpha = gridAlpha)
        val stroke1 = 1.dp.toPx()
        for (i in 1..2) {
            val gx = cropLeft + cropW * (i / 3f)
            val gy = cropTop + cropH * (i / 3f)
            drawLine(gridColor, Offset(gx, cropTop), Offset(gx, cropBottom), strokeWidth = stroke1)
            drawLine(gridColor, Offset(cropLeft, gy), Offset(cropRight, gy), strokeWidth = stroke1)
        }

        // Crisp outer crop border
        drawRect(
            color = Color.White.copy(alpha = 0.92f),
            topLeft = Offset(cropLeft, cropTop),
            size = Size(cropW, cropH),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Bold Amber L-shaped corner brackets + grab knobs
        val bracketLen = minOf(18.dp.toPx(), cropW * 0.25f, cropH * 0.25f)
        val bracketStroke = 3.5.dp.toPx()
        val knobRadius = 5.dp.toPx()
        val activeKnobRadius = 7.dp.toPx()

        fun cornerColor(h: DarktableEngine.CropHandle) =
            if (activeHandle == h) Color.White else DtAmberPrimary

        // Top-Left
        val tlColor = cornerColor(DarktableEngine.CropHandle.TOP_LEFT)
        drawLine(tlColor, Offset(cropLeft, cropTop), Offset(cropLeft + bracketLen, cropTop), strokeWidth = bracketStroke)
        drawLine(tlColor, Offset(cropLeft, cropTop), Offset(cropLeft, cropTop + bracketLen), strokeWidth = bracketStroke)
        drawCircle(tlColor, if (activeHandle == DarktableEngine.CropHandle.TOP_LEFT) activeKnobRadius else knobRadius, Offset(cropLeft, cropTop))

        // Top-Right
        val trColor = cornerColor(DarktableEngine.CropHandle.TOP_RIGHT)
        drawLine(trColor, Offset(cropRight - bracketLen, cropTop), Offset(cropRight, cropTop), strokeWidth = bracketStroke)
        drawLine(trColor, Offset(cropRight, cropTop), Offset(cropRight, cropTop + bracketLen), strokeWidth = bracketStroke)
        drawCircle(trColor, if (activeHandle == DarktableEngine.CropHandle.TOP_RIGHT) activeKnobRadius else knobRadius, Offset(cropRight, cropTop))

        // Bottom-Left
        val blColor = cornerColor(DarktableEngine.CropHandle.BOTTOM_LEFT)
        drawLine(blColor, Offset(cropLeft, cropBottom), Offset(cropLeft + bracketLen, cropBottom), strokeWidth = bracketStroke)
        drawLine(blColor, Offset(cropLeft, cropBottom - bracketLen), Offset(cropLeft, cropBottom), strokeWidth = bracketStroke)
        drawCircle(blColor, if (activeHandle == DarktableEngine.CropHandle.BOTTOM_LEFT) activeKnobRadius else knobRadius, Offset(cropLeft, cropBottom))

        // Bottom-Right
        val brColor = cornerColor(DarktableEngine.CropHandle.BOTTOM_RIGHT)
        drawLine(brColor, Offset(cropRight - bracketLen, cropBottom), Offset(cropRight, cropBottom), strokeWidth = bracketStroke)
        drawLine(brColor, Offset(cropRight, cropBottom - bracketLen), Offset(cropRight, cropBottom), strokeWidth = bracketStroke)
        drawCircle(brColor, if (activeHandle == DarktableEngine.CropHandle.BOTTOM_RIGHT) activeKnobRadius else knobRadius, Offset(cropRight, cropBottom))

        // Mid-edge pill handles (Top, Bottom, Left, Right)
        val pillLong = minOf(22.dp.toPx(), cropW * 0.3f, cropH * 0.3f)
        val pillThick = 4.dp.toPx()
        val cornerR = CornerRadius(pillThick / 2f, pillThick / 2f)
        val midX = (cropLeft + cropRight) / 2f
        val midY = (cropTop + cropBottom) / 2f

        // Top edge pill
        drawRoundRect(
            color = cornerColor(DarktableEngine.CropHandle.TOP),
            topLeft = Offset(midX - pillLong / 2f, cropTop - pillThick / 2f),
            size = Size(pillLong, pillThick),
            cornerRadius = cornerR
        )
        // Bottom edge pill
        drawRoundRect(
            color = cornerColor(DarktableEngine.CropHandle.BOTTOM),
            topLeft = Offset(midX - pillLong / 2f, cropBottom - pillThick / 2f),
            size = Size(pillLong, pillThick),
            cornerRadius = cornerR
        )
        // Left edge pill
        drawRoundRect(
            color = cornerColor(DarktableEngine.CropHandle.LEFT),
            topLeft = Offset(cropLeft - pillThick / 2f, midY - pillLong / 2f),
            size = Size(pillThick, pillLong),
            cornerRadius = cornerR
        )
        // Right edge pill
        drawRoundRect(
            color = cornerColor(DarktableEngine.CropHandle.RIGHT),
            topLeft = Offset(cropRight - pillThick / 2f, midY - pillLong / 2f),
            size = Size(pillThick, pillLong),
            cornerRadius = cornerR
        )
    }
}
