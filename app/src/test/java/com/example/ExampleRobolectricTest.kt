package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.engine.DarktableEngine
import com.example.model.ColorFilterMode
import com.example.model.ColorLabel
import com.example.model.EditParams
import com.example.model.HistogramScale
import com.example.model.PhotoItem
import com.example.model.RatingComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `main activity launches and seeds catalog without crashing`() {
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        composeTestRule.waitForIdle()
    }

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Darktable", appName)
    }

    @Test
    fun `filmic tone curve evaluates smoothly between 0 and 1`() {
        val y0 = DarktableEngine.evaluateFilmicCurve(0f)
        val yMid = DarktableEngine.evaluateFilmicCurve(0.5f)
        val y1 = DarktableEngine.evaluateFilmicCurve(1f)

        assertEquals(0f, y0, 0.01f)
        assertTrue("Midpoint must be between 0 and 1", yMid in 0.1f..0.9f)
        assertEquals(1f, y1, 0.01f)
    }

    @Test
    fun `edit params serialization and deserialization roundtrip`() {
        val original = EditParams(
            exposure = 1.25f,
            contrast = 25f,
            temperature = 6200f,
            filmicToe = 45f,
            isMonochrome = true
        )
        val json = original.toJson()
        val restored = EditParams.fromJson(json)

        assertEquals(original.exposure, restored.exposure, 0.001f)
        assertEquals(original.contrast, restored.contrast, 0.001f)
        assertEquals(original.temperature, restored.temperature, 0.001f)
        assertEquals(original.filmicToe, restored.filmicToe, 0.001f)
        assertEquals(original.isMonochrome, restored.isMonochrome)
    }

    @Test
    fun `star rating filtering with exact versus less-equal comparator`() {
        val p1 = PhotoItem(title = "Photo 1", uriString = "", rating = 1)
        val p2 = PhotoItem(title = "Photo 2", uriString = "", rating = 3)
        val p3 = PhotoItem(title = "Photo 3", uriString = "", rating = 3)
        val p4 = PhotoItem(title = "Photo 4", uriString = "", rating = 5)
        val list = listOf(p1, p2, p3, p4)

        // 1. Exact 3 stars only
        val exact3 = list.filter { photo ->
            when (RatingComparator.EXACT) {
                RatingComparator.EXACT -> photo.rating == 3
                RatingComparator.LESS_EQUAL -> photo.rating <= 3
                RatingComparator.GREATER_EQUAL -> photo.rating >= 3
            }
        }
        assertEquals(2, exact3.size)
        assertTrue(exact3.all { it.rating == 3 })

        // 2. 3 stars and less
        val lessEqual3 = list.filter { photo ->
            when (RatingComparator.LESS_EQUAL) {
                RatingComparator.EXACT -> photo.rating == 3
                RatingComparator.LESS_EQUAL -> photo.rating <= 3
                RatingComparator.GREATER_EQUAL -> photo.rating >= 3
            }
        }
        assertEquals(3, lessEqual3.size)
        assertTrue(lessEqual3.all { it.rating <= 3 })
        assertFalse(lessEqual3.contains(p4)) // 5 stars excluded
    }

    @Test
    fun `color tag filtering with at-least-one versus only-selected modes`() {
        val pRedOnly = PhotoItem(title = "Red Only", uriString = "", colorLabels = setOf(ColorLabel.RED))
        val pRedBlue = PhotoItem(title = "Red and Blue", uriString = "", colorLabels = setOf(ColorLabel.RED, ColorLabel.BLUE))
        val pYellow = PhotoItem(title = "Yellow Only", uriString = "", colorLabels = setOf(ColorLabel.YELLOW))
        val pNoColor = PhotoItem(title = "No Color", uriString = "", colorLabels = emptySet())
        val list = listOf(pRedOnly, pRedBlue, pYellow, pNoColor)

        val selectedColors = setOf(ColorLabel.RED)

        // 1. At least one of selected (Any)
        val atLeastOne = list.filter { photo ->
            photo.colorLabels.any { it in selectedColors }
        }
        assertEquals(2, atLeastOne.size) // pRedOnly and pRedBlue both have Red
        assertTrue(atLeastOne.contains(pRedOnly))
        assertTrue(atLeastOne.contains(pRedBlue))

        // 2. Only the colors selected (Strict)
        val onlySelected = list.filter { photo ->
            photo.colorLabels.isNotEmpty() && photo.colorLabels.all { it in selectedColors }
        }
        assertEquals(1, onlySelected.size) // Only pRedOnly has strictly RED (pRedBlue has Blue which is not selected)
        assertTrue(onlySelected.contains(pRedOnly))
        assertFalse(onlySelected.contains(pRedBlue))
    }

    @Test
    fun `xmp sidecar xml generation and parsing roundtrip preserves edits and metadata`() {
        val params = EditParams(
            exposure = 0.85f,
            contrast = 18f,
            highlights = -45f,
            shadows = 35f,
            blackPoint = 12f,
            temperature = 6400f,
            tint = -8f,
            saturation = 15f,
            vibrance = 22f,
            filmicEnabled = true,
            filmicToe = 42f,
            filmicShoulder = 55f,
            filmicContrast = 1.3f,
            sharpness = 45f,
            grain = 20f,
            vignette = -30f,
            vignetteRadius = 65f,
            isMonochrome = false,
            rotationAngle = 4.5f,
            flipHorizontal = true,
            cropAspectRatio = "16:9"
        )
        val photo = PhotoItem(
            id = "test_xmp_01",
            uriString = "file:///data/user/0/com.example/files/imported_photos/dt_123.jpg",
            title = "Mountain Sunset & Ridge",
            rating = 4,
            isRejected = false,
            colorLabels = setOf(ColorLabel.RED, ColorLabel.BLUE),
            tags = listOf("Landscape", "Sunset"),
            params = params
        )

        val xml = com.example.engine.XmpSidecarManager.generateXmpXml(photo)
        assertTrue(xml.contains("x:xmpmeta"))
        assertTrue(xml.contains("darktable:active_params"))

        val parsed = com.example.engine.XmpSidecarManager.parseXmpXml(xml)
        requireNotNull(parsed)
        assertEquals(4, parsed.rating)
        assertFalse(parsed.isRejected)
        assertEquals(setOf(ColorLabel.RED, ColorLabel.BLUE), parsed.colorLabels)
        assertEquals(listOf("Landscape", "Sunset"), parsed.tags)
        assertEquals(0.85f, parsed.params.exposure, 0.001f)
        assertEquals(-45f, parsed.params.highlights, 0.001f)
        assertEquals(35f, parsed.params.shadows, 0.001f)
        assertEquals(12f, parsed.params.blackPoint, 0.001f)
        assertEquals("16:9", parsed.params.cropAspectRatio)
        assertTrue(parsed.params.flipHorizontal)
        assertEquals(4.5f, parsed.params.rotationAngle, 0.001f)
    }

    @Test
    fun `exposure tone lut responds to highlights, shadows, and black point`() {
        val neutral = EditParams(filmicEnabled = false)

        val highRecovered = EditParams(filmicEnabled = false, highlights = -80f)
        assertTrue(
            "Negative highlights should pull bright tones down",
            DarktableEngine.evaluateToneLut(0.85f, highRecovered) < DarktableEngine.evaluateToneLut(0.85f, neutral)
        )

        val shadowLifted = EditParams(filmicEnabled = false, shadows = 80f)
        assertTrue(
            "Positive shadows should lift dark tones up",
            DarktableEngine.evaluateToneLut(0.20f, shadowLifted) > DarktableEngine.evaluateToneLut(0.20f, neutral)
        )

        val deepenedBlacks = EditParams(filmicEnabled = false, blackPoint = 35f)
        assertTrue(
            "Positive black point should deepen dark tones",
            DarktableEngine.evaluateToneLut(0.15f, deepenedBlacks) < DarktableEngine.evaluateToneLut(0.15f, neutral)
        )
    }

    @Test
    fun `crop rect normalized computes accurate aspect frames`() {
        // 4:3 image cropped to 1:1 should trim left/right evenly
        val squareRect = DarktableEngine.computeCropRectNormalized(4000f, 3000f, "1:1")
        assertEquals(0.125f, squareRect[0], 0.001f)
        assertEquals(0f, squareRect[1], 0.001f)
        assertEquals(0.875f, squareRect[2], 0.001f)
        assertEquals(1f, squareRect[3], 0.001f)

        // 4:3 image cropped to 16:9 should trim top/bottom evenly
        val wideRect = DarktableEngine.computeCropRectNormalized(4000f, 3000f, "16:9")
        assertEquals(0f, wideRect[0], 0.001f)
        assertTrue(wideRect[1] > 0.1f)
        assertEquals(1f, wideRect[2], 0.001f)
        assertTrue(wideRect[3] < 0.9f)
    }

    @Test
    fun `crop frame move and resize preserve bounds and aspect ratio`() {
        // 1. Move crop frame within image bounds without altering size
        val initial = floatArrayOf(0.2f, 0.2f, 0.6f, 0.7f)
        val moved = DarktableEngine.moveCropRect(initial, 0.1f, -0.05f)
        assertEquals(0.3f, moved[0], 0.001f)
        assertEquals(0.15f, moved[1], 0.001f)
        assertEquals(0.7f, moved[2], 0.001f)
        assertEquals(0.65f, moved[3], 0.001f)

        // Clamped move past right edge keeps exact width & height
        val clamped = DarktableEngine.moveCropRect(initial, 0.8f, 0f)
        assertEquals(0.6f, clamped[0], 0.001f)
        assertEquals(1.0f, clamped[2], 0.001f)

        // 2. Freeform resize from bottom-right corner
        val freeResized = DarktableEngine.resizeCropRect(
            rect = floatArrayOf(0.1f, 0.1f, 0.8f, 0.8f),
            handle = DarktableEngine.CropHandle.BOTTOM_RIGHT,
            dxNorm = -0.2f,
            dyNorm = -0.1f,
            imageWidth = 4000f,
            imageHeight = 3000f,
            cropAspectRatio = "Free"
        )
        assertEquals(0.1f, freeResized[0], 0.001f)
        assertEquals(0.1f, freeResized[1], 0.001f)
        assertEquals(0.6f, freeResized[2], 0.001f)
        assertEquals(0.7f, freeResized[3], 0.001f)

        // 3. Aspect-locked 1:1 resize keeps pixel width == pixel height on 4000x3000 image
        val squareStart = DarktableEngine.computeCropRectNormalized(4000f, 3000f, "1:1")
        val squareResized = DarktableEngine.resizeCropRect(
            rect = squareStart,
            handle = DarktableEngine.CropHandle.TOP_LEFT,
            dxNorm = 0.15f,
            dyNorm = 0.10f,
            imageWidth = 4000f,
            imageHeight = 3000f,
            cropAspectRatio = "1:1"
        )
        val pxW = (squareResized[2] - squareResized[0]) * 4000f
        val pxH = (squareResized[3] - squareResized[1]) * 3000f
        assertEquals(pxW, pxH, 1.0f)
    }

    @Test
    fun `custom crop bounds roundtrip through xmp sidecar`() {
        val params = EditParams(
            cropAspectRatio = "Free",
            cropLeft = 0.15f,
            cropTop = 0.20f,
            cropRight = 0.82f,
            cropBottom = 0.78f
        )
        val photo = PhotoItem(
            id = "test_crop_xmp",
            uriString = "res://sample_mountain_lake",
            title = "Custom Crop Test",
            params = params
        )
        val xml = com.example.engine.XmpSidecarManager.generateXmpXml(photo)
        val parsed = com.example.engine.XmpSidecarManager.parseXmpXml(xml)
        requireNotNull(parsed)
        assertEquals("Free", parsed.params.cropAspectRatio)
        assertEquals(0.15f, parsed.params.cropLeft, 0.001f)
        assertEquals(0.20f, parsed.params.cropTop, 0.001f)
        assertEquals(0.82f, parsed.params.cropRight, 0.001f)
        assertEquals(0.78f, parsed.params.cropBottom, 0.001f)
    }

    @Test
    fun `histogram scale linear versus logarithmic normalization`() {
        // Low bin count (10 out of 1000) should be 0.01 linearly, but visibly lifted logarithmically
        val linearLow = HistogramScale.LINEAR.normalizeBin(count = 10, maxCount = 1000)
        val logLow = HistogramScale.LOGARITHMIC.normalizeBin(count = 10, maxCount = 1000)

        assertEquals(0.01f, linearLow, 0.0001f)
        assertTrue("Logarithmic scale must amplify low-count shadow/highlight detail", logLow > 0.30f)

        // Peak bin (1000 out of 1000) and zero bin (0) should map to 1.0 and 0.0 in both modes
        assertEquals(1.0f, HistogramScale.LINEAR.normalizeBin(1000, 1000), 0.0001f)
        assertEquals(1.0f, HistogramScale.LOGARITHMIC.normalizeBin(1000, 1000), 0.0001f)
        assertEquals(0.0f, HistogramScale.LINEAR.normalizeBin(0, 1000), 0.0001f)
        assertEquals(0.0f, HistogramScale.LOGARITHMIC.normalizeBin(0, 1000), 0.0001f)
    }

    @Test
    fun `clicking settings icon opens application settings with theme and histogram options`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_open_settings").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("APPLICATION SETTINGS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("option_color_scheme_dark").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("option_color_scheme_light").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("option_color_scheme_system").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("option_histogram_scale_linear").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("option_histogram_scale_logarithmic").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("option_color_scheme_light").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("option_histogram_scale_logarithmic").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("histogram_view_logarithmic")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("histogram_view_logarithmic").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `edited thumbnail pipeline applies monochrome and crop for overview cards`() = kotlinx.coroutines.runBlocking {
        val loadedSample = DarktableEngine.loadBitmap(
            ApplicationProvider.getApplicationContext(),
            "res://sample_mountain_lake",
            420
        )
        assertTrue("Sample drawable must decode cleanly", loadedSample != null && loadedSample.width > 0)

        val vm = com.example.viewmodel.DarktableViewModel(ApplicationProvider.getApplicationContext())
        val vmPhoto = PhotoItem(
            id = "sample_lake_01",
            uriString = "res://sample_mountain_lake",
            title = "Test Lake",
            params = EditParams(isMonochrome = true, cropAspectRatio = "1:1")
        )
        val vmRendered = vm.getOrRenderEditedThumbnail(vmPhoto)
        assertTrue(
            "ViewModel getOrRenderEditedThumbnail must succeed (loaded=${loadedSample!!.width}x${loadedSample.height})",
            vmRendered != null && vmRendered.width > 0 && vmRendered.height > 0
        )

        val renderedSample = DarktableEngine.renderPreviewBitmap(loadedSample, EditParams(isMonochrome = true, cropAspectRatio = "1:1"), applyCrop = true)
        assertTrue("Rendered sample thumbnail must be valid", renderedSample.width > 0 && renderedSample.height > 0)

        val src = android.graphics.Bitmap.createBitmap(200, 100, android.graphics.Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(220, 40, 20))
        }
        val params = EditParams(
            isMonochrome = true,
            cropAspectRatio = "1:1",
            cropLeft = 0.25f,
            cropTop = 0f,
            cropRight = 0.75f,
            cropBottom = 1f
        )

        val thumb = DarktableEngine.renderPreviewBitmap(src, params, applyCrop = true)
        assertEquals(100, thumb.width)
        assertEquals(100, thumb.height)

        val centerPixel = thumb.getPixel(50, 50)
        val r = android.graphics.Color.red(centerPixel)
        val g = android.graphics.Color.green(centerPixel)
        val b = android.graphics.Color.blue(centerPixel)
        assertEquals("Monochrome edit must desaturate pixel channels equally", r, g)
        assertEquals("Monochrome edit must desaturate pixel channels equally", g, b)
    }

    @Test
    fun `edits applied in darkroom render edited thumbnail on lighttable overview`() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("photo_card_sample_lake_01")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("photo_card_sample_lake_01").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("tab_geometry")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Apply a 1:1 crop in Darkroom
        composeTestRule.onNodeWithTag("tab_geometry").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("crop_ratio_1_1").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Return to Lighttable overview
        composeTestRule.onNodeWithTag("btn_back_to_lighttable").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("photo_card_sample_lake_01")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("photo_edited_thumbnail_sample_lake_01", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("photo_edited_thumbnail_sample_lake_01", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}


