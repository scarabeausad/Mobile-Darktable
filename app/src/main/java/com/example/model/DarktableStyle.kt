package com.example.model

import java.util.UUID

data class DarktableStyle(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val category: String,
    val isBuiltIn: Boolean = true,
    val params: EditParams
) {
    companion object {
        val BUILT_IN_STYLES = listOf(
            DarktableStyle(
                id = "style_filmic_punch",
                name = "Filmic High Dynamic",
                description = "Modern Darktable Filmic S-curve with deep shadows, lifted highlights, and punchy micro-contrast.",
                category = "Filmic",
                params = EditParams(
                    exposure = 0.25f,
                    contrast = 15f,
                    highlights = -20f,
                    shadows = 25f,
                    filmicEnabled = true,
                    filmicToe = 45f,
                    filmicShoulder = 50f,
                    filmicContrast = 1.35f,
                    vibrance = 18f,
                    sharpness = 25f,
                    vignette = -15f
                )
            ),
            DarktableStyle(
                id = "style_kodachrome",
                name = "Kodachrome 64",
                description = "Emulates vintage color reversal slide film with rich reds, warm golden highlights, and classic grain.",
                category = "Film Emulation",
                params = EditParams(
                    exposure = -0.1f,
                    contrast = 20f,
                    temperature = 5900f,
                    tint = 5f,
                    saturation = 12f,
                    vibrance = 15f,
                    splitToningEnabled = true,
                    shadowHue = 220f,
                    shadowSat = 10f,
                    highlightHue = 45f,
                    highlightSat = 20f,
                    splitBalance = 10f,
                    grain = 30f,
                    vignette = -20f
                )
            ),
            DarktableStyle(
                id = "style_classic_bw",
                name = "Classic B&W Tri-X 400",
                description = "High-contrast monochrome with silver halide film grain and customized spectral sensitivity.",
                category = "Monochrome",
                params = EditParams(
                    isMonochrome = true,
                    redWeight = 45f,
                    greenWeight = 40f,
                    blueWeight = 15f,
                    contrast = 35f,
                    highlights = -15f,
                    shadows = -10f,
                    filmicEnabled = true,
                    filmicToe = 55f,
                    filmicShoulder = 45f,
                    filmicContrast = 1.4f,
                    sharpness = 30f,
                    grain = 40f,
                    vignette = -25f
                )
            ),
            DarktableStyle(
                id = "style_golden_hour",
                name = "Golden Hour Glow",
                description = "Infuses warm sunlight tones, softened highlights, and radiant amber warmth.",
                category = "Atmosphere",
                params = EditParams(
                    exposure = 0.3f,
                    temperature = 6800f,
                    tint = -8f,
                    highlights = -30f,
                    shadows = 20f,
                    vibrance = 25f,
                    saturation = 10f,
                    splitToningEnabled = true,
                    shadowHue = 35f,
                    shadowSat = 15f,
                    highlightHue = 42f,
                    highlightSat = 35f,
                    vignette = -18f
                )
            ),
            DarktableStyle(
                id = "style_nordic_mood",
                name = "Nordic Moody Cold",
                description = "Desaturated cool aesthetic with deep cyan/blue shadows and muted earthy tones.",
                category = "Moody",
                params = EditParams(
                    exposure = -0.2f,
                    temperature = 4700f,
                    tint = 12f,
                    contrast = 15f,
                    highlights = -10f,
                    shadows = -15f,
                    saturation = -20f,
                    vibrance = 10f,
                    splitToningEnabled = true,
                    shadowHue = 205f,
                    shadowSat = 25f,
                    highlightHue = 50f,
                    highlightSat = 8f,
                    sharpness = 20f,
                    vignette = -30f
                )
            ),
            DarktableStyle(
                id = "style_teal_orange",
                name = "Cinematic Teal & Orange",
                description = "Hollywood color grading contrast between complementary warm skin tones and deep teal shadows.",
                category = "Cinematic",
                params = EditParams(
                    contrast = 20f,
                    temperature = 5300f,
                    vibrance = 20f,
                    saturation = 15f,
                    splitToningEnabled = true,
                    shadowHue = 195f,
                    shadowSat = 40f,
                    highlightHue = 35f,
                    highlightSat = 35f,
                    splitBalance = -5f,
                    filmicEnabled = true,
                    filmicContrast = 1.25f,
                    vignette = -20f
                )
            ),
            DarktableStyle(
                id = "style_landscape_vivid",
                name = "Ektachrome Vivid Nature",
                description = "Rich foliage greens, deep skies, and crisp clarity for landscape and nature photography.",
                category = "Landscape",
                params = EditParams(
                    exposure = 0.15f,
                    contrast = 18f,
                    highlights = -25f,
                    shadows = 20f,
                    saturation = 22f,
                    vibrance = 30f,
                    sharpness = 35f,
                    filmicEnabled = true,
                    filmicToe = 30f,
                    filmicShoulder = 35f,
                    filmicContrast = 1.2f
                )
            ),
            DarktableStyle(
                id = "style_portrait_soft",
                name = "Studio Portrait Bloom",
                description = "Flattering portrait rendering with gentle skin tones, recovered highlights, and subtle vignette.",
                category = "Portrait",
                params = EditParams(
                    exposure = 0.2f,
                    contrast = -8f,
                    highlights = -20f,
                    shadows = 15f,
                    temperature = 5650f,
                    tint = 4f,
                    vibrance = 12f,
                    saturation = -5f,
                    sharpness = 12f,
                    vignette = -15f,
                    vignetteRadius = 75f
                )
            )
        )
    }
}
