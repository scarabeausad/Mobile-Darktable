package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DtAmberPrimary
import com.example.ui.theme.DtDeepDark

@Composable
fun SplitCompareView(
    modifier: Modifier = Modifier,
    beforeContent: @Composable () -> Unit,
    afterContent: @Composable () -> Unit
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val splitPositionX = widthPx * splitFraction

        // Full 'After' (developed) content in background
        Box(modifier = Modifier.fillMaxSize()) {
            afterContent()
        }

        // 'Before' (original) clipped to left of splitPosition
        val splitDp = with(density) { splitPositionX.toDp() }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(splitDp)
                .clipToBounds()
        ) {
            // Anchor full content inside so it aligns with background
            Box(modifier = Modifier.fillMaxSize()) {
                beforeContent()
            }
        }

        // Divider Line
        Box(
            modifier = Modifier
                .offset { IntOffset(splitPositionX.toInt() - 2, 0) }
                .fillMaxHeight()
                .width(3.dp)
                .background(DtAmberPrimary)
        )

        // Draggable Handle
        Box(
            modifier = Modifier
                .offset { IntOffset(splitPositionX.toInt() - 20, (constraints.maxHeight / 2) - 20) }
                .size(40.dp)
                .clip(CircleShape)
                .background(DtAmberPrimary)
                .pointerInput(widthPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newPos = (splitPositionX + dragAmount.x).coerceIn(20f, widthPx - 20f)
                        splitFraction = newPos / widthPx
                    }
                }
                .testTag("split_slider_handle"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                contentDescription = "Drag to compare original and edited",
                tint = DtDeepDark,
                modifier = Modifier.size(24.dp)
            )
        }

        // Labels
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "BEFORE (ORIGINAL)",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "AFTER (DARKROOM)",
                color = DtAmberPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
