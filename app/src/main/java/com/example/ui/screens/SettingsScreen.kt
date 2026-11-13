package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppColorScheme
import com.example.model.HistogramData
import com.example.model.HistogramScale
import com.example.ui.components.HistogramView
import com.example.ui.theme.DtThemeColors
import com.example.viewmodel.DarktableViewModel
import kotlin.math.exp
import kotlin.math.pow

@Composable
fun SettingsScreen(
    viewModel: DarktableViewModel,
    modifier: Modifier = Modifier
) {
    val palette = DtThemeColors.current
    val selectedColorScheme by viewModel.colorScheme.collectAsState()
    val selectedHistogramScale by viewModel.histogramScale.collectAsState()
    val activeHistogram by viewModel.currentHistogram.collectAsState()
    val selectedPhoto by viewModel.selectedPhoto.collectAsState()

    val previewHistogram = remember(activeHistogram, selectedPhoto) {
        if (selectedPhoto != null && activeHistogram.maxBinCount > 1) {
            activeHistogram
        } else {
            createSamplePreviewHistogram()
        }
    }

    BackHandler {
        viewModel.navigateBackFromSettings()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = palette.deepDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.panelDark)
                    .border(width = 0.5.dp, color = palette.border)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { viewModel.navigateBackFromSettings() },
                    modifier = Modifier.testTag("btn_back_from_settings")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.textPrimary
                    )
                }

                Column {
                    Text(
                        text = "APPLICATION SETTINGS",
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Color Scheme & Histogram Preferences",
                        color = palette.textTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // SECTION 1: COLOR SCHEME
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "COLOR SCHEME",
                        color = palette.textTertiary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    AppColorScheme.entries.forEach { scheme ->
                        val isSelected = selectedColorScheme == scheme
                        val icon: ImageVector = when (scheme) {
                            AppColorScheme.DARK -> Icons.Default.DarkMode
                            AppColorScheme.LIGHT -> Icons.Default.LightMode
                            AppColorScheme.SYSTEM -> Icons.Default.BrightnessAuto
                        }
                        SettingOptionRow(
                            title = scheme.label,
                            description = scheme.description,
                            icon = icon,
                            isSelected = isSelected,
                            onClick = { viewModel.setColorScheme(scheme) },
                            testTag = "option_color_scheme_${scheme.name.lowercase()}"
                        )
                    }
                }

                // SECTION 2: HISTOGRAM DISPLAY SCALE
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "HISTOGRAM SCALE",
                        color = palette.textTertiary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    HistogramScale.entries.forEach { scale ->
                        val isSelected = selectedHistogramScale == scale
                        val icon: ImageVector = when (scale) {
                            HistogramScale.LINEAR -> Icons.Default.BarChart
                            HistogramScale.LOGARITHMIC -> Icons.Default.ShowChart
                        }
                        SettingOptionRow(
                            title = scale.label,
                            description = scale.description,
                            icon = icon,
                            isSelected = isSelected,
                            onClick = { viewModel.setHistogramScale(scale) },
                            testTag = "option_histogram_scale_${scale.name.lowercase()}"
                        )
                    }

                    // Live Preview Card for Histogram Scale
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, palette.border, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = palette.cardDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE HISTOGRAM PREVIEW",
                                    color = palette.textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            palette.accentContainer.copy(alpha = 0.45f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = selectedHistogramScale.label.uppercase(),
                                        color = palette.accentPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            HistogramView(
                                histogram = previewHistogram,
                                histogramScale = selectedHistogramScale
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingOptionRow(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = DtThemeColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) palette.accentContainer.copy(alpha = if (palette.isDark) 0.25f else 0.45f)
                else palette.cardDark
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) palette.accentPrimary else palette.border,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) palette.accentPrimary else palette.panelDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) palette.onAccent else palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) palette.accentPrimary else palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = palette.textSecondary,
                fontSize = 11.sp
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = palette.accentPrimary,
                modifier = Modifier.size(22.dp)
            )
        } else {
            RadioButton(
                selected = false,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    unselectedColor = palette.textTertiary
                )
            )
        }
    }
}

private fun createSamplePreviewHistogram(): HistogramData {
    val r = IntArray(256)
    val g = IntArray(256)
    val b = IntArray(256)
    val l = IntArray(256)
    var maxCount = 1
    for (i in 0 until 256) {
        val x = i / 255f
        // Midtone peak + gentle shadow and highlight tails so Linear vs Log contrast is immediately clear
        val midPeak = 950f * exp(-((x - 0.52f) / 0.11f).pow(2))
        val shadowTail = 95f * exp(-((x - 0.14f) / 0.08f).pow(2))
        val highTail = 70f * exp(-((x - 0.86f) / 0.06f).pow(2))
        val rVal = (midPeak * 0.9f + shadowTail * 0.7f + highTail * 1.4f + 12f).toInt()
        val gVal = (midPeak * 1.0f + shadowTail * 1.0f + highTail * 0.9f + 12f).toInt()
        val bVal = (midPeak * 0.85f + shadowTail * 1.3f + highTail * 0.8f + 14f).toInt()
        val lVal = (midPeak + shadowTail + highTail + 12f).toInt()
        r[i] = rVal
        g[i] = gVal
        b[i] = bVal
        l[i] = lVal
        maxCount = maxOf(maxCount, rVal, gVal, bVal, lVal)
    }
    return HistogramData(
        redChannel = r,
        greenChannel = g,
        blueChannel = b,
        lumaChannel = l,
        maxBinCount = maxCount
    )
}
