package com.example.model

import kotlin.math.ln

enum class AppColorScheme(val label: String, val description: String) {
    DARK("Dark Mode", "Low-glare darkroom studio palette"),
    LIGHT("Bright Mode", "High-visibility daylight workstation palette"),
    SYSTEM("System", "Follow Android system dark or bright theme")
}

enum class HistogramScale(val label: String, val badge: String, val description: String) {
    LINEAR("Linear", "LIN", "Proportional pixel counts across tonal values"),
    LOGARITHMIC("Logarithmic", "LOG", "Expands subtle shadow and highlight tail detail");

    fun normalizeBin(count: Int, maxCount: Int): Float {
        if (count <= 0 || maxCount <= 0) return 0f
        return when (this) {
            LINEAR -> (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
            LOGARITHMIC -> {
                val denom = ln(1f + maxCount.toFloat())
                if (denom <= 0f) 0f else (ln(1f + count.toFloat()) / denom).coerceIn(0f, 1f)
            }
        }
    }
}

data class AppSettings(
    val colorScheme: AppColorScheme = AppColorScheme.DARK,
    val histogramScale: HistogramScale = HistogramScale.LINEAR
)
