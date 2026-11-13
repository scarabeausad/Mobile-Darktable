package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ParameterSlider
import com.example.ui.theme.DtLabelGreen
import com.example.ui.theme.DtThemeColors
import com.example.viewmodel.DarktableMode
import com.example.viewmodel.DarktableViewModel

@Composable
fun ExportScreen(
    viewModel: DarktableViewModel,
    modifier: Modifier = Modifier
) {
    val palette = DtThemeColors.current
    val photo by viewModel.selectedPhoto.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportMsg by viewModel.exportMessage.collectAsState()

    var format by remember { mutableStateOf("JPEG") }
    var quality by remember { mutableIntStateOf(92) }
    var maxDimension by remember { mutableIntStateOf(0) } // 0 = Full
    var savedUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    BackHandler {
        viewModel.setMode(if (photo != null) DarktableMode.DARKROOM else DarktableMode.LIGHTTABLE)
    }

    if (photo == null) {
        Box(modifier = modifier.fillMaxSize().background(palette.deepDark), contentAlignment = Alignment.Center) {
            Text("No photograph selected", color = palette.textSecondary)
        }
        return
    }

    val currentPhoto = photo!!

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
                    onClick = { viewModel.setMode(DarktableMode.DARKROOM) },
                    modifier = Modifier.testTag("btn_back_from_export")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }

                Column {
                    Text(
                        text = "EXPORT MANAGER",
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "High-fidelity rendering of developed darkroom pipeline",
                        color = palette.textTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source summary card
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
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ACTIVE PHOTO WORKFLOW",
                        color = palette.accentPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentPhoto.title,
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentPhoto.fileFormat} • Source: ${currentPhoto.dimensions} (${currentPhoto.megaPixels})",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Pipeline: ${currentPhoto.history.size} modules active (Filmic, Color Calibration, Tone Curve)",
                        color = palette.textTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            // Output Format Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TARGET ENCODING FORMAT",
                    color = palette.textTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("JPEG", "PNG", "WEBP").forEach { fmt ->
                        val isSelected = format == fmt
                        FilterChip(
                            selected = isSelected,
                            onClick = { format = fmt },
                            label = { Text(fmt, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.accentPrimary,
                                selectedLabelColor = palette.onAccent,
                                containerColor = palette.cardDark,
                                labelColor = palette.textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = palette.border,
                                selectedBorderColor = palette.accentPrimary
                            ),
                            modifier = Modifier.testTag("chip_format_$fmt")
                        )
                    }
                }
            }

            // Quality Slider (if JPEG or WEBP)
            if (format != "PNG") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ParameterSlider(
                        label = "Compression Quality",
                        value = quality.toFloat(),
                        range = 50f..100f,
                        defaultValue = 92f,
                        unit = "%",
                        onValueChange = { quality = it.toInt() },
                        testTagKey = "slider_export_quality"
                    )
                }
            }

            // Resolution Scaling
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "MAXIMUM PIXEL DIMENSION",
                    color = palette.textTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                val dimensionOptions = listOf(
                    0 to "Original Full Res",
                    3840 to "4K UHD (3840px)",
                    1920 to "Full HD (1920px)",
                    1080 to "Web / Social (1080px)"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dimensionOptions.forEach { (dim, label) ->
                        val isSelected = maxDimension == dim
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) palette.accentContainer.copy(alpha = 0.35f) else palette.cardDark)
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    color = if (isSelected) palette.accentPrimary else palette.border,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { maxDimension = dim }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) palette.accentPrimary else palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Destination Readout
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = palette.panelDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = palette.secondaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Destination: Android Pictures/Darktable",
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Public gallery storage with full color space retention",
                            color = palette.textTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Status message
            if (exportMsg != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (savedUri != null) DtLabelGreen.copy(alpha = 0.2f) else palette.accentContainer.copy(alpha = 0.2f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (savedUri != null) DtLabelGreen else palette.accentPrimary,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = exportMsg!!,
                        color = if (savedUri != null) DtLabelGreen else palette.accentPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Primary Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        viewModel.exportPhoto(
                            format = format,
                            quality = quality,
                            maxDimension = maxDimension
                        ) { uri ->
                            savedUri = uri
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accentPrimary,
                        contentColor = palette.onAccent
                    ),
                    enabled = !isExporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_execute_export")
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = palette.onAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DEVELOPING & EXPORTING...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Export")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXPORT & SAVE TO GALLERY", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }

                if (savedUri != null) {
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (format == "PNG") "image/png" else "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, savedUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Developed Photograph"))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.secondaryBlue),
                        border = BorderStroke(1.dp, palette.secondaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_share_exported")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SHARE PHOTOGRAPH", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
