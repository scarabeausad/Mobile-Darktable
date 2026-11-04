package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ColorLabel
import com.example.model.EditParams
import com.example.model.HistoryStep
import com.example.model.PhotoItem
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: String,
    val uriString: String,
    val title: String,
    val captureDate: Long,
    val fileFormat: String,
    val dimensions: String,
    val megaPixels: String,
    val cameraModel: String,
    val lens: String,
    val focalLength: String,
    val aperture: String,
    val shutterSpeed: String,
    val iso: Int,
    val exposureBias: String,
    val colorSpace: String,
    val rating: Int,
    val isRejected: Boolean,
    val colorLabel: String,
    val tagsJson: String,
    val paramsJson: String,
    val historyJson: String,
    val snapshotAJson: String?,
    val snapshotBJson: String?
) {
    fun toPhotoItem(): PhotoItem {
        val parsedLabels = mutableSetOf<ColorLabel>()
        if (colorLabel.isNotBlank()) {
            colorLabel.split(",").forEach { name ->
                try {
                    val lbl = ColorLabel.valueOf(name.trim())
                    if (lbl != ColorLabel.NONE) {
                        parsedLabels.add(lbl)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        val parsedTags = mutableListOf<String>()
        try {
            val arr = JSONArray(tagsJson)
            for (i in 0 until arr.length()) {
                parsedTags.add(arr.getString(i))
            }
        } catch (e: Exception) {
            // ignore
        }

        val parsedHistory = mutableListOf<HistoryStep>()
        try {
            val arr = JSONArray(historyJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                parsedHistory.add(
                    HistoryStep(
                        id = obj.optString("id"),
                        moduleName = obj.optString("moduleName"),
                        description = obj.optString("description"),
                        timestamp = obj.optLong("timestamp"),
                        paramsSnapshot = EditParams.fromJson(obj.optString("params"))
                    )
                )
            }
        } catch (e: Exception) {
            parsedHistory.add(
                HistoryStep(
                    moduleName = "Base RAW Input",
                    description = "Demosaic & Linear RGB Color Profile",
                    paramsSnapshot = EditParams()
                )
            )
        }

        return PhotoItem(
            id = id,
            uriString = uriString,
            title = title,
            captureDate = captureDate,
            fileFormat = fileFormat,
            dimensions = dimensions,
            megaPixels = megaPixels,
            cameraModel = cameraModel,
            lens = lens,
            focalLength = focalLength,
            aperture = aperture,
            shutterSpeed = shutterSpeed,
            iso = iso,
            exposureBias = exposureBias,
            colorSpace = colorSpace,
            rating = rating,
            isRejected = isRejected,
            colorLabels = parsedLabels,
            tags = parsedTags,
            params = EditParams.fromJson(paramsJson),
            history = if (parsedHistory.isEmpty()) listOf(
                HistoryStep(
                    moduleName = "Base RAW Input",
                    description = "Demosaic & Linear RGB Color Profile",
                    paramsSnapshot = EditParams()
                )
            ) else parsedHistory,
            snapshotA = snapshotAJson?.let { EditParams.fromJson(it) },
            snapshotB = snapshotBJson?.let { EditParams.fromJson(it) }
        )
    }

    companion object {
        fun fromPhotoItem(item: PhotoItem): PhotoEntity {
            val tagsArr = JSONArray()
            item.tags.forEach { tagsArr.put(it) }

            val histArr = JSONArray()
            item.history.forEach { step ->
                val obj = JSONObject()
                obj.put("id", step.id)
                obj.put("moduleName", step.moduleName)
                obj.put("description", step.description)
                obj.put("timestamp", step.timestamp)
                obj.put("params", step.paramsSnapshot.toJson())
                histArr.put(obj)
            }

            return PhotoEntity(
                id = item.id,
                uriString = item.uriString,
                title = item.title,
                captureDate = item.captureDate,
                fileFormat = item.fileFormat,
                dimensions = item.dimensions,
                megaPixels = item.megaPixels,
                cameraModel = item.cameraModel,
                lens = item.lens,
                focalLength = item.focalLength,
                aperture = item.aperture,
                shutterSpeed = item.shutterSpeed,
                iso = item.iso,
                exposureBias = item.exposureBias,
                colorSpace = item.colorSpace,
                rating = item.rating,
                isRejected = item.isRejected,
                colorLabel = if (item.colorLabels.isEmpty()) "NONE" else item.colorLabels.joinToString(",") { it.name },
                tagsJson = tagsArr.toString(),
                paramsJson = item.params.toJson(),
                historyJson = histArr.toString(),
                snapshotAJson = item.snapshotA?.toJson(),
                snapshotBJson = item.snapshotB?.toJson()
            )
        }
    }
}
