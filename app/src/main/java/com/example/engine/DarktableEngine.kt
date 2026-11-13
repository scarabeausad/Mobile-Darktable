package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.model.EditParams
import com.example.model.HistogramData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class ImportedPhotoInfo(
    val persistentUriString: String,
    val title: String,
    val dimensions: String,
    val megaPixels: String,
    val cameraModel: String,
    val lens: String,
    val focalLength: String,
    val aperture: String,
    val shutterSpeed: String,
    val iso: Int,
    val fileFormat: String
)

object DarktableEngine {

    // Neutral 5500K RGB baseline so 5500K yields exact (1f, 1f, 1f) multipliers
    private val neutral5500K: Triple<Float, Float, Float> by lazy {
        rawKelvinToRgb(5500f)
    }

    private fun rawKelvinToRgb(kelvin: Float): Triple<Float, Float, Float> {
        val temp = (kelvin / 100f).coerceIn(10f, 400f)
        val red = if (temp <= 66f) {
            255f
        } else {
            val r = temp - 60f
            (329.698727446f * (r.toDouble().pow(-0.1332047592)).toFloat()).coerceIn(0f, 255f)
        }
        val green = if (temp <= 66f) {
            (99.4708025861f * ln(temp.toDouble()).toFloat() - 161.1195681661f).coerceIn(0f, 255f)
        } else {
            val g = temp - 60f
            (288.1221695283f * (g.toDouble().pow(-0.0755148492)).toFloat()).coerceIn(0f, 255f)
        }
        val blue = if (temp >= 66f) {
            255f
        } else if (temp <= 19f) {
            0f
        } else {
            val b = temp - 10f
            (138.5177312231f * ln(b.toDouble()).toFloat() - 305.0447927307f).coerceIn(0f, 255f)
        }
        return Triple(red / 255f, green / 255f, blue / 255f)
    }

    /**
     * Approximates Planckian locus color temperature normalized to 5500K neutral daylight.
     */
    fun kelvinToRgbMultiplier(kelvin: Float): Triple<Float, Float, Float> {
        val (r, g, b) = rawKelvinToRgb(kelvin)
        val (nr, ng, nb) = neutral5500K
        // Blend between warm/cool relative to 5500K (note: higher Kelvin means warmer scene -> cooler compensation,
        // or in photo editor UX higher Kelvin slider = warmer/amber image). Keeping warm-up as K increases:
        val rr = (r / nr).coerceIn(0.4f, 2.0f)
        val gg = (g / ng).coerceIn(0.5f, 1.8f)
        val bb = (b / nb).coerceIn(0.4f, 2.0f)
        // Invert blue/red around 5500K so moving slider right (6500K+) warms the photo and left (3500K) cools it
        return Triple(
            (2f - bb).coerceIn(0.5f, 1.8f),
            gg,
            (2f - rr).coerceIn(0.5f, 1.8f)
        )
    }

    /**
     * Build Compose ColorMatrix for instantaneous fallback rendering.
     */
    fun createColorFilter(params: EditParams): ColorFilter {
        val exposureScale = 2.0f.pow(params.exposure)
        val (tempR, tempG, tempB) = kelvinToRgbMultiplier(params.temperature)
        val tintScaleG = 1.0f - (params.tint / 200f)
        val tintScaleM = 1.0f + (params.tint / 200f)

        val totalR = exposureScale * tempR * tintScaleM
        val totalG = exposureScale * tempG * tintScaleG
        val totalB = exposureScale * tempB

        val contrastFactor = (100f + params.contrast) / 100f
        val contrastOffset = 128f * (1.0f - contrastFactor)
        val satFactor = (100f + params.saturation + (params.vibrance * 0.7f)).coerceAtLeast(0f) / 100f

        val array = FloatArray(20)
        if (params.isMonochrome) {
            val sum = max(1f, params.redWeight + params.greenWeight + params.blueWeight)
            val rw = (params.redWeight / sum) * contrastFactor * exposureScale
            val gw = (params.greenWeight / sum) * contrastFactor * exposureScale
            val bw = (params.blueWeight / sum) * contrastFactor * exposureScale
            for (row in 0..2) {
                array[row * 5 + 0] = rw
                array[row * 5 + 1] = gw
                array[row * 5 + 2] = bw
                array[row * 5 + 3] = 0f
                array[row * 5 + 4] = contrastOffset
            }
            array[18] = 1f
        } else {
            val lr = 0.2126f
            val lg = 0.7152f
            val lb = 0.0722f
            val invSat = 1.0f - satFactor
            val rSat = invSat * lr
            val gSat = invSat * lg
            val bSat = invSat * lb

            array[0] = (rSat + satFactor) * totalR * contrastFactor
            array[1] = gSat * totalR * contrastFactor
            array[2] = bSat * totalR * contrastFactor
            array[3] = 0f
            array[4] = contrastOffset

            array[5] = rSat * totalG * contrastFactor
            array[6] = (gSat + satFactor) * totalG * contrastFactor
            array[7] = bSat * totalG * contrastFactor
            array[8] = 0f
            array[9] = contrastOffset

            array[10] = rSat * totalB * contrastFactor
            array[11] = gSat * totalB * contrastFactor
            array[12] = (bSat + satFactor) * totalB * contrastFactor
            array[13] = 0f
            array[14] = contrastOffset

            array[18] = 1f
        }
        return ColorFilter.colorMatrix(ColorMatrix(array))
    }

