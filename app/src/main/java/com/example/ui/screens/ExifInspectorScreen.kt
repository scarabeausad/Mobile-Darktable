package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HistogramView
import com.example.ui.theme.DtAmberPrimary
import com.example.ui.theme.DtBorder
import com.example.ui.theme.DtCardDark
import com.example.ui.theme.DtDeepDark
import com.example.ui.theme.DtPanelDark
import com.example.ui.theme.DtSecondaryBlue
import com.example.ui.theme.DtTextPrimary
import com.example.ui.theme.DtTextSecondary
import com.example.ui.theme.DtTextTertiary
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
    val photo by viewModel.selectedPhoto.collectAsState()
    val histogram by viewModel.currentHistogram.collectAsState()

    BackHandler {
        viewModel.setMode(if (photo != null) DarktableMode.DARKROOM else DarktableMode.LIGHTTABLE)
    }

    if (photo == null) {
        Box(modifier = modifier.fillMaxSize().background(DtDeepDark), contentAlignment = Alignment.Center) {
            Text("No photograph selected", color = DtTextSecondary)
        }
        return
    }

    val currentPhoto = photo!!
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(currentPhoto.captureDate))

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DtDeepDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DtPanelDark)
                    .border(width = 0.5.dp, color = DtBorder)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { viewModel.setMode(DarktableMode.DARKROOM) },
                    modifier = Modifier.testTag("btn_back_from_exif")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DtTextPrimary)
                }

                Column {
                    Text(
                        text = "IMAGE INFORMATION",
                        color = DtTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "EXIF & Optical Telemetry Inspector",
                        color = DtTextTertiary,
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
                text = "TECHNICAL HISTOGRAM & SPECTRAL GAMUT",
                color = DtTextTertiary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            HistogramView(histogram = histogram)

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

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ExifSectionCard(
    title: String,
    icon: ImageVector,
    entries: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, DtBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = DtCardDark)
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
                    tint = DtAmberPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    color = DtTextPrimary,
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
                        color = DtTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = value,
                        color = DtTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
