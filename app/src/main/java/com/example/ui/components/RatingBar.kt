package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DtAmberPrimary
import com.example.ui.theme.DtLabelRed
import com.example.ui.theme.DtTextTertiary

@Composable
fun RatingBar(
    rating: Int,
    isRejected: Boolean = false,
    onRatingChange: (Int) -> Unit,
    onRejectToggle: () -> Unit,
    modifier: Modifier = Modifier,
    starSize: Int = 20
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Reject button
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Reject photo",
            tint = if (isRejected) DtLabelRed else DtTextTertiary.copy(alpha = 0.5f),
            modifier = Modifier
                .size((starSize + 2).dp)
                .clickable { onRejectToggle() }
                .padding(2.dp)
                .testTag("btn_reject_photo")
        )

        // 1 to 5 stars
        for (i in 1..5) {
            val isFilled = !isRejected && i <= rating
            Icon(
                imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.Star,
                contentDescription = "$i stars",
                tint = if (isFilled) DtAmberPrimary else DtTextTertiary.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(starSize.dp)
                    .clickable {
                        if (rating == i) onRatingChange(0) else onRatingChange(i)
                    }
                    .padding(1.dp)
                    .testTag("star_$i")
            )
        }
    }
}
