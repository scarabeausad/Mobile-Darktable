package com.example.model

import java.util.UUID

enum class ColorLabel {
    NONE, RED, YELLOW, GREEN, BLUE, PURPLE
}

enum class RatingComparator(val symbol: String, val label: String) {
    EXACT("=", "Exact only"),
    LESS_EQUAL("≤", "At most"),
    GREATER_EQUAL("≥", "At least")
}

enum class ColorFilterMode(val label: String, val shortLabel: String, val description: String) {
    AT_LEAST_ONE("At least one", "Any", "Matches images containing at least one selected color"),
    ONLY_SELECTED("Only selected", "Only", "Matches images having strictly only the selected colors")
}

data class HistoryStep(
    val id: String = UUID.randomUUID().toString(),
    val moduleName: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val paramsSnapshot: EditParams
)

data class PhotoItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val title: String,
    val captureDate: Long = System.currentTimeMillis(),
    val fileFormat: String = "RAW / DNG",
    val dimensions: String = "6000 x 4000",
    val megaPixels: String = "24.2 MP",
    // EXIF metadata
    val cameraModel: String = "Sony Alpha 7R V",
    val lens: String = "FE 24-70mm F2.8 GM II",
    val focalLength: String = "35mm",
    val aperture: String = "f/2.8",
    val shutterSpeed: String = "1/500s",
    val iso: Int = 100,
    val exposureBias: String = "0.0 EV",
    val colorSpace: String = "Linear Rec.2020 (Darktable Wide)",
    // Darktable ratings & organization
    val rating: Int = 0, // 0 to 5
    val isRejected: Boolean = false,
    val colorLabels: Set<ColorLabel> = emptySet(),
    val tags: List<String> = emptyList(),
    // Active adjustments
    val params: EditParams = EditParams(),
    val history: List<HistoryStep> = listOf(
        HistoryStep(
            moduleName = "Base RAW Input",
            description = "Demosaic & Linear RGB Color Profile",
            paramsSnapshot = EditParams()
        )
    ),
    // Snapshots
    val snapshotA: EditParams? = null,
    val snapshotB: EditParams? = null
) {
    // Backwards-compatible single-color accessor
    val colorLabel: ColorLabel
        get() = colorLabels.firstOrNull() ?: ColorLabel.NONE
}
