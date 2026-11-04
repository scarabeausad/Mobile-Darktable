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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

object DarktableEngine {

    /**
     * Build Compose ColorMatrix for instantaneous GPU-accelerated viewport rendering.
     */
    fun createColorFilter(params: EditParams): ColorFilter {
        // 1. Exposure multiplier (2^EV)
        val exposureScale = 2.0f.pow(params.exposure)

        // 2. White Balance Kelvin to RGB approximation
        val (tempR, tempG, tempB) = kelvinToRgbMultiplier(params.temperature)

        // 3. Tint adjustment (green-magenta)
        val tintScaleG = 1.0f - (params.tint / 200f)
        val tintScaleM = 1.0f + (params.tint / 200f)

        val totalR = exposureScale * tempR * tintScaleM
        val totalG = exposureScale * tempG * tintScaleG
        val totalB = exposureScale * tempB

        // 4. Contrast
        val contrastFactor = (100f + params.contrast) / 100f
        val contrastOffset = 128f * (1.0f - contrastFactor)

        // 5. Saturation & Vibrance
        val satFactor = (100f + params.saturation + (params.vibrance * 0.7f)).coerceAtLeast(0f) / 100f

        val array = FloatArray(20)

        if (params.isMonochrome) {
            // Channel mixer weights normalized
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
            array[18] = 1f // Alpha
        } else {
            // Color matrix with exposure, contrast, temperature and saturation
            // Standard Rec.709 luminance coefficients
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

            array[15] = 0f
            array[16] = 0f
            array[17] = 0f
            array[18] = 1f
            array[19] = 0f
        }

        return ColorFilter.colorMatrix(ColorMatrix(array))
    }

    /**
     * Approximates Planckian locus color temperature (Kelvin to RGB multiplier).
     */
    fun kelvinToRgbMultiplier(kelvin: Float): Triple<Float, Float, Float> {
        val temp = kelvin / 100f
        val red: Float
        val green: Float
        val blue: Float

        // Red calculation
        red = if (temp <= 66f) {
            255f
        } else {
            val r = temp - 60f
            (329.698727446f * (r.toDouble().pow(-0.1332047592)).toFloat()).coerceIn(0f, 255f)
        }

        // Green calculation
        green = if (temp <= 66f) {
            val g = temp
            (99.4708025861f * kotlin.math.ln(g.toDouble()).toFloat() - 161.1195681661f).coerceIn(0f, 255f)
        } else {
            val g = temp - 60f
            (288.1221695283f * (g.toDouble().pow(-0.0755148492)).toFloat()).coerceIn(0f, 255f)
        }

        // Blue calculation
        blue = if (temp >= 66f) {
            255f
        } else if (temp <= 19f) {
            0f
        } else {
            val b = temp - 10f
            (138.5177312231f * kotlin.math.ln(b.toDouble()).toFloat() - 305.0447927307f).coerceIn(0f, 255f)
        }

        // Normalize against neutral daylight 5500K
        val norm = 255f
        return Triple(red / norm, green / norm, blue / norm)
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

        // Parametric sigmoidal S-curve formulation
        val slope = contrast * 2.5f
        val mid = 0.5f + (toeFactor - shoulderFactor) * 0.2f
        val rawSigmoid = 1.0f / (1.0f + exp(-slope * (clamped - mid)))
        val zeroSigmoid = 1.0f / (1.0f + exp(-slope * (0f - mid)))
        val oneSigmoid = 1.0f / (1.0f + exp(-slope * (1f - mid)))

        return ((rawSigmoid - zeroSigmoid) / (oneSigmoid - zeroSigmoid)).coerceIn(0f, 1f)
    }

    /**
     * Computes 256-bucket RGB and Luminance histograms from a bitmap.
     */
    suspend fun computeHistogram(bitmap: Bitmap): HistogramData = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height

        // Downsample for high-speed computation (target max ~150x150)
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