    /**
     * Evaluates Darktable's S-curve transfer function for filmic tone mapping.
     * x in [0.0, 1.0] -> y in [0.0, 1.0]
     */
    fun evaluateFilmicCurve(
        x: Float,
        toe: Float = 35f,
        shoulder: Float = 40f,
        contrast: Float = 1.15f
    ): Float {
        val clamped = x.coerceIn(0f, 1f)
        val toeFactor = toe / 100f
        val shoulderFactor = shoulder / 100f

        val slope = contrast * 2.5f
        val mid = 0.5f + (toeFactor - shoulderFactor) * 0.2f
        val rawSigmoid = 1.0f / (1.0f + exp(-slope * (clamped - mid)))
        val zeroSigmoid = 1.0f / (1.0f + exp(-slope * (0f - mid)))
        val oneSigmoid = 1.0f / (1.0f + exp(-slope * (1f - mid)))

        return ((rawSigmoid - zeroSigmoid) / (oneSigmoid - zeroSigmoid)).coerceIn(0f, 1f)
    }

    /**
     * Evaluates the full Exposure & Tone LUT mapping for a normalized luminance/channel value [0, 1],
     * combining Filmic RGB, Black Point, Shadows Lift, Highlights Recovery, and Contrast.
     */
    fun evaluateToneLut(norm: Float, params: EditParams): Float {
        var mapped = norm.coerceIn(0f, 1f)

        // 1. Black Point Level (-50 to +50)
        // Positive values deepen blacks (clip low floor), negative values lift/fade blacks
        if (params.blackPoint != 0f) {
            val bpShift = (params.blackPoint / 100f) * 0.38f
            mapped = if (bpShift > 0f) {
                ((mapped - bpShift) / max(0.05f, 1f - bpShift)).coerceIn(0f, 1f)
            } else {
                val lift = -bpShift
                (lift + mapped * (1f - lift)).coerceIn(0f, 1f)
            }
        }

        // 2. Filmic RGB S-curve
        if (params.filmicEnabled) {
            mapped = evaluateFilmicCurve(
                mapped,
                params.filmicToe,
                params.filmicShoulder,
                params.filmicContrast
            )
        }

        // 3. Shadows Lift (-100 to +100) — affects darker regions smoothly
        if (params.shadows != 0f) {
            val shadowWeight = (1f - mapped).pow(1.6f)
            mapped = (mapped + (params.shadows / 100f) * shadowWeight * 0.42f).coerceIn(0f, 1f)
        }

        // 4. Highlights Recovery (-100 to +100) — affects bright regions smoothly
        if (params.highlights != 0f) {
            val highlightWeight = mapped.pow(1.6f)
            mapped = (mapped + (params.highlights / 100f) * highlightWeight * 0.42f).coerceIn(0f, 1f)
        }

        // 5. Midtone Contrast (-100 to +100)
        if (params.contrast != 0f) {
            val contrastFactor = (100f + params.contrast) / 100f
            mapped = ((mapped - 0.5f) * contrastFactor + 0.5f).coerceIn(0f, 1f)
        }

        return mapped.coerceIn(0f, 1f)
    }

    /**
     * Computes 256-bucket RGB and Luminance histograms from a bitmap.
     */
    suspend fun computeHistogram(bitmap: Bitmap): HistogramData = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val stepX = max(1, width / 120)
        val stepY = max(1, height / 120)

        val rBins = IntArray(256)
        val gBins = IntArray(256)
        val bBins = IntArray(256)
        val lumaBins = IntArray(256)

        var totalSamples = 0
        var clippedShadows = 0
        var clippedHighlights = 0

