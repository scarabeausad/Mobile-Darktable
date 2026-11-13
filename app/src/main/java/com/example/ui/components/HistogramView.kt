package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HistogramData
import com.example.model.HistogramScale
import com.example.ui.theme.DtHistoBlue
import com.example.ui.theme.DtHistoGreen
import com.example.ui.theme.DtHistoRed
import com.example.ui.theme.DtLabelBlue
import com.example.ui.theme.DtLabelRed
import com.example.ui.theme.DtThemeColors

@Composable
fun HistogramView(
    histogram: HistogramData,
    modifier: Modifier = Modifier,
    showChannels: Boolean = true,
    histogramScale: HistogramScale = HistogramScale.LINEAR
) {
    val palette = DtThemeColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(palette.histoBg, RoundedCornerShape(8.dp))
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .padding(4.dp)
            .testTag("histogram_view_${histogramScale.name.lowercase()}")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val maxCount = if (histogram.maxBinCount > 0) histogram.maxBinCount else 1

            // Grid lines (EV stops)
            val gridColor = palette.histoGrid
            for (step in 1..4) {
                val x = width * (step / 5f)
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }
            drawLine(
                color = gridColor,
                start = Offset(0f, height * 0.5f),
                end = Offset(width, height * 0.5f),
                strokeWidth = 1f
            )

            // Draw Channels (Red, Green, Blue, Luma)
            fun drawChannelPath(bins: IntArray, color: Color, fillAlpha: Float = 0.25f) {
                if (bins.isEmpty()) return
                val path = Path()
                path.moveTo(0f, height)

                val binStep = width / 255f
                for (i in 0 until 256) {
                    val x = i * binStep
                    val normalizedY = histogramScale.normalizeBin(bins[i], maxCount)
                    val y = height - (normalizedY * height * 0.95f)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                // Stroke
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.85f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Fill under
                path.lineTo(width, height)
                path.lineTo(0f, height)
                path.close()
                drawPath(
                    path = path,
                    color = color.copy(alpha = fillAlpha)
                )
            }

            if (showChannels) {
                drawChannelPath(histogram.redChannel, DtHistoRed, 0.15f)
                drawChannelPath(histogram.greenChannel, DtHistoGreen, 0.15f)
                drawChannelPath(histogram.blueChannel, DtHistoBlue, 0.15f)
            }
            drawChannelPath(histogram.lumaChannel, palette.histoLuma, 0.20f)
        }

        // Clipping Warnings Overlays (Classic Darktable Exposure Gamut Indicators)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            if (histogram.clippedShadowsPercent > 0.05f) {
                Box(
                    modifier = Modifier
                        .background(DtLabelBlue.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "UNDER ${"%.1f".format(histogram.clippedShadowsPercent)}%",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = "0 EV (SHADOWS)",
                    color = palette.textTertiary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .background(palette.panelDark.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = histogramScale.badge,
                    color = palette.accentPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (histogram.clippedHighlightsPercent > 0.05f) {
                Box(
                    modifier = Modifier
                        .background(DtLabelRed.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "OVER ${"%.1f".format(histogram.clippedHighlightsPercent)}%",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = "255 (HIGHLIGHTS)",
                    color = palette.textTertiary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
