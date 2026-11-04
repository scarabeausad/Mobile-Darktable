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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DtAmberDark
import com.example.ui.theme.DtAmberPrimary
import com.example.ui.theme.DtBorder
import com.example.ui.theme.DtCardDark
import com.example.ui.theme.DtTextPrimary
import com.example.ui.theme.DtTextSecondary

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
    modifier: Modifier = Modifier
) {
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
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    color = if (isModified) DtTextPrimary else DtTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isModified) FontWeight.SemiBold else FontWeight.Normal
                )
                if (isModified) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(DtAmberPrimary, CircleShape)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isModified) DtAmberDark.copy(alpha = 0.35f) else DtCardDark,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formattedValue,
                        color = if (isModified) DtAmberPrimary else DtTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (isModified) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(DtBorder.copy(alpha = 0.5f))
                            .clickable { onValueChange(defaultValue) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset $label",
                            tint = DtTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = DtAmberPrimary,
                activeTrackColor = DtAmberPrimary,
                inactiveTrackColor = DtBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(if (testTagKey.isNotEmpty()) testTagKey else "slider_${label.lowercase().replace(" ", "_")}")
        )
    }
}