        val pixels = IntArray(width)
        for (y in 0 until height step stepY) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
            for (x in 0 until width step stepX) {
                val pixel = pixels[x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luma = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)

                rBins[r]++
                gBins[g]++
                bBins[b]++
                lumaBins[luma]++
                totalSamples++

                if (r <= 1 && g <= 1 && b <= 1) clippedShadows++
                if (r >= 254 && g >= 254 && b >= 254) clippedHighlights++
            }
        }

        var maxBin = 1
        for (i in 0 until 256) {
            if (rBins[i] > maxBin) maxBin = rBins[i]
            if (gBins[i] > maxBin) maxBin = gBins[i]
            if (bBins[i] > maxBin) maxBin = bBins[i]
            if (lumaBins[i] > maxBin) maxBin = lumaBins[i]
        }

        val shadowClipPct = if (totalSamples > 0) (clippedShadows.toFloat() / totalSamples) * 100f else 0f
        val highlightClipPct = if (totalSamples > 0) (clippedHighlights.toFloat() / totalSamples) * 100f else 0f

        HistogramData(
            redChannel = rBins,
            greenChannel = gBins,
            blueChannel = bBins,
            lumaChannel = lumaBins,
            maxBinCount = maxBin,
            clippedShadowsPercent = shadowClipPct,
            clippedHighlightsPercent = highlightClipPct
        )
    }

    /**
     * Copies a picked photo from temporary content:// URI into persistent app internal storage
     * and extracts EXIF telemetry so the image remains permanently available across app restarts.
     */
    suspend fun importToInternalStorage(context: Context, sourceUri: Uri): ImportedPhotoInfo? =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mime = resolver.getType(sourceUri) ?: "image/jpeg"
                val ext = when {
                    mime.contains("png", ignoreCase = true) -> "png"
                    mime.contains("webp", ignoreCase = true) -> "webp"
                    mime.contains("dng", ignoreCase = true) -> "dng"
                    else -> "jpg"
                }

                val importDir = File(context.filesDir, "imported_photos").apply {
                    if (!exists()) mkdirs()
                }
                val destFile = File(importDir, "dt_${UUID.randomUUID()}.$ext")

                resolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                } ?: return@withContext null

                // Read dimensions
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(destFile.absolutePath, bounds)
                var w = bounds.outWidth
                var h = bounds.outHeight

                // Extract EXIF
                var cameraModel = "Device Camera"
                var lens = "Integrated Lens"
                var focalLength = "28mm"
                var aperture = "f/1.8"
                var shutterSpeed = "1/120s"
                var iso = 100
                var orientation = ExifInterface.ORIENTATION_NORMAL

                try {
                    val exif = ExifInterface(destFile.absolutePath)
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                        orientation == ExifInterface.ORIENTATION_ROTATE_270
                    ) {
                        val tmp = w; w = h; h = tmp
                    }

                    val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim() ?: ""
                    val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim() ?: ""
                    if (model.isNotEmpty()) {
                        cameraModel = if (make.isNotEmpty() && !model.startsWith(make, ignoreCase = true)) {
                            "$make $model"
                        } else model
                    }
                    exif.getAttribute("LensModel")?.let { if (it.isNotBlank()) lens = it }
                    exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { raw ->
                        val parts = raw.split("/")
                        if (parts.size == 2) {
                            val num = parts[0].toDoubleOrNull() ?: 0.0
                            val den = parts[1].toDoubleOrNull() ?: 1.0
                            if (den > 0) focalLength = "${(num / den).toInt()}mm"
                        }
                    }
                    exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let {
                        if (it.isNotBlank()) aperture = "f/$it"
                    }
                    exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { expStr ->
                        val sec = expStr.toDoubleOrNull()
                        if (sec != null && sec > 0) {
                            shutterSpeed = if (sec < 1.0) "1/${(1.0 / sec).toInt()}s" else "${sec}s"
                        }
                    }
                    exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0).let {
                        if (it > 0) iso = it
                    }
                } catch (_: Exception) {
                    // EXIF optional
                }

                // Query original display name if available
                var displayName = "Imported ${destFile.nameWithoutExtension.takeLast(6)}"
                try {
                    resolver.query(sourceUri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                        ?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                                if (idx >= 0) {
                                    val name = cursor.getString(idx)
                                    if (!name.isNullOrBlank()) {
                                        displayName = name.substringBeforeLast(".")
                                    }
                                }
                            }
                        }
                } catch (_: Exception) {
                }

                val mpValue = if (w > 0 && h > 0) (w.toDouble() * h.toDouble()) / 1_000_000.0 else 12.0
                val dimensionsStr = if (w > 0 && h > 0) "$w x $h" else "4000 x 3000"

                ImportedPhotoInfo(
                    persistentUriString = Uri.fromFile(destFile).toString(),
                    title = displayName,
                    dimensions = dimensionsStr,
                    megaPixels = "${"%.1f".format(mpValue)} MP",
                    cameraModel = cameraModel,
                    lens = lens,
                    focalLength = focalLength,
                    aperture = aperture,
                    shutterSpeed = shutterSpeed,
                    iso = iso,
                    fileFormat = "${ext.uppercase()} Source"
                )
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Loads a Bitmap safely from uriString (supports res://, file://, /, or content://)
     * and applies EXIF orientation automatically.
     */
    suspend fun loadBitmap(context: Context, uriString: String, maxDimension: Int = 2048): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                if (uriString.startsWith("res://")) {
                    val resName = uriString.removePrefix("res://")
                    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                    if (resId != 0) {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeResource(context.resources, resId, options)
                        var sample = 1
                        while (options.outWidth / sample > maxDimension || options.outHeight / sample > maxDimension) {
                            sample *= 2
                        }
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                        return@withContext BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
                    }
                }

                if (uriString.startsWith("file://") || uriString.startsWith("/")) {
                    val path = uriString.removePrefix("file://")
                    val file = File(path)
                    if (!file.exists()) return@withContext null

                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, options)
                    var sample = 1
                    while (options.outWidth / sample > maxDimension || options.outHeight / sample > maxDimension) {
                        sample *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                    val rawBmp = BitmapFactory.decodeFile(path, decodeOpts) ?: return@withContext null
                    return@withContext applyExifOrientation(rawBmp, path)
                }

                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                var sample = 1
                while (options.outWidth / sample > maxDimension || options.outHeight / sample > maxDimension) {
                    sample *= 2
                }

                val decodeStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOpts)
                decodeStream.close()
                bitmap
            } catch (e: Exception) {
                null
            }
        }

    private fun applyExifOrientation(bitmap: Bitmap, filePath: String): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (_: Exception) {
            bitmap
        }
    }

    /**
     * Fast scaled proxy bitmap for real-time darkroom preview processing.
     */
    fun createPreviewProxy(source: Bitmap, maxDim: Int = 720): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maxDim) return source
        val scale = maxDim.toFloat() / longest
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }

    enum class CropHandle {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, MOVE
    }

    /**
     * Computes the exact target crop rectangle ratios (left, top, right, bottom in 0f..1f)
     * for a given image size and cropAspectRatio string.
     */
    fun computeCropRectNormalized(
        imageWidth: Float,
        imageHeight: Float,
        cropAspectRatio: String
    ): FloatArray {
        if (cropAspectRatio == "Original" || cropAspectRatio == "Free" || imageWidth <= 0f || imageHeight <= 0f) {
            return floatArrayOf(0f, 0f, 1f, 1f)
        }
        val targetRatio = aspectRatioValue(cropAspectRatio) ?: (imageWidth / imageHeight)
        val currentRatio = imageWidth / imageHeight
        return if (currentRatio > targetRatio) {
            val widthFrac = (targetRatio / currentRatio).coerceIn(0.05f, 1f)
            val marginX = (1f - widthFrac) / 2f
            floatArrayOf(marginX, 0f, 1f - marginX, 1f)
        } else {
            val heightFrac = (currentRatio / targetRatio).coerceIn(0.05f, 1f)
            val marginY = (1f - heightFrac) / 2f
            floatArrayOf(0f, marginY, 1f, 1f - marginY)
        }
    }

    fun aspectRatioValue(cropAspectRatio: String): Float? = when (cropAspectRatio) {
        "1:1" -> 1.0f
        "4:3" -> 4f / 3f
        "3:2" -> 3f / 2f
        "16:9" -> 16f / 9f
        else -> null
    }

    /**
     * Resolves the effective normalized crop rectangle [left, top, right, bottom] for rendering/overlay.
     */
    fun resolveCropRect(
        imageWidth: Float,
        imageHeight: Float,
        params: EditParams
    ): FloatArray {
        val isDefaultBounds = params.cropLeft <= 0.0005f &&
            params.cropTop <= 0.0005f &&
            params.cropRight >= 0.9995f &&
            params.cropBottom >= 0.9995f

        if (isDefaultBounds) {
            return computeCropRectNormalized(imageWidth, imageHeight, params.cropAspectRatio)
        }

        val left = params.cropLeft.coerceIn(0f, 0.90f)
        val top = params.cropTop.coerceIn(0f, 0.90f)
        val right = params.cropRight.coerceIn(left + 0.08f, 1f)
        val bottom = params.cropBottom.coerceIn(top + 0.08f, 1f)
        return floatArrayOf(left, top, right, bottom)
    }

    /**
     * Moves the crop rectangle by normalized delta (dxNorm, dyNorm) clamped inside [0f..1f].
     */
    fun moveCropRect(rect: FloatArray, dxNorm: Float, dyNorm: Float): FloatArray {
        val w = (rect[2] - rect[0]).coerceIn(0.08f, 1f)
        val h = (rect[3] - rect[1]).coerceIn(0.08f, 1f)
        val newLeft = (rect[0] + dxNorm).coerceIn(0f, 1f - w)
        val newTop = (rect[1] + dyNorm).coerceIn(0f, 1f - h)
        return floatArrayOf(newLeft, newTop, newLeft + w, newTop + h)
    }

    /**
     * Resizes the crop rectangle from the given handle, preserving aspect ratio if locked
     * (1:1, 4:3, 3:2, 16:9) or freely in Original/Free mode.
     */
    fun resizeCropRect(
        rect: FloatArray,
        handle: CropHandle,
        dxNorm: Float,
        dyNorm: Float,
        imageWidth: Float,
        imageHeight: Float,
        cropAspectRatio: String
    ): FloatArray {
        if (handle == CropHandle.MOVE) {
            return moveCropRect(rect, dxNorm, dyNorm)
        }
        if (handle == CropHandle.NONE || imageWidth <= 0f || imageHeight <= 0f) {
            return rect
        }

        val minSpan = 0.10f
        val targetRatio = aspectRatioValue(cropAspectRatio)
        var left = rect[0]
        var top = rect[1]
        var right = rect[2]
        var bottom = rect[3]

        if (targetRatio == null) {
            // Freeform resize
            when (handle) {
                CropHandle.TOP_LEFT -> {
                    left = (left + dxNorm).coerceIn(0f, right - minSpan)
                    top = (top + dyNorm).coerceIn(0f, bottom - minSpan)
                }
                CropHandle.TOP_RIGHT -> {
                    right = (right + dxNorm).coerceIn(left + minSpan, 1f)
                    top = (top + dyNorm).coerceIn(0f, bottom - minSpan)
                }
                CropHandle.BOTTOM_LEFT -> {
                    left = (left + dxNorm).coerceIn(0f, right - minSpan)
                    bottom = (bottom + dyNorm).coerceIn(top + minSpan, 1f)
                }
                CropHandle.BOTTOM_RIGHT -> {
                    right = (right + dxNorm).coerceIn(left + minSpan, 1f)
                    bottom = (bottom + dyNorm).coerceIn(top + minSpan, 1f)
                }
                CropHandle.LEFT -> left = (left + dxNorm).coerceIn(0f, right - minSpan)
                CropHandle.RIGHT -> right = (right + dxNorm).coerceIn(left + minSpan, 1f)
                CropHandle.TOP -> top = (top + dyNorm).coerceIn(0f, bottom - minSpan)
                CropHandle.BOTTOM -> bottom = (bottom + dyNorm).coerceIn(top + minSpan, 1f)
                else -> Unit
            }
            return floatArrayOf(left, top, right, bottom)
        }

        // Aspect-ratio locked resize: hNorm = wNorm * k
        val k = (imageWidth / imageHeight) / targetRatio

        when (handle) {
            CropHandle.TOP_LEFT -> {
                val desiredW = max(right - (left + dxNorm), (bottom - (top + dyNorm)) / k)
                val maxW = min(right, bottom / k)
                val minW = max(minSpan, minSpan / k).coerceAtMost(maxW)
                val w = desiredW.coerceIn(minW, maxW)
                val h = w * k
                return floatArrayOf(right - w, bottom - h, right, bottom)
            }
            CropHandle.TOP_RIGHT -> {
                val desiredW = max((right + dxNorm) - left, (bottom - (top + dyNorm)) / k)
                val maxW = min(1f - left, bottom / k)
                val minW = max(minSpan, minSpan / k).coerceAtMost(maxW)
                val w = desiredW.coerceIn(minW, maxW)
                val h = w * k
                return floatArrayOf(left, bottom - h, left + w, bottom)
            }
            CropHandle.BOTTOM_LEFT -> {
                val desiredW = max(right - (left + dxNorm), ((bottom + dyNorm) - top) / k)
                val maxW = min(right, (1f - top) / k)
                val minW = max(minSpan, minSpan / k).coerceAtMost(maxW)
                val w = desiredW.coerceIn(minW, maxW)
                val h = w * k
                return floatArrayOf(right - w, top, right, top + h)
            }
            CropHandle.BOTTOM_RIGHT -> {
                val desiredW = max((right + dxNorm) - left, ((bottom + dyNorm) - top) / k)
                val maxW = min(1f - left, (1f - top) / k)
                val minW = max(minSpan, minSpan / k).coerceAtMost(maxW)
                val w = desiredW.coerceIn(minW, maxW)
                val h = w * k
                return floatArrayOf(left, top, left + w, top + h)
            }
            CropHandle.LEFT -> {
                val desiredW = right - (left + dxNorm)
                val maxW = min(right, 1f / k)
                val minW = max(minSpan, minSpan / k).coerceAtMost(maxW)
                val w = desiredW.coerceIn(minW, maxW)
                val h = w * k
                val cy = (top + bottom) / 2f
                val newTop = (cy - h / 2f).coerceIn(0f, 1f - h)
                return floatArrayOf(right - w, newTop, right, newTop + h)
            }
            CropHandle.RIGHT -> {
                val desiredW = (right + dxNorm) - left
                val maxW = min(1f - left, 1f / k)
                val minW = max(minSpan, minSpan / k).coerceAtMost(maxW)
                val w = desiredW.coerceIn(minW, maxW)
                val h = w * k
                val cy = (top + bottom) / 2f
                val newTop = (cy - h / 2f).coerceIn(0f, 1f - h)
                return floatArrayOf(left, newTop, left + w, newTop + h)
            }
            CropHandle.TOP -> {
                val desiredH = bottom - (top + dyNorm)
                val desiredW = desiredH / k
                val maxW = min(1f, bottom / k)
                val minW = max(minSpan, minSpan / k).coerceAtMost(maxW)
                val w = desiredW.coerceIn(minW, maxW)
                val h = w * k
                val cx = (left + right) / 2f
                val newLeft = (cx - w / 2f).coerceIn(0f, 1f - w)
                return floatArrayOf(newLeft, bottom - h, newLeft + w, bottom)
            }
            CropHandle.BOTTOM -> {
                val desiredH = (bottom + dyNorm) - top
                val desiredW = desiredH / k
                val maxW = min(1f, (1f - top) / k)
                val minW = max(minSpan, minSpan / k).coerceAtMost(maxW)
                val w = desiredW.coerceIn(minW, maxW)
                val h = w * k
                val cx = (left + right) / 2f
                val newLeft = (cx - w / 2f).coerceIn(0f, 1f - w)
                return floatArrayOf(newLeft, top, newLeft + w, top + h)
            }
            else -> return rect
        }
    }

    /**
     * Real-time preview render pipeline: applies every active module accurately to a proxy bitmap.
     */
    suspend fun renderPreviewBitmap(
        proxyBitmap: Bitmap,
        params: EditParams,
        applyCrop: Boolean = false
    ): Bitmap = renderPipelineInternal(proxyBitmap, params, maxDimension = 0, applyCrop = applyCrop)

    /**
     * Full precision render pipeline for export.
     */
    suspend fun renderFinalBitmap(
        sourceBitmap: Bitmap,
        params: EditParams,
        maxDimension: Int = 0
    ): Bitmap = renderPipelineInternal(sourceBitmap, params, maxDimension = maxDimension, applyCrop = true)

    private suspend fun renderPipelineInternal(
        sourceBitmap: Bitmap,
        params: EditParams,
        maxDimension: Int,
        applyCrop: Boolean
    ): Bitmap = withContext(Dispatchers.Default) {
        // Step 1: Geometric transformations (flip & rotation)
        val matrix = Matrix()
        if (params.flipHorizontal) {
            matrix.postScale(-1f, 1f, sourceBitmap.width / 2f, sourceBitmap.height / 2f)
        }
        if (params.rotationAngle != 0f) {
            matrix.postRotate(params.rotationAngle, sourceBitmap.width / 2f, sourceBitmap.height / 2f)
        }

        var transformed = if (params.flipHorizontal || params.rotationAngle != 0f) {
            Bitmap.createBitmap(
                sourceBitmap,
                0,
                0,
                sourceBitmap.width,
                sourceBitmap.height,
                matrix,
                true
            )
        } else {
            sourceBitmap
        }

        // Step 2: Aspect Ratio / Custom Crop (if enabled)
        if (applyCrop) {
            val normRect = resolveCropRect(
                transformed.width.toFloat(),
                transformed.height.toFloat(),
                params
            )
            val isFullFrame = normRect[0] <= 0.001f && normRect[1] <= 0.001f &&
                normRect[2] >= 0.999f && normRect[3] >= 0.999f
            if (!isFullFrame) {
                val startX = (normRect[0] * transformed.width).toInt().coerceIn(0, transformed.width - 1)
                val startY = (normRect[1] * transformed.height).toInt().coerceIn(0, transformed.height - 1)
                val cropW = ((normRect[2] - normRect[0]) * transformed.width).toInt().coerceIn(1, transformed.width - startX)
                val cropH = ((normRect[3] - normRect[1]) * transformed.height).toInt().coerceIn(1, transformed.height - startY)
                val cropped = Bitmap.createBitmap(transformed, startX, startY, cropW, cropH)
                if (cropped != transformed && transformed != sourceBitmap) {
                    transformed.recycle()
                }
                transformed = cropped
            }
        }

        // Step 3: Optional downscale for export dimension
        if (maxDimension in 1 until max(transformed.width, transformed.height)) {
            val scale = maxDimension.toFloat() / max(transformed.width, transformed.height)
            val scaledW = (transformed.width * scale).toInt().coerceAtLeast(1)
            val scaledH = (transformed.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(transformed, scaledW, scaledH, true)
            if (scaled != transformed && transformed != sourceBitmap) {
                transformed.recycle()
            }
            transformed = scaled
        }

        val width = transformed.width
        val height = transformed.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Precompute color and tone parameters
        val exposureScale = 2.0f.pow(params.exposure)
        val (tempR, tempG, tempB) = kelvinToRgbMultiplier(params.temperature)
        val tintScaleG = 1.0f - (params.tint / 200f)
        val tintScaleM = 1.0f + (params.tint / 200f)
        val multR = exposureScale * tempR * tintScaleM
        val multG = exposureScale * tempG * tintScaleG
        val multB = exposureScale * tempB

        val satShift = params.saturation / 100f
        val vibShift = params.vibrance / 100f
        val grainStrength = (params.grain / 100f) * 42f

        // Tone curve LUT (256 entries) combining Black Point, Filmic, Shadows, Highlights, Contrast
        val toneLut = FloatArray(256)
        for (i in 0 until 256) {
            toneLut[i] = evaluateToneLut(i / 255f, params)
        }

        val pixels = IntArray(width * height)
        transformed.getPixels(pixels, 0, width, 0, 0, width, height)

        val monoSum = max(1f, params.redWeight + params.greenWeight + params.blueWeight)
        val monoR = params.redWeight / monoSum
        val monoG = params.greenWeight / monoSum
        val monoB = params.blueWeight / monoSum

        for (idx in pixels.indices) {
            val pixel = pixels[idx]
            val a = (pixel ushr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            var outR: Int
            var outG: Int
            var outB: Int

            if (params.isMonochrome) {
                val gray = ((r * monoR + g * monoG + b * monoB) * exposureScale).toInt().coerceIn(0, 255)
                val mapped = (toneLut[gray] * 255f).toInt().coerceIn(0, 255)
                outR = mapped
                outG = mapped
                outB = mapped
            } else {
                // 1. Exposure + White Balance
                var fr = (r * multR).coerceIn(0f, 255f)
                var fg = (g * multG).coerceIn(0f, 255f)
                var fb = (b * multB).coerceIn(0f, 255f)

                // 2. Tone LUT mapping (Black Point, Filmic, Shadows, Highlights, Contrast)
                val tr = toneLut[fr.toInt().coerceIn(0, 255)] * 255f
                val tg = toneLut[fg.toInt().coerceIn(0, 255)] * 255f
                val tb = toneLut[fb.toInt().coerceIn(0, 255)] * 255f

                // 3. Saturation & Vibrance (vibrance boosts muted pixels stronger than vivid pixels)
                if (satShift != 0f || vibShift != 0f) {
                    val luma = 0.2126f * tr + 0.7152f * tg + 0.0722f * tb
                    val maxC = max(tr, max(tg, tb))
                    val minC = min(tr, min(tg, tb))
                    val pixelSat = if (maxC > 1f) (maxC - minC) / maxC else 0f
                    val effectiveSat = (1f + satShift + vibShift * (1f - pixelSat)).coerceAtLeast(0f)

                    outR = (luma + effectiveSat * (tr - luma)).toInt().coerceIn(0, 255)
                    outG = (luma + effectiveSat * (tg - luma)).toInt().coerceIn(0, 255)
                    outB = (luma + effectiveSat * (tb - luma)).toInt().coerceIn(0, 255)
                } else {
                    outR = tr.toInt().coerceIn(0, 255)
                    outG = tg.toInt().coerceIn(0, 255)
                    outB = tb.toInt().coerceIn(0, 255)
                }
            }

            // 4. Deterministic Film Grain
            if (grainStrength > 0.5f) {
                var h = idx * 374761393 + 668265263
                h = (h xor (h ushr 13)) * 1274126177
                val noiseNorm = ((h xor (h ushr 16)) and 0xFF) / 127.5f - 1.0f
                val delta = (noiseNorm * grainStrength).toInt()
                outR = (outR + delta).coerceIn(0, 255)
                outG = (outG + delta).coerceIn(0, 255)
                outB = (outB + delta).coerceIn(0, 255)
            }

            pixels[idx] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        // Step 5: Unsharp Mask Sharpness (when sharpness > 10)
        val sharpAmount = ((params.sharpness - 10f).coerceAtLeast(0f) / 90f) * 1.1f
        if (sharpAmount > 0.02f && width > 2 && height > 2) {
            val sharpened = pixels.copyOf()
            for (y in 1 until height - 1) {
                val rowOffset = y * width
                for (x in 1 until width - 1) {
                    val idx = rowOffset + x
                    val c = pixels[idx]
                    val top = pixels[idx - width]
                    val bot = pixels[idx + width]
                    val lft = pixels[idx - 1]
                    val rgt = pixels[idx + 1]

                    val a = (c ushr 24) and 0xFF
                    val cr = (c shr 16) and 0xFF
                    val cg = (c shr 8) and 0xFF
                    val cb = c and 0xFF

                    val blurR = (((top shr 16) and 0xFF) + ((bot shr 16) and 0xFF) + ((lft shr 16) and 0xFF) + ((rgt shr 16) and 0xFF)) shr 2
                    val blurG = (((top shr 8) and 0xFF) + ((bot shr 8) and 0xFF) + ((lft shr 8) and 0xFF) + ((rgt shr 8) and 0xFF)) shr 2
                    val blurB = ((top and 0xFF) + (bot and 0xFF) + (lft and 0xFF) + (rgt and 0xFF)) shr 2

                    val sr = (cr + (cr - blurR) * sharpAmount).toInt().coerceIn(0, 255)
                    val sg = (cg + (cg - blurG) * sharpAmount).toInt().coerceIn(0, 255)
                    val sb = (cb + (cb - blurB) * sharpAmount).toInt().coerceIn(0, 255)

                    sharpened[idx] = (a shl 24) or (sr shl 16) or (sg shl 8) or sb
                }
            }
            outputBitmap.setPixels(sharpened, 0, width, 0, 0, width, height)
        } else {
            outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        }

        // Step 6: Radial Vignette shader overlay
        if (params.vignette != 0f) {
            val canvas = Canvas(outputBitmap)
            val centerX = width / 2f
            val centerY = height / 2f
            val maxRadius = sqrt(centerX * centerX + centerY * centerY)
            val radius = max(1f, maxRadius * (params.vignetteRadius / 100f))

            val vignetteAlpha = (abs(params.vignette) * 2.2f).toInt().coerceIn(0, 240)
            val colorOuter = if (params.vignette < 0) {
                (vignetteAlpha shl 24) or 0x000000
            } else {
                (vignetteAlpha shl 24) or 0xFFFFFF
            }

            val shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(0x00000000, colorOuter),
                floatArrayOf(0.35f, 1.0f),
                Shader.TileMode.CLAMP
            )
            val paint = Paint().apply { this.shader = shader }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        if (transformed != sourceBitmap) {
            transformed.recycle()
        }

        outputBitmap
    }

    /**
     * Exports processed image to Android public media gallery and returns destination URI.
     */
    suspend fun saveToGallery(
        context: Context,
        renderedBitmap: Bitmap,
        title: String,
        format: String = "JPEG",
        quality: Int = 92
    ): Uri? = withContext(Dispatchers.IO) {
        val compressFormat = when (format.uppercase()) {
            "PNG" -> Bitmap.CompressFormat.PNG
            "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            else -> Bitmap.CompressFormat.JPEG
        }

        val extension = when (format.uppercase()) {
            "PNG" -> "png"
            "WEBP" -> "webp"
            else -> "jpg"
        }
        val mimeType = when (format.uppercase()) {
            "PNG" -> "image/png"
            "WEBP" -> "image/webp"
            else -> "image/jpeg"
        }

        val filename = "Darktable_${System.currentTimeMillis()}_$title.$extension"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Darktable")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@withContext null

        try {
            val outputStream: OutputStream? = resolver.openOutputStream(uri)
            if (outputStream != null) {
                renderedBitmap.compress(compressFormat, quality, outputStream)
                outputStream.flush()
                outputStream.close()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    /**
     * Saves rendered bitmap to app cache for sharing via ACTION_SEND.
     */
    suspend fun saveToCacheForShare(
        context: Context,
        renderedBitmap: Bitmap,
        title: String
    ): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(cacheDir, "Darktable_${System.currentTimeMillis()}_$title.jpg")
        FileOutputStream(file).use { out ->
            renderedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            out.flush()
        }
        file
    }
}
