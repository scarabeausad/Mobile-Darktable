package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ColorLabel
import com.example.ui.theme.DtDeepDark
import com.example.ui.theme.DtLabelBlue
import com.example.ui.theme.DtLabelGreen
import com.example.ui.theme.DtLabelPurple
import com.example.ui.theme.DtLabelRed
import com.example.ui.theme.DtLabelYellow
import com.example.ui.theme.DtThemeColors

val COLOR_LABEL_MAP = listOf(
    ColorLabel.RED to DtLabelRed,
    ColorLabel.YELLOW to DtLabelYellow,
    ColorLabel.GREEN to DtLabelGreen,
    ColorLabel.BLUE to DtLabelBlue,
    ColorLabel.PURPLE to DtLabelPurple
)

@Composable
fun ColorLabelMultiSelector(
    selectedLabels: Set<ColorLabel>,
    onToggleLabel: (ColorLabel) -> Unit,
    modifier: Modifier = Modifier,
    dotSize: Int = 18,
    spacing: Int = 6,
    showCheckMark: Boolean = false
) {
    val palette = DtThemeColors.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        COLOR_LABEL_MAP.forEach { (label, color) ->
            val isSelected = selectedLabels.contains(label)
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) palette.textPrimary else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onToggleLabel(label) }
                    .testTag("color_label_${label.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected && showCheckMark && dotSize >= 18) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (label == ColorLabel.YELLOW) DtDeepDark else Color.White,
                        modifier = Modifier.size((dotSize - 6).dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ColorLabelSelector(
    selectedLabel: ColorLabel,
    onSelectLabel: (ColorLabel) -> Unit,
    modifier: Modifier = Modifier,
    dotSize: Int = 18
) {
    ColorLabelMultiSelector(
        selectedLabels = if (selectedLabel == ColorLabel.NONE) emptySet() else setOf(selectedLabel),
        onToggleLabel = onSelectLabel,
        modifier = modifier,
        dotSize = dotSize
    )
}

@Composable
fun ColorLabelDisplayBadges(
    labels: Set<ColorLabel>,
    modifier: Modifier = Modifier,
    badgeSize: Int = 8,
    spacing: Int = 3
) {
    if (labels.isEmpty()) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        COLOR_LABEL_MAP.forEach { (label, color) ->
            if (labels.contains(label)) {
                Box(
                    modifier = Modifier
                        .size(badgeSize.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                )
            }
        }
    }
}
