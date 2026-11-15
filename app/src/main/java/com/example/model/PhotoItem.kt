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

enum class SortField(val label: String, val shortLabel: String) {
    CAPTURE_DATE("EXIF Capture Date & Time", "EXIF Date"),
    FILENAME("Filename", "Filename"),
    STAR_RATING("Star Rating", "Star Rating")
}

enum class SortDirection(val label: String, val shortLabel: String, val symbol: String) {
    ASCENDING("Ascending", "Asc", "↑"),
    DESCENDING("Descending", "Desc", "↓")
}

data class HistoryStep(
    val id: String = UUID.randomUUID().toString(),
    val moduleName: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val paramsSnapshot: EditParams
)

const val RATING_REJECTED = -1

fun PhotoItem.effectiveRating(): Int = if (isRejected) RATING_REJECTED else rating

fun PhotoItem.matchesRatingFilter(ratingFilter: Int?, comparator: RatingComparator): Boolean {
    if (ratingFilter == null) return true
    val eff = effectiveRating()
    return when (comparator) {
        RatingComparator.EXACT -> eff == ratingFilter
        RatingComparator.LESS_EQUAL -> eff <= ratingFilter
        RatingComparator.GREATER_EQUAL -> eff >= ratingFilter
    }
}

fun List<PhotoItem>.sortedByOption(
    field: SortField,
    direction: SortDirection
): List<PhotoItem> {
    val baseComparator = when (field) {
        SortField.FILENAME -> Comparator<PhotoItem> { a, b ->
            val cmp = String.CASE_INSENSITIVE_ORDER.compare(a.title, b.title)
            if (cmp != 0) cmp else a.captureDate.compareTo(b.captureDate)
        }
        SortField.CAPTURE_DATE -> Comparator<PhotoItem> { a, b ->
            val cmp = a.captureDate.compareTo(b.captureDate)
            if (cmp != 0) cmp else String.CASE_INSENSITIVE_ORDER.compare(a.title, b.title)
        }
        SortField.STAR_RATING -> Comparator<PhotoItem> { a, b ->
            val cmp = a.effectiveRating().compareTo(b.effectiveRating())
            if (cmp != 0) cmp else a.captureDate.compareTo(b.captureDate)
        }
    }
    val directedComparator = if (direction == SortDirection.DESCENDING) {
        baseComparator.reversed()
    } else {
        baseComparator
    }
    return this.sortedWith(directedComparator)
}

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
