package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DtThemeColors

@Composable
fun ParameterSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    defaultValue: Float = 0f,
    unit: String = "",
    displayDecimals: Int = 0,
    testTagKey: String = "",
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = DtThemeColors.current

    val formattedValue = when (displayDecimals) {
        0 -> "${if (value > 0 && defaultValue == 0f) "+" else ""}${value.toInt()}$unit"
        1 -> "${if (value > 0 && defaultValue == 0f) "+" else ""}${"%.1f".format(value)}$unit"
        2 -> "${if (value > 0 && defaultValue == 0f) "+" else ""}${"%.2f".format(value)}$unit"
        else -> "${value.toInt()}$unit"
    }

    val isModified = kotlin.math.abs(value - defaultValue) > 0.001f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    color = if (isModified) palette.textPrimary else palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isModified) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isModified) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(palette.accentPrimary, CircleShape)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isModified) palette.accentContainer.copy(alpha = 0.35f) else palette.cardDark,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formattedValue,
                        color = if (isModified) palette.accentPrimary else palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (isModified) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(palette.border.copy(alpha = 0.5f))
                            .clickable {
                                onValueChange(defaultValue)
                                onValueChangeFinished?.invoke()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset $label",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.accentPrimary,
                activeTrackColor = palette.accentPrimary,
                inactiveTrackColor = palette.border
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag(if (testTagKey.isNotEmpty()) testTagKey else "slider_${label.lowercase().replace(" ", "_")}")
        )
    }
}