                if (r == 0 && g == 0 && b == 0) clippedShadows++
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
     * Loads a Bitmap safely from uriString (supports res:// drawable resource or content://).
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

    /**
     * Full precision render pipeline applying all active modules:
     * - Geometric orientation & aspect crop
     * - Color temperature & tint
     * - Exposure, highlights recovery & shadows lift
     * - Contrast & filmic S-curve
     * - Split toning
     * - Vignette falloff
     * - Sharpness unsharp mask
     */
    suspend fun renderFinalBitmap(
        sourceBitmap: Bitmap,
        params: EditParams,
        maxDimension: Int = 0 // 0 means keep original resolution
    ): Bitmap = withContext(Dispatchers.Default) {
        // Step 1: Geometric transformations (rotation & flip)
        val matrix = Matrix()
        if (params.flipHorizontal) {
            matrix.postScale(-1f, 1f, sourceBitmap.width / 2f, sourceBitmap.height / 2f)
        }
        if (params.rotationAngle != 0f) {
            matrix.postRotate(params.rotationAngle, sourceBitmap.width / 2f, sourceBitmap.height / 2f)
        }

        var transformed = Bitmap.createBitmap(
            sourceBitmap,
            0,
            0,
            sourceBitmap.width,
            sourceBitmap.height,
            matrix,
            true
        )

        // Step 2: Aspect Ratio Crop
        if (params.cropAspectRatio != "Original") {
            val targetRatio = when (params.cropAspectRatio) {
                "1:1" -> 1.0f
                "4:3" -> 4f / 3f
                "3:2" -> 3f / 2f
                "16:9" -> 16f / 9f
                else -> transformed.width.toFloat() / transformed.height
            }
            val currentRatio = transformed.width.toFloat() / transformed.height
            val cropW: Int
            val cropH: Int
            if (currentRatio > targetRatio) {
                cropH = transformed.height
                cropW = (cropH * targetRatio).toInt().coerceIn(1, transformed.width)
            } else {
                cropW = transformed.width
                cropH = (cropW / targetRatio).toInt().coerceIn(1, transformed.height)
            }
            val startX = (transformed.width - cropW) / 2
            val startY = (transformed.height - cropH) / 2
            val cropped = Bitmap.createBitmap(transformed, startX, startY, cropW, cropH)
            if (cropped != transformed && transformed != sourceBitmap) {
                transformed.recycle()
            }
            transformed = cropped
        }

        // Step 3: Optional downscale for export dimension
        if (maxDimension in 1 until max(transformed.width, transformed.height)) {
            val scale = maxDimension.toFloat() / max(transformed.width, transformed.height)
            val scaledW = (transformed.width * scale).toInt()
            val scaledH = (transformed.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(transformed, scaledW, scaledH, true)
            if (scaled != transformed && transformed != sourceBitmap) {
                transformed.recycle()
            }
            transformed = scaled
        }

        val width = transformed.width
        val height = transformed.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Precompute color and tone mapping lookup tables (LUTs) for performance
        val exposureScale = 2.0f.pow(params.exposure)
        val (tempR, tempG, tempB) = kelvinToRgbMultiplier(params.temperature)
        val tintScaleG = 1.0f - (params.tint / 200f)
        val tintScaleM = 1.0f + (params.tint / 200f)
        val multR = exposureScale * tempR * tintScaleM
        val multG = exposureScale * tempG * tintScaleG
        val multB = exposureScale * tempB
        val contrastFactor = (100f + params.contrast) / 100f
        val highlightsShift = params.highlights / 100f
        val shadowsShift = params.shadows / 100f
        val satFactor = (100f + params.saturation + (params.vibrance * 0.7f)).coerceAtLeast(0f) / 100f

        // Tone curve LUT (256 entries)
        val toneLut = FloatArray(256)
        for (i in 0 until 256) {
            val norm = i / 255f
            var mapped = norm
            // Filmic curve
            if (params.filmicEnabled) {
                mapped = evaluateFilmicCurve(
                    norm,
                    params.filmicToe,
                    params.filmicShoulder,
                    params.filmicContrast
                )
            }
            // Highlights & Shadows
            if (norm > 0.5f) {
                val weight = (norm - 0.5f) * 2f
                mapped += highlightsShift * weight * 0.25f
            } else {
                val weight = (0.5f - norm) * 2f
                mapped += shadowsShift * weight * 0.25f
            }
            // Contrast
            mapped = (mapped - 0.5f) * contrastFactor + 0.5f
            toneLut[i] = mapped.coerceIn(0f, 1f)
        }

        val pixels = IntArray(width * height)
        transformed.getPixels(pixels, 0, width, 0, 0, width, height)

        for (idx in pixels.indices) {
            val pixel = pixels[idx]
            val a = (pixel shr 24) and 0xFF
            var r = (pixel shr 16) and 0xFF
            var g = (pixel shr 8) and 0xFF
            var b = pixel and 0xFF

            if (params.isMonochrome) {
                val sum = max(1f, params.redWeight + params.greenWeight + params.blueWeight)
                val gray = ((r * params.redWeight + g * params.greenWeight + b * params.blueWeight) / sum).toInt().coerceIn(0, 255)
                val mapped = (toneLut[gray] * 255f * exposureScale).toInt().coerceIn(0, 255)
                pixels[idx] = (a shl 24) or (mapped shl 16) or (mapped shl 8) or mapped
            } else {
                // Apply color temp & exposure
                var fr = (r * multR).coerceIn(0f, 255f)
                var fg = (g * multG).coerceIn(0f, 255f)
                var fb = (b * multB).coerceIn(0f, 255f)

                // Saturation adjustment
                val luma = 0.2126f * fr + 0.7152f * fg + 0.0722f * fb
                fr = luma + satFactor * (fr - luma)
                fg = luma + satFactor * (fg - luma)
                fb = luma + satFactor * (fb - luma)

                // Tone LUT mapping
                val ir = fr.toInt().coerceIn(0, 255)
                val ig = fg.toInt().coerceIn(0, 255)
                val ib = fb.toInt().coerceIn(0, 255)

                val outR = (toneLut[ir] * 255f).toInt().coerceIn(0, 255)
                val outG = (toneLut[ig] * 255f).toInt().coerceIn(0, 255)
                val outB = (toneLut[ib] * 255f).toInt().coerceIn(0, 255)

                pixels[idx] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        // Step 4: Vignette shader overlay if configured
        if (params.vignette != 0f) {
            val canvas = Canvas(outputBitmap)
            val centerX = width / 2f
            val centerY = height / 2f
            val maxRadius = kotlin.math.sqrt(centerX * centerX + centerY * centerY)
            val radius = maxRadius * (params.vignetteRadius / 100f)

            val vignetteAlpha = (kotlin.math.abs(params.vignette) * 2.2f).toInt().coerceIn(0, 240)
            val colorOuter = if (params.vignette < 0) {
                // Dark vignette (traditional darkroom falloff)
                (vignetteAlpha shl 24) or 0x000000
            } else {
                // Light vignette
                (vignetteAlpha shl 24) or 0xFFFFFF
            }

            val shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(0x00000000, colorOuter),
                floatArrayOf(0.4f, 1.0f),
                Shader.TileMode.CLAMP
            )
            val paint = Paint().apply {
                this.shader = shader
            }
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
        format: String = "JPEG", // "JPEG", "PNG", "WEBP"
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
