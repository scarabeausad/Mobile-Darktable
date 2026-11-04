package com.example.model

import org.json.JSONObject

data class EditParams(
    // Exposure & Tone
    val exposure: Float = 0f,          // EV: -3.0 to +3.0
    val contrast: Float = 0f,          // -100 to +100
    val highlights: Float = 0f,        // -100 to +100
    val shadows: Float = 0f,           // -100 to +100
    val blackPoint: Float = 0f,        // -100 to +100

    // Color & White Balance
    val temperature: Float = 5500f,    // Kelvin: 2000K to 10000K
    val tint: Float = 0f,              // -100 to +100 (Green to Magenta)
    val saturation: Float = 0f,        // -100 to +100
    val vibrance: Float = 0f,          // -100 to +100

    // Filmic RGB Module
    val filmicEnabled: Boolean = true,
    val filmicToe: Float = 35f,        // 0 to 100
    val filmicShoulder: Float = 40f,   // 0 to 100
    val filmicContrast: Float = 1.15f, // 0.5 to 2.5

    // Tone Curve
    val curveBlacks: Float = 0f,       // -50 to +50
    val curveShadows: Float = 0f,      // -50 to +50
    val curveMidtones: Float = 0f,     // -50 to +50
    val curveHighlights: Float = 0f,   // -50 to +50
    val curveWhites: Float = 0f,       // -50 to +50

    // Split Toning
    val splitToningEnabled: Boolean = false,
    val shadowHue: Float = 215f,       // 0 to 360 deg
    val shadowSat: Float = 0f,         // 0 to 100
    val highlightHue: Float = 40f,     // 0 to 360 deg
    val highlightSat: Float = 0f,      // 0 to 100
    val splitBalance: Float = 0f,      // -100 to +100

    // Detail & Effects
    val sharpness: Float = 10f,        // 0 to 100
    val grain: Float = 0f,             // 0 to 100
    val vignette: Float = 0f,          // -100 to +100
    val vignetteRadius: Float = 60f,   // 10 to 100

    // Monochrome & Channel Mixer
    val isMonochrome: Boolean = false,
    val redWeight: Float = 30f,        // 0 to 100
    val greenWeight: Float = 59f,      // 0 to 100
    val blueWeight: Float = 11f,       // 0 to 100

    // Geometry
    val rotationAngle: Float = 0f,     // -45 to +45 deg
    val flipHorizontal: Boolean = false,
    val cropAspectRatio: String = "Original" // "Original", "1:1", "4:3", "3:2", "16:9"
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("exposure", exposure.toDouble())
        json.put("contrast", contrast.toDouble())
        json.put("highlights", highlights.toDouble())
        json.put("shadows", shadows.toDouble())
        json.put("blackPoint", blackPoint.toDouble())
        json.put("temperature", temperature.toDouble())
        json.put("tint", tint.toDouble())
        json.put("saturation", saturation.toDouble())
        json.put("vibrance", vibrance.toDouble())
        json.put("filmicEnabled", filmicEnabled)
        json.put("filmicToe", filmicToe.toDouble())
        json.put("filmicShoulder", filmicShoulder.toDouble())
        json.put("filmicContrast", filmicContrast.toDouble())
        json.put("curveBlacks", curveBlacks.toDouble())
        json.put("curveShadows", curveShadows.toDouble())
        json.put("curveMidtones", curveMidtones.toDouble())
        json.put("curveHighlights", curveHighlights.toDouble())
        json.put("curveWhites", curveWhites.toDouble())
        json.put("splitToningEnabled", splitToningEnabled)
        json.put("shadowHue", shadowHue.toDouble())
        json.put("shadowSat", shadowSat.toDouble())
        json.put("highlightHue", highlightHue.toDouble())
        json.put("highlightSat", highlightSat.toDouble())
        json.put("splitBalance", splitBalance.toDouble())
        json.put("sharpness", sharpness.toDouble())
        json.put("grain", grain.toDouble())
        json.put("vignette", vignette.toDouble())
        json.put("vignetteRadius", vignetteRadius.toDouble())
        json.put("isMonochrome", isMonochrome)
        json.put("redWeight", redWeight.toDouble())
        json.put("greenWeight", greenWeight.toDouble())
        json.put("blueWeight", blueWeight.toDouble())
        json.put("rotationAngle", rotationAngle.toDouble())
        json.put("flipHorizontal", flipHorizontal)
        json.put("cropAspectRatio", cropAspectRatio)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String?): EditParams {
            if (jsonStr.isNullOrEmpty()) return EditParams()
            return try {
                val json = JSONObject(jsonStr)
                EditParams(
                    exposure = json.optDouble("exposure", 0.0).toFloat(),
                    contrast = json.optDouble("contrast", 0.0).toFloat(),
                    highlights = json.optDouble("highlights", 0.0).toFloat(),
                    shadows = json.optDouble("shadows", 0.0).toFloat(),
                    blackPoint = json.optDouble("blackPoint", 0.0).toFloat(),
                    temperature = json.optDouble("temperature", 5500.0).toFloat(),
                    tint = json.optDouble("tint", 0.0).toFloat(),
                    saturation = json.optDouble("saturation", 0.0).toFloat(),
                    vibrance = json.optDouble("vibrance", 0.0).toFloat(),
                    filmicEnabled = json.optBoolean("filmicEnabled", true),
                    filmicToe = json.optDouble("filmicToe", 35.0).toFloat(),
                    filmicShoulder = json.optDouble("filmicShoulder", 40.0).toFloat(),
                    filmicContrast = json.optDouble("filmicContrast", 1.15).toFloat(),
                    curveBlacks = json.optDouble("curveBlacks", 0.0).toFloat(),
                    curveShadows = json.optDouble("curveShadows", 0.0).toFloat(),
                    curveMidtones = json.optDouble("curveMidtones", 0.0).toFloat(),
                    curveHighlights = json.optDouble("curveHighlights", 0.0).toFloat(),
                    curveWhites = json.optDouble("curveWhites", 0.0).toFloat(),
                    splitToningEnabled = json.optBoolean("splitToningEnabled", false),
                    shadowHue = json.optDouble("shadowHue", 215.0).toFloat(),
                    shadowSat = json.optDouble("shadowSat", 0.0).toFloat(),
                    highlightHue = json.optDouble("highlightHue", 40.0).toFloat(),
                    highlightSat = json.optDouble("highlightSat", 0.0).toFloat(),
                    splitBalance = json.optDouble("splitBalance", 0.0).toFloat(),
                    sharpness = json.optDouble("sharpness", 10.0).toFloat(),
                    grain = json.optDouble("grain", 0.0).toFloat(),
                    vignette = json.optDouble("vignette", 0.0).toFloat(),
                    vignetteRadius = json.optDouble("vignetteRadius", 60.0).toFloat(),
                    isMonochrome = json.optBoolean("isMonochrome", false),
                    redWeight = json.optDouble("redWeight", 30.0).toFloat(),
                    greenWeight = json.optDouble("greenWeight", 59.0).toFloat(),
                    blueWeight = json.optDouble("blueWeight", 11.0).toFloat(),
                    rotationAngle = json.optDouble("rotationAngle", 0.0).toFloat(),
                    flipHorizontal = json.optBoolean("flipHorizontal", false),
                    cropAspectRatio = json.optString("cropAspectRatio", "Original")
                )
            } catch (e: Exception) {
                EditParams()
            }
        }
    }
}
