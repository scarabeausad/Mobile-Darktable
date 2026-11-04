package com.example.repository

import com.example.db.DarktableDao
import com.example.db.PhotoEntity
import com.example.db.StyleEntity
import com.example.model.ColorLabel
import com.example.model.DarktableStyle
import com.example.model.EditParams
import com.example.model.HistoryStep
import com.example.model.PhotoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class PhotoRepository(private val dao: DarktableDao) {

    val allPhotos: Flow<List<PhotoItem>> = dao.getAllPhotos().map { entities ->
        entities.map { it.toPhotoItem() }
    }

    val allStyles: Flow<List<DarktableStyle>> = dao.getAllStyles().map { entities ->
        entities.map { it.toDarktableStyle() }
    }

    suspend fun getPhotoById(id: String): PhotoItem? {
        return dao.getPhotoById(id)?.toPhotoItem()
    }

    suspend fun ensureInitialDataSeeded() {
        // Seed default styles if empty
        val defaultStyles = DarktableStyle.BUILT_IN_STYLES.map { StyleEntity.fromDarktableStyle(it) }
        dao.insertStyles(defaultStyles)

        // Seed default sample photos if empty
        val sampleLake = PhotoItem(
            id = "sample_lake_01",
            uriString = "res://sample_mountain_lake",
            title = "Alpine Reflection at Dawn",
            fileFormat = "Sony RAW (.ARW)",
            dimensions = "9504 x 6336",
            megaPixels = "61.0 MP",
            cameraModel = "Sony Alpha 7R V",
            lens = "FE 16-35mm F2.8 GM II",
            focalLength = "24mm",
            aperture = "f/8.0",
            shutterSpeed = "1/80s",
            iso = 100,
            exposureBias = "+0.3 EV",
            colorSpace = "Linear Rec.2020",
            rating = 5,
            colorLabels = setOf(ColorLabel.BLUE),
            tags = listOf("Landscape", "Golden Hour", "Mountains", "Lake"),
            params = EditParams(
                exposure = 0.15f,
                contrast = 12f,
                highlights = -15f,
                shadows = 20f,
                filmicToe = 35f,
                filmicShoulder = 40f,
                vibrance = 15f
            )
        )

        val sampleStreet = PhotoItem(
            id = "sample_street_02",
            uriString = "res://sample_neon_street",
            title = "Tokyo Rain Neo-Shinjuku",
            fileFormat = "Leica RAW (.DNG)",
            dimensions = "9528 x 6328",
            megaPixels = "60.3 MP",
            cameraModel = "Leica M11 Monochrome",
            lens = "Summilux-M 35mm f/1.4 ASPH",
            focalLength = "35mm",
            aperture = "f/1.4",
            shutterSpeed = "1/250s",
            iso = 800,
            exposureBias = "-0.7 EV",
            colorSpace = "Linear Rec.2020",
            rating = 4,
            colorLabels = setOf(ColorLabel.PURPLE, ColorLabel.BLUE),
            tags = listOf("Street", "Night", "Neon", "Rain"),
            params = EditParams(
                exposure = -0.1f,
                contrast = 22f,
                temperature = 4800f,
                tint = 10f,
                saturation = 18f,
                vignette = -20f
            )
        )

        val samplePortrait = PhotoItem(
            id = "sample_portrait_03",
            uriString = "res://sample_portrait",
            title = "Editorial Natural Light Study",
            fileFormat = "Hasselblad (.3FR)",
            dimensions = "11656 x 8742",
            megaPixels = "100 MP",
            cameraModel = "Hasselblad X2D 100C",
            lens = "XCD 80mm f/1.9",
            focalLength = "80mm",
            aperture = "f/2.0",
            shutterSpeed = "1/500s",
            iso = 64,
            exposureBias = "+0.0 EV",
            colorSpace = "Hasselblad Natural Color",
            rating = 3,
            colorLabels = setOf(ColorLabel.YELLOW),
            tags = listOf("Portrait", "Studio", "Editorial", "Natural Light"),
            params = EditParams(
                exposure = 0.1f,
                contrast = -5f,
                highlights = -10f,
                shadows = 10f,
                temperature = 5600f,
                vibrance = 8f
            )
        )

        val sampleDesert = PhotoItem(
            id = "sample_desert_04",
            uriString = "res://sample_desert_dunes",
            title = "Erg Chebbi Ridge Geometry",
            fileFormat = "Canon RAW (.CR3)",
            dimensions = "8192 x 5464",
            megaPixels = "45.0 MP",
            cameraModel = "Canon EOS R5",
            lens = "RF 70-200mm F2.8L IS USM",
            focalLength = "135mm",
            aperture = "f/11.0",
            shutterSpeed = "1/1000s",
            iso = 200,
            exposureBias = "-0.3 EV",
            colorSpace = "Linear Rec.2020",
            rating = 3,
            colorLabels = setOf(ColorLabel.RED, ColorLabel.YELLOW),
            tags = listOf("Desert", "Minimalist", "Fine Art", "Texture"),
            params = EditParams(
                exposure = 0.05f,
                contrast = 25f,
                temperature = 6200f,
                highlights = -20f,
                shadows = 15f,
                vibrance = 20f
            )
        )

        dao.insertPhotos(
            listOf(sampleLake, sampleStreet, samplePortrait, sampleDesert).map {
                PhotoEntity.fromPhotoItem(it)
            }
        )
    }

    suspend fun updateParams(
        photo: PhotoItem,
        newParams: EditParams,
        moduleName: String,
        description: String
    ) {
        val updatedHistory = photo.history.toMutableList()
        updatedHistory.add(
            HistoryStep(
                moduleName = moduleName,
                description = description,
                paramsSnapshot = newParams
            )
        )
        val updated = photo.copy(
            params = newParams,
            history = updatedHistory
        )
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
    }

    suspend fun setParamsDirect(photo: PhotoItem, newParams: EditParams) {
        val updated = photo.copy(params = newParams)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
    }

    suspend fun updateRating(photo: PhotoItem, newRating: Int) {
        val updated = photo.copy(rating = newRating)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
    }

    suspend fun updateColorLabel(photo: PhotoItem, label: ColorLabel) {
        val updatedLabels = if (label == ColorLabel.NONE) {
            emptySet()
        } else if (photo.colorLabels.contains(label)) {
            photo.colorLabels - label
        } else {
            photo.colorLabels + label
        }
        val updated = photo.copy(colorLabels = updatedLabels)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
    }

    suspend fun toggleColorLabel(photo: PhotoItem, label: ColorLabel) {
        updateColorLabel(photo, label)
    }

    suspend fun updateColorLabels(photo: PhotoItem, labels: Set<ColorLabel>) {
        val updated = photo.copy(colorLabels = labels)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
    }

    suspend fun toggleReject(photo: PhotoItem) {
        val updated = photo.copy(isRejected = !photo.isRejected)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
    }

    suspend fun saveSnapshotA(photo: PhotoItem) {
        val updated = photo.copy(snapshotA = photo.params)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
    }

    suspend fun saveSnapshotB(photo: PhotoItem) {
        val updated = photo.copy(snapshotB = photo.params)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
    }

    suspend fun restoreHistoryStep(photo: PhotoItem, stepIndex: Int) {
        if (stepIndex in photo.history.indices) {
            val targetStep = photo.history[stepIndex]
            val truncatedHistory = photo.history.subList(0, stepIndex + 1)
            val updated = photo.copy(
                params = targetStep.paramsSnapshot,
                history = truncatedHistory
            )
            dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
        }
    }

    suspend fun importPhoto(
        uriString: String,
        title: String,
        dimensions: String = "4000 x 3000"
    ): PhotoItem {
        val item = PhotoItem(
            id = UUID.randomUUID().toString(),
            uriString = uriString,
            title = title,
            fileFormat = "JPEG / RAW Source",
            dimensions = dimensions,
            megaPixels = "12.0 MP",
            cameraModel = "Device Camera",
            lens = "Standard Lens",
            focalLength = "28mm equivalent",
            aperture = "f/1.8",
            shutterSpeed = "1/120s",
            iso = 100,
            rating = 0,
            colorLabels = emptySet(),
            tags = listOf("Imported", "User Media"),
            params = EditParams()
        )
        dao.insertPhoto(PhotoEntity.fromPhotoItem(item))
        return item
    }

    suspend fun duplicatePhoto(photo: PhotoItem) {
        val copy = photo.copy(
            id = UUID.randomUUID().toString(),
            title = "${photo.title} (Copy)"
        )
        dao.insertPhoto(PhotoEntity.fromPhotoItem(copy))
    }

    suspend fun deletePhoto(photo: PhotoItem) {
        dao.deletePhoto(PhotoEntity.fromPhotoItem(photo))
    }

    suspend fun saveCustomStyle(
        name: String,
        description: String,
        category: String,
        params: EditParams
    ) {
        val style = DarktableStyle(
            name = name,
            description = description,
            category = category,
            isBuiltIn = false,
            params = params
        )
        dao.insertStyle(StyleEntity.fromDarktableStyle(style))
    }

    suspend fun deleteStyle(style: DarktableStyle) {
        dao.deleteStyle(StyleEntity.fromDarktableStyle(style))
    }
}
