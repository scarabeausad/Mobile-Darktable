package com.example.repository

import com.example.db.DarktableDao
import com.example.db.PhotoEntity
import com.example.db.StyleEntity
import com.example.engine.ImportedPhotoInfo
import com.example.engine.XmpSidecarManager
import com.example.model.ColorLabel
import com.example.model.DarktableStyle
import com.example.model.EditParams
import com.example.model.HistoryStep
import com.example.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PhotoRepository(
    private val dao: DarktableDao,
    private val filesDir: File? = null
) {

    val allPhotos: Flow<List<PhotoItem>> = dao.getAllPhotos().map { entities ->
        entities.map { it.toPhotoItem() }
    }

    val allStyles: Flow<List<DarktableStyle>> = dao.getAllStyles().map { entities ->
        entities.map { it.toDarktableStyle() }
    }

    suspend fun getPhotoById(id: String): PhotoItem? {
        return dao.getPhotoById(id)?.toPhotoItem()
    }

    private suspend fun persistSidecar(photo: PhotoItem) {
        val dir = filesDir ?: return
        withContext(Dispatchers.IO) {
            try {
                XmpSidecarManager.writeSidecar(dir, photo)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun getXmpSidecarPathAndContent(photo: PhotoItem): Pair<String, String> =
        withContext(Dispatchers.IO) {
            val dir = filesDir
            if (dir != null) {
                val file = XmpSidecarManager.getSidecarFile(dir, photo)
                if (!file.exists()) {
                    XmpSidecarManager.writeSidecar(dir, photo)
                }
                val content = if (file.exists()) file.readText(Charsets.UTF_8) else XmpSidecarManager.generateXmpXml(photo)
                file.absolutePath to content
            } else {
                "${photo.id}.xmp" to XmpSidecarManager.generateXmpXml(photo)
            }
        }

    suspend fun ensureInitialDataSeeded() {
        // Seed default styles only if styles table is empty
        if (dao.getStyleCount() == 0) {
            val defaultStyles = DarktableStyle.BUILT_IN_STYLES.map { StyleEntity.fromDarktableStyle(it) }
            dao.insertStyles(defaultStyles)
        }

        // Seed sample photos only if photos table is empty (preserves user edits & imports across restarts)
        if (dao.getPhotoCount() == 0) {
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

            val samples = listOf(sampleLake, sampleStreet, samplePortrait, sampleDesert)
            dao.insertPhotos(samples.map { PhotoEntity.fromPhotoItem(it) })
            samples.forEach { persistSidecar(it) }
        } else {
            // Sync any existing photos from their XMP sidecar files on disk (and create missing XMPs)
            val dir = filesDir ?: return
            val existing = dao.getAllPhotosOnce().map { it.toPhotoItem() }
            for (photo in existing) {
                val xmpData = withContext(Dispatchers.IO) { XmpSidecarManager.readSidecar(dir, photo) }
                if (xmpData != null) {
                    val synced = photo.copy(
                        rating = xmpData.rating,
                        isRejected = xmpData.isRejected,
                        colorLabels = xmpData.colorLabels,
                        tags = if (xmpData.tags.isNotEmpty()) xmpData.tags else photo.tags,
                        params = xmpData.params,
                        history = if (xmpData.history.isNotEmpty()) xmpData.history else photo.history,
                        snapshotA = xmpData.snapshotA ?: photo.snapshotA,
                        snapshotB = xmpData.snapshotB ?: photo.snapshotB
                    )
                    if (synced != photo) {
                        dao.updatePhoto(PhotoEntity.fromPhotoItem(synced))
                    }
                } else {
                    persistSidecar(photo)
                }
            }
        }
    }

    suspend fun updateParams(
        photo: PhotoItem,
        newParams: EditParams,
        moduleName: String,
        description: String
    ): PhotoItem {
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
        persistSidecar(updated)
        return updated
    }

    suspend fun setParamsDirect(photo: PhotoItem, newParams: EditParams): PhotoItem {
        val updated = photo.copy(params = newParams)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
        persistSidecar(updated)
        return updated
    }

    suspend fun updateRating(photo: PhotoItem, newRating: Int): PhotoItem {
        val updated = photo.copy(rating = newRating)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
        persistSidecar(updated)
        return updated
    }

    suspend fun updateColorLabel(photo: PhotoItem, label: ColorLabel): PhotoItem {
        val updatedLabels = if (label == ColorLabel.NONE) {
            emptySet()
        } else if (photo.colorLabels.contains(label)) {
            photo.colorLabels - label
        } else {
            photo.colorLabels + label
        }
        val updated = photo.copy(colorLabels = updatedLabels)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
        persistSidecar(updated)
        return updated
    }

    suspend fun toggleColorLabel(photo: PhotoItem, label: ColorLabel): PhotoItem {
        return updateColorLabel(photo, label)
    }

    suspend fun updateColorLabels(photo: PhotoItem, labels: Set<ColorLabel>): PhotoItem {
        val updated = photo.copy(colorLabels = labels)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
        persistSidecar(updated)
        return updated
    }

    suspend fun toggleReject(photo: PhotoItem): PhotoItem {
        val updated = photo.copy(isRejected = !photo.isRejected)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
        persistSidecar(updated)
        return updated
    }

    suspend fun saveSnapshotA(photo: PhotoItem): PhotoItem {
        val updated = photo.copy(snapshotA = photo.params)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
        persistSidecar(updated)
        return updated
    }

    suspend fun saveSnapshotB(photo: PhotoItem): PhotoItem {
        val updated = photo.copy(snapshotB = photo.params)
        dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
        persistSidecar(updated)
        return updated
    }

    suspend fun restoreHistoryStep(photo: PhotoItem, stepIndex: Int): PhotoItem? {
        if (stepIndex in photo.history.indices) {
            val targetStep = photo.history[stepIndex]
            val truncatedHistory = photo.history.subList(0, stepIndex + 1)
            val updated = photo.copy(
                params = targetStep.paramsSnapshot,
                history = truncatedHistory
            )
            dao.updatePhoto(PhotoEntity.fromPhotoItem(updated))
            persistSidecar(updated)
            return updated
        }
        return null
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
        persistSidecar(item)
        return item
    }

    suspend fun importPhotoWithMetadata(info: ImportedPhotoInfo): PhotoItem {
        val item = PhotoItem(
            id = UUID.randomUUID().toString(),
            uriString = info.persistentUriString,
            title = info.title,
            fileFormat = info.fileFormat,
            dimensions = info.dimensions,
            megaPixels = info.megaPixels,
            cameraModel = info.cameraModel,
            lens = info.lens,
            focalLength = info.focalLength,
            aperture = info.aperture,
            shutterSpeed = info.shutterSpeed,
            iso = info.iso,
            rating = 0,
            colorLabels = emptySet(),
            tags = listOf("Imported", "User Media"),
            params = EditParams()
        )
        dao.insertPhoto(PhotoEntity.fromPhotoItem(item))
        persistSidecar(item)
        return item
    }

    suspend fun duplicatePhoto(photo: PhotoItem) {
        var newUri = photo.uriString
        if (photo.uriString.startsWith("file://")) {
            withContext(Dispatchers.IO) {
                try {
                    val srcFile = File(photo.uriString.removePrefix("file://"))
                    if (srcFile.exists() && srcFile.absolutePath.contains("imported_photos")) {
                        val ext = srcFile.extension.ifEmpty { "jpg" }
                        val destFile = File(srcFile.parentFile, "dt_${UUID.randomUUID()}.$ext")
                        srcFile.copyTo(destFile, overwrite = true)
                        newUri = android.net.Uri.fromFile(destFile).toString()
                    }
                } catch (_: Exception) {
                }
            }
        }
        val copy = photo.copy(
            id = UUID.randomUUID().toString(),
            uriString = newUri,
            title = "${photo.title} (Copy)"
        )
        dao.insertPhoto(PhotoEntity.fromPhotoItem(copy))
        persistSidecar(copy)
    }

    suspend fun deletePhoto(photo: PhotoItem) {
        dao.deletePhoto(PhotoEntity.fromPhotoItem(photo))
        val dir = filesDir ?: return
        withContext(Dispatchers.IO) {
            try {
                XmpSidecarManager.deleteSidecar(dir, photo)
                if (photo.uriString.startsWith("file://")) {
                    val file = File(photo.uriString.removePrefix("file://"))
                    if (file.exists() && file.absolutePath.contains("imported_photos")) {
                        file.delete()
                    }
                }
            } catch (_: Exception) {
            }
        }
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
