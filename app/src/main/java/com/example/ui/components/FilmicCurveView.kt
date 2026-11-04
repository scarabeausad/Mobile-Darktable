package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DarktableEngine
import com.example.ui.theme.DtAmberPrimary
import com.example.ui.theme.DtBorder
import com.example.ui.theme.DtDeepDark
import com.example.ui.theme.DtTealFilmic

@Composable
fun FilmicCurveView(
    toe: Float,
    shoulder: Float,
    contrast: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(DtDeepDark, RoundedCornerShape(8.dp))
            .border(1.dp, DtBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Grid
            val gridColor = Color(0xFF24242E)
            for (i in 1..3) {
                val gx = width * (i / 4f)
                val gy = height * (i / 4f)
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, height), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, gy), Offset(width, gy), strokeWidth = 1f)
            }

            // 2. Linear identity diagonal (neutral reference line)
            drawLine(
                color = Color(0xFF383848),
                start = Offset(0f, height),
                end = Offset(width, 0f),
                strokeWidth = 1.dp.toPx()
            )

            // 3. Filmic S-curve path
            val path = Path()
            val steps = 60
            for (step in 0..steps) {
                val normX = step / steps.toFloat()
                val normY = DarktableEngine.evaluateFilmicCurve(normX, toe, shoulder, contrast)
                val x = normX * width
                val y = height - (normY * height)
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Glow under-layer
            drawPath(
                path = path,
                color = DtAmberPrimary.copy(alpha = 0.35f),
                style = Stroke(width = 4.dp.toPx())
            )

            // Sharp active curve
            drawPath(
                path = path,
                color = DtAmberPrimary,
                style = Stroke(width = 2.dp.toPx())
            )

            // Reference control point indicators
            val toeX = (toe / 100f) * 0.35f * width
            val toeY = height - (DarktableEngine.evaluateFilmicCurve(toeX / width, toe, shoulder, contrast) * height)
            drawCircle(DtTealFilmic, radius = 3.5.dp.toPx(), center = Offset(toeX, toeY))

            val shoulderX = width - ((shoulder / 100f) * 0.35f * width)
            val shoulderY = height - (DarktableEngine.evaluateFilmicCurve(shoulderX / width, toe, shoulder, contrast) * height)
            drawCircle(DtAmberPrimary, radius = 3.5.dp.toPx(), center = Offset(shoulderX, shoulderY))
        }

        Text(
            text = "FILMIC RGB S-CURVE",
            color = Color(0xFF888898),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
