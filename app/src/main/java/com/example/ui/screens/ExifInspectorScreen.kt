package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HistogramView
import com.example.ui.theme.DtThemeColors
import com.example.viewmodel.DarktableMode
import com.example.viewmodel.DarktableViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExifInspectorScreen(
    viewModel: DarktableViewModel,
    modifier: Modifier = Modifier
) {
    val palette = DtThemeColors.current
    val photo by viewModel.selectedPhoto.collectAsState()
    val histogram by viewModel.currentHistogram.collectAsState()
    val histogramScale by viewModel.histogramScale.collectAsState()

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
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(currentPhoto.captureDate))

    var xmpPath by remember { mutableStateOf("") }
    var xmpXml by remember { mutableStateOf("") }
    var showXmpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentPhoto.id, currentPhoto.history.size, currentPhoto.rating, currentPhoto.colorLabels) {
        val (path, xml) = viewModel.getXmpSidecarInfo(currentPhoto)
        xmpPath = path
        xmpXml = xml
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
                    onClick = { viewModel.setMode(DarktableMode.DARKROOM) },
                    modifier = Modifier.testTag("btn_back_from_exif")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                }

                Column {
                    Text(
                        text = "IMAGE INFORMATION",
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "EXIF & Optical Telemetry Inspector",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Histogram & Gamut Analysis
            Text(
                text = "TECHNICAL HISTOGRAM & SPECTRAL GAMUT (${histogramScale.label.uppercase()})",
                color = palette.textTertiary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            HistogramView(histogram = histogram, histogramScale = histogramScale)

            // Camera & Optics Card
            ExifSectionCard(
                title = "CAMERA & OPTICAL SYSTEM",
                icon = Icons.Default.CameraAlt,
                entries = listOf(
                    "Camera Body" to currentPhoto.cameraModel,
                    "Lens Specification" to currentPhoto.lens,
                    "Focal Length" to currentPhoto.focalLength,
                    "Aperture" to currentPhoto.aperture,
                    "Shutter Speed" to currentPhoto.shutterSpeed,
                    "ISO Sensitivity" to "ISO ${currentPhoto.iso}",
                    "Exposure Bias" to currentPhoto.exposureBias
                )
            )

            // File & Sensor Architecture Card
            ExifSectionCard(
                title = "SENSOR & FILE METADATA",
                icon = Icons.Default.Folder,
                entries = listOf(
                    "File Title" to currentPhoto.title,
                    "Raw / Container" to currentPhoto.fileFormat,
                    "Pixel Dimensions" to currentPhoto.dimensions,
                    "Resolution" to currentPhoto.megaPixels,
                    "Capture Timestamp" to formattedDate,
                    "Color Workspace" to currentPhoto.colorSpace
                )
            )

            // Darktable Non-Destructive Module State
            ExifSectionCard(
                title = "DARKROOM PIPELINE TELEMETRY",
                icon = Icons.Default.Speed,
                entries = listOf(
                    "Active Modules" to "${currentPhoto.history.size} steps",
                    "Star Rating" to "${currentPhoto.rating} Stars${if (currentPhoto.isRejected) " (Rejected)" else ""}",
                    "Color Labels" to if (currentPhoto.colorLabels.isEmpty()) "None" else currentPhoto.colorLabels.joinToString { it.name },
                    "Filmic S-Curve" to if (currentPhoto.params.filmicEnabled) "Active (Toe: ${currentPhoto.params.filmicToe.toInt()}%, Shoulder: ${currentPhoto.params.filmicShoulder.toInt()}%)" else "Bypassed",
                    "White Balance Temp" to "${currentPhoto.params.temperature.toInt()} Kelvin",
                    "Color Mode" to if (currentPhoto.params.isMonochrome) "Monochrome Channel Mixer" else "Full Color Gamut",
                    "Aspect Crop" to currentPhoto.params.cropAspectRatio,
                    "Vignette" to "${currentPhoto.params.vignette.toInt()}%"
                )
            )

            // XMP Sidecar Persistence Card
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "XMP SIDECAR PERSISTENCE",
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Sidecar File: ${xmpPath.substringAfterLast("/", xmpPath)}",
                        color = palette.textPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Non-destructive edits, history steps, ratings, and color labels are automatically synced to this .xmp XML sidecar.",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                    Button(
                        onClick = { showXmpDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentPrimary,
                            contentColor = palette.onAccent
                        ),
                        modifier = Modifier.testTag("btn_view_xmp_xml")
                    ) {
                        Text("VIEW XMP XML", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showXmpDialog) {
        AlertDialog(
            onDismissRequest = { showXmpDialog = false },
            containerColor = palette.panelDark,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = xmpPath.substringAfterLast("/", "sidecar.xmp"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(palette.deepDark, RoundedCornerShape(6.dp))
                        .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = xmpXml,
                        color = palette.textPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showXmpDialog = false }) {
                    Text("CLOSE", color = palette.accentPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ExifSectionCard(
    title: String,
    icon: ImageVector,
    entries: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    val palette = DtThemeColors.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, palette.border, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = palette.cardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            entries.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = value,
                        color = palette.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
