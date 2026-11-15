package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.engine.DarktableEngine
import com.example.model.ColorFilterMode
import com.example.model.ColorLabel
import com.example.model.EditParams
import com.example.model.HistogramScale
import com.example.model.PhotoItem
import com.example.model.RATING_REJECTED
import com.example.model.RatingComparator
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.matchesRatingFilter
import com.example.model.sortedByOption
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

    @Test
    fun `sibling xmp discovery and filename pairing supports dot-ext-xmp and stem-xmp`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = java.io.File(context.cacheDir, "xmp_discovery_${System.nanoTime()}").apply { mkdirs() }
        try {
            val img1 = java.io.File(tempDir, "alpine.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val xmp1 = java.io.File(tempDir, "alpine.jpg.xmp").apply { writeText("<x:xmpmeta/>") }
            assertEquals(xmp1.absolutePath, com.example.engine.XmpSidecarManager.findSiblingSidecarFile(img1)?.absolutePath)

            val img2 = java.io.File(tempDir, "sunset.dng").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val xmp2 = java.io.File(tempDir, "sunset.xmp").apply { writeText("<x:xmpmeta/>") }
            assertEquals(xmp2.absolutePath, com.example.engine.XmpSidecarManager.findSiblingSidecarFile(img2)?.absolutePath)

            val matched = com.example.engine.XmpSidecarManager.findMatchingXmpName(
                "IMG_2026.ARW",
                listOf("other.xmp", "IMG_2026.xmp", "IMG_2026.ARW.xmp")
            )
            assertEquals("IMG_2026.ARW.xmp", matched)

            val embeddedFile = java.io.File(tempDir, "embedded.jpg")
            val embeddedXml = com.example.engine.XmpSidecarManager.generateXmpXml(
                PhotoItem(id = "emb", uriString = "", title = "Emb", params = EditParams(exposure = 0.7f))
            )
            embeddedFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + embeddedXml.toByteArray(Charsets.UTF_8) + byteArrayOf(0xFF.toByte(), 0xD9.toByte()))
            val extracted = com.example.engine.XmpSidecarManager.extractEmbeddedXmpXml(embeddedFile)
            assertTrue("Embedded Darktable XMP XML must be extracted", extracted != null && extracted.contains("exposure=\"0.7\""))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `importing image with sibling xmp file reads edits and renders them on imported image`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = java.io.File(context.cacheDir, "xmp_import_${System.nanoTime()}").apply { mkdirs() }
        try {
            val imgFile = java.io.File(tempDir, "crimson_import.jpg")
            val srcBmp = android.graphics.Bitmap.createBitmap(160, 100, android.graphics.Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.rgb(210, 40, 20))
            }
            java.io.FileOutputStream(imgFile).use { out ->
                srcBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
            }

            val expectedParams = EditParams(
                exposure = 0.65f,
                contrast = 24f,
                isMonochrome = true,
                cropAspectRatio = "1:1",
                cropLeft = 0.1875f,
                cropTop = 0f,
                cropRight = 0.8125f,
                cropBottom = 1f
            )
            val sourcePhoto = PhotoItem(
                id = "external_src",
                uriString = android.net.Uri.fromFile(imgFile).toString(),
                title = "Crimson Peak XMP",
                rating = 5,
                colorLabels = setOf(ColorLabel.RED, ColorLabel.BLUE),
                tags = listOf("Imported", "XMP"),
                params = expectedParams
            )
            val xmpFile = java.io.File(tempDir, "crimson_import.jpg.xmp")
            xmpFile.writeText(com.example.engine.XmpSidecarManager.generateXmpXml(sourcePhoto), Charsets.UTF_8)

            val importedInfo = DarktableEngine.importToInternalStorage(context, android.net.Uri.fromFile(imgFile))
            requireNotNull(importedInfo)
            requireNotNull(importedInfo.xmpSidecarData)
            assertEquals(true, importedInfo.xmpSidecarData!!.params.isMonochrome)
            assertEquals("1:1", importedInfo.xmpSidecarData!!.params.cropAspectRatio)
            assertEquals(0.65f, importedInfo.xmpSidecarData!!.params.exposure, 0.001f)

            val dao = com.example.db.DarktableDatabase.getDatabase(context).darktableDao()
            val repo = com.example.repository.PhotoRepository(dao, context.filesDir)
            val importedItem = repo.importPhotoWithMetadata(importedInfo)

            assertEquals("Crimson Peak XMP", importedItem.title)
            assertEquals(5, importedItem.rating)
            assertEquals(setOf(ColorLabel.RED, ColorLabel.BLUE), importedItem.colorLabels)
            assertTrue(importedItem.params.isMonochrome)
            assertEquals("1:1", importedItem.params.cropAspectRatio)
            assertTrue("History should mark imported XMP as edited", importedItem.history.size > 1)

            val vm = com.example.viewmodel.DarktableViewModel(ApplicationProvider.getApplicationContext())
            val renderedThumb = vm.getOrRenderEditedThumbnail(importedItem)
            requireNotNull(renderedThumb)
            assertEquals("1:1 crop from XMP must produce square overview thumbnail", renderedThumb.width, renderedThumb.height)
            val centerPixel = renderedThumb.getPixel(renderedThumb.width / 2, renderedThumb.height / 2)
            assertEquals(android.graphics.Color.red(centerPixel), android.graphics.Color.green(centerPixel))
            assertEquals(android.graphics.Color.green(centerPixel), android.graphics.Color.blue(centerPixel))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `clicking import fab opens options including files with xmp and folder auto match`() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("fab_import_photo")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("fab_import_photo").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("btn_import_files_with_xmp").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_import_folder_xmp").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_import_gallery").assertIsDisplayed()
    }

    @Test
    fun `matchesRatingFilter handles rejected status across comparators`() {
        val rejectedWithOldStars = PhotoItem(title = "Rejected", uriString = "", rating = 4, isRejected = true)
        val unrated = PhotoItem(title = "Unrated", uriString = "", rating = 0, isRejected = false)
        val threeStars = PhotoItem(title = "Three Stars", uriString = "", rating = 3, isRejected = false)
        val catalog = listOf(rejectedWithOldStars, unrated, threeStars)

        // ALL returns everything including rejected
        assertEquals(3, catalog.count { it.matchesRatingFilter(null, RatingComparator.EXACT) })

        // EXACT Rejected returns only rejected
        val exactRejected = catalog.filter { it.matchesRatingFilter(RATING_REJECTED, RatingComparator.EXACT) }
        assertEquals(listOf(rejectedWithOldStars), exactRejected)

        // LESS_EQUAL Rejected returns only rejected (-1 <= -1)
        val lessEqRejected = catalog.filter { it.matchesRatingFilter(RATING_REJECTED, RatingComparator.LESS_EQUAL) }
        assertEquals(listOf(rejectedWithOldStars), lessEqRejected)

        // GREATER_EQUAL Rejected returns rejected (-1) and >= 0 stars
        val greaterEqRejected = catalog.filter { it.matchesRatingFilter(RATING_REJECTED, RatingComparator.GREATER_EQUAL) }
        assertEquals(3, greaterEqRejected.size)

        // EXACT 4 stars does NOT include rejectedWithOldStars
        val exact4 = catalog.filter { it.matchesRatingFilter(4, RatingComparator.EXACT) }
        assertTrue(exact4.isEmpty())

        // LESS_EQUAL 0 stars includes both rejected (-1) and 0-star unrated (0)
        val lessEq0 = catalog.filter { it.matchesRatingFilter(0, RatingComparator.LESS_EQUAL) }
        assertEquals(listOf(rejectedWithOldStars, unrated), lessEq0)
    }

    @Test
    fun `color filter mode button is on the left before color tags and rejected filter works end to end`() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("photo_card_sample_lake_01")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Row 1 & Row 2 alignment: comparator / mode button on LEFT, options on RIGHT
        val ratingButtonBounds = composeTestRule.onNodeWithTag("btn_cycle_rating_comparator").getBoundsInRoot()
        val ratingAllBounds = composeTestRule.onNodeWithTag("filter_stars_all").getBoundsInRoot()
        assertTrue(
            "Star rating comparator button must be to the left of filter chips",
            ratingButtonBounds.right <= ratingAllBounds.left
        )

        val colorModeBounds = composeTestRule.onNodeWithTag("btn_toggle_color_filter_mode").getBoundsInRoot()
        val colorDotsBounds = composeTestRule.onNodeWithTag("filter_color_labels").getBoundsInRoot()
        assertTrue(
            "Color filter mode button must be to the left of color tag dots to match star rating row",
            colorModeBounds.right <= colorDotsBounds.left
        )

        // Verify Rejected filter chip is visible in star rating row
        composeTestRule.onNodeWithTag("filter_rejected").performScrollTo().assertIsDisplayed()

        // Mark first photo as Rejected using card's reject button
        composeTestRule.onAllNodesWithTag("btn_reject_photo", useUnmergedTree = true)
            .onFirst()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("badge_rejected_sample_lake_01", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("badge_rejected_sample_lake_01", useUnmergedTree = true)
            .assertIsDisplayed()

        // Filter by Rejected in the Star Rating bar
        composeTestRule.onNodeWithTag("filter_rejected").performClick()
        composeTestRule.waitForIdle()

        // Rejected photo stays visible, while non-rejected sample_portrait_02 is filtered out
        composeTestRule.onNodeWithTag("photo_card_sample_lake_01").assertIsDisplayed()
        assertTrue(
            composeTestRule.onAllNodesWithTag("photo_card_sample_portrait_02")
                .fetchSemanticsNodes().isEmpty()
        )
    }

    private fun floatsToLittleEndianHex(intsAndFloats: List<Number>): String {
        val buf = java.nio.ByteBuffer.allocate(intsAndFloats.size * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (v in intsAndFloats) {
            when (v) {
                is Int -> buf.putInt(v)
                else -> buf.putFloat(v.toFloat())
            }
        }
        return buf.array().joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `importing image with desktop darktable hex xmp sidecar applies and renders edits`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = java.io.File(context.cacheDir, "dt_desktop_xmp_${System.nanoTime()}").apply { mkdirs() }
        try {
            val imgFile = java.io.File(tempDir, "DSC0100.jpg")
            val srcBmp = android.graphics.Bitmap.createBitmap(200, 100, android.graphics.Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.rgb(220, 45, 25))
            }
            java.io.FileOutputStream(imgFile).use { out ->
                srcBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
            }

            // Build authentic desktop Darktable XMP with UTF-8 BOM + binary hex params
            val exposureHex = floatsToLittleEndianHex(listOf(0, 0.0f, 1.5f, 0.5f, 0.0f))
            // Crop square center on 200x100: left=0.25, top=0.0, right=0.75, bottom=1.0
            val cropHex = floatsToLittleEndianHex(listOf(0.25f, 0.0f, 0.75f, 1.0f, 1, 1))
            val disabledVignetteHex = floatsToLittleEndianHex(listOf(50.0f, 50.0f, -0.8f))

            val desktopXmp = "\uFEFF" + """
                <?xml version="1.0" encoding="UTF-8"?>
                <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="XMP Core 4.4.0-Exiv2">
                 <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about=""
                    xmlns:exif="http://ns.adobe.com/exif/1.0/"
                    xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                    xmlns:darktable="http://darktable.sf.net/"
                    xmp:Rating="4"
                    darktable:xmp_version="5"
                    darktable:history_end="4">
                   <darktable:colorlabels>
                    <rdf:Seq>
                     <rdf:li>0</rdf:li>
                     <rdf:li>3</rdf:li>
                    </rdf:Seq>
                   </darktable:colorlabels>
                   <darktable:history>
                    <rdf:Seq>
                     <rdf:li darktable:num="0" darktable:operation="exposure" darktable:enabled="1" darktable:modversion="6" darktable:params="$exposureHex"/>
                     <rdf:li darktable:num="1" darktable:operation="crop" darktable:enabled="1" darktable:modversion="1" darktable:params="$cropHex"/>
                     <rdf:li darktable:num="2" darktable:operation="monochrome" darktable:enabled="1" darktable:modversion="2" darktable:params="00000000"/>
                     <rdf:li darktable:num="3" darktable:operation="vignette" darktable:enabled="0" darktable:modversion="4" darktable:params="$disabledVignetteHex"/>
                    </rdf:Seq>
                   </darktable:history>
                  </rdf:Description>
                 </rdf:RDF>
                </x:xmpmeta>
            """.trimIndent()

            java.io.File(tempDir, "DSC0100.jpg.xmp").writeText(desktopXmp, Charsets.UTF_8)

            // Verify extensionless name matching too
            assertEquals(
                "DSC0100.jpg.xmp",
                com.example.engine.XmpSidecarManager.findMatchingXmpName("DSC0100", listOf("other.xmp", "DSC0100.jpg.xmp"))
            )

            val importedInfo = DarktableEngine.importToInternalStorage(context, android.net.Uri.fromFile(imgFile))
            requireNotNull(importedInfo)
            val xmpData = importedInfo.xmpSidecarData
            requireNotNull(xmpData)

            assertEquals(4, xmpData.rating)
            assertEquals(setOf(ColorLabel.RED, ColorLabel.BLUE), xmpData.colorLabels)
            assertEquals(1.5f, xmpData.params.exposure, 0.01f)
            assertTrue(xmpData.params.isMonochrome)
            assertEquals(0.25f, xmpData.params.cropLeft, 0.001f)
            assertEquals(0.75f, xmpData.params.cropRight, 0.001f)
            assertEquals("Disabled vignette step must remain 0", 0f, xmpData.params.vignette, 0.001f)

            val dao = com.example.db.DarktableDatabase.getDatabase(context).darktableDao()
            val repo = com.example.repository.PhotoRepository(dao, context.filesDir)
            val item = repo.importPhotoWithMetadata(importedInfo)

            val vm = com.example.viewmodel.DarktableViewModel(ApplicationProvider.getApplicationContext())
            val rendered = vm.getOrRenderEditedThumbnail(item)
            requireNotNull(rendered)
            assertEquals("Imported desktop XMP crop must yield square output", rendered.width, rendered.height)
            val center = rendered.getPixel(rendered.width / 2, rendered.height / 2)
            assertEquals(android.graphics.Color.red(center), android.graphics.Color.green(center))
            assertEquals(android.graphics.Color.green(center), android.graphics.Color.blue(center))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `parses adobe camera raw crs xmp with multiple rdf description blocks`() {
        val crsXmp = """
            <?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
             <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="" xmlns:xmp="http://ns.adobe.com/xap/1.0/" xmp:Rating="5" xmp:Label="Purple"/>
              <rdf:Description rdf:about=""
                xmlns:crs="http://ns.adobe.com/camera-raw-settings/1.0/"
                crs:Exposure2012="+0.90"
                crs:Contrast2012="+20"
                crs:Temperature="6500"
                crs:ConvertToGrayscale="True"
                crs:HasCrop="True"
                crs:CropLeft="0.1"
                crs:CropTop="0.15"
                crs:CropRight="0.9"
                crs:CropBottom="0.85"/>
             </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        val parsed = com.example.engine.XmpSidecarManager.parseXmpXml(crsXmp)
        requireNotNull(parsed)
        assertEquals(5, parsed.rating)
        assertEquals(setOf(ColorLabel.PURPLE), parsed.colorLabels)
        assertEquals(0.90f, parsed.params.exposure, 0.001f)
        assertEquals(20f, parsed.params.contrast, 0.001f)
        assertEquals(6500f, parsed.params.temperature, 0.001f)
        assertTrue(parsed.params.isMonochrome)
        assertEquals(0.1f, parsed.params.cropLeft, 0.001f)
        assertEquals(0.85f, parsed.params.cropBottom, 0.001f)
    }

    @Test
    fun `exif capture date parsing and photo sorting by filename, exif date, and star rating`() {
        val t1 = DarktableEngine.parseExifDateTimeMillis("2024:06:15 08:30:45", "25", "Z")
        val t2 = DarktableEngine.parseExifDateTimeMillis("2024:06:15 08:30:45", "50", "Z")
        requireNotNull(t1)
        requireNotNull(t2)
        assertEquals(250L, t2 - t1)

        val alphaLow = PhotoItem(
            id = "a",
            uriString = "",
            title = "alpha_frame.ARW",
            captureDate = 2000L,
            rating = 2
        )
        val betaHigh = PhotoItem(
            id = "b",
            uriString = "",
            title = "Beta_frame.ARW",
            captureDate = 1000L,
            rating = 5
        )
        val charlieRejected = PhotoItem(
            id = "c",
            uriString = "",
            title = "charlie.DNG",
            captureDate = 3000L,
            rating = 4,
            isRejected = true
        )
        val items = listOf(betaHigh, charlieRejected, alphaLow)

        // Filename Asc / Desc (case-insensitive)
        assertEquals(
            listOf("a", "b", "c"),
            items.sortedByOption(SortField.FILENAME, SortDirection.ASCENDING).map { it.id }
        )
        assertEquals(
            listOf("c", "b", "a"),
            items.sortedByOption(SortField.FILENAME, SortDirection.DESCENDING).map { it.id }
        )

        // EXIF Capture Date & Time Asc / Desc
        assertEquals(
            listOf("b", "a", "c"),
            items.sortedByOption(SortField.CAPTURE_DATE, SortDirection.ASCENDING).map { it.id }
        )
        assertEquals(
            listOf("c", "a", "b"),
            items.sortedByOption(SortField.CAPTURE_DATE, SortDirection.DESCENDING).map { it.id }
        )

        // Star Rating Asc / Desc (rejected = -1 ranks below 0..5 stars)
        assertEquals(
            listOf("c", "a", "b"),
            items.sortedByOption(SortField.STAR_RATING, SortDirection.ASCENDING).map { it.id }
        )
        assertEquals(
            listOf("b", "a", "c"),
            items.sortedByOption(SortField.STAR_RATING, SortDirection.DESCENDING).map { it.id }
        )
    }

    private fun comesBeforeInGrid(
        firstTag: String,
        secondTag: String
    ): Boolean {
        val b1 = composeTestRule.onNodeWithTag(firstTag).getBoundsInRoot()
        val b2 = composeTestRule.onNodeWithTag(secondTag).getBoundsInRoot()
        return b1.top < b2.top || (b1.top == b2.top && b1.left < b2.left)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `lighttable sort options reorder overview grid by filename, exif date, star rating, and direction`() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("photo_card_sample_lake_01").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithTag("photo_card_sample_desert_04").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Sort bar controls are visible on Overview
        composeTestRule.onNodeWithTag("btn_toggle_sort_direction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sort_field_capture_date").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("sort_field_filename").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("sort_field_star_rating").performScrollTo().assertIsDisplayed()

        // 1. Default EXIF Capture Date Descending: Alpine (newest) comes before Erg Chebbi (oldest)
        assertTrue(
            "Newest sample_lake_01 should precede oldest sample_desert_04 in Capture Date Desc",
            comesBeforeInGrid("photo_card_sample_lake_01", "photo_card_sample_desert_04")
        )

        // 2. Sort by Filename (defaults to Asc): "Editorial..." precedes "Tokyo..."
        composeTestRule.onNodeWithTag("sort_field_filename").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(
            "Filename Asc should place Editorial (sample_portrait_03) before Tokyo (sample_street_02)",
            comesBeforeInGrid("photo_card_sample_portrait_03", "photo_card_sample_street_02")
        )

        // Toggle Direction -> Filename Desc: "Tokyo..." comes first, before "Alpine..."
        composeTestRule.onNodeWithTag("btn_toggle_sort_direction").performClick()
        composeTestRule.waitForIdle()
        assertTrue(
            "Filename Desc should place Tokyo (sample_street_02) before Alpine (sample_lake_01)",
            comesBeforeInGrid("photo_card_sample_street_02", "photo_card_sample_lake_01")
        )

        // 3. Switch to EXIF Capture Date Desc then tap again for Asc (oldest sample_desert_04 first)
        composeTestRule.onNodeWithTag("sort_field_capture_date").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sort_field_capture_date").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(
            "Capture Date Asc should place oldest sample_desert_04 before newest sample_lake_01",
            comesBeforeInGrid("photo_card_sample_desert_04", "photo_card_sample_lake_01")
        )

        // 4. Switch to Star Rating Desc (5★ first), then tap again for Star Rating Asc (3★ before 5★)
        composeTestRule.onNodeWithTag("sort_field_star_rating").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(
            "Star Rating Desc should place 5-star sample_lake_01 before 3-star sample_portrait_03",
            comesBeforeInGrid("photo_card_sample_lake_01", "photo_card_sample_portrait_03")
        )
        composeTestRule.onNodeWithTag("sort_field_star_rating").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(
            "Star Rating Asc should place 3-star sample_portrait_03 before 5-star sample_lake_01",
            comesBeforeInGrid("photo_card_sample_portrait_03", "photo_card_sample_lake_01")
        )

        // Restore default sort order (EXIF Capture Date Desc) for test isolation
        composeTestRule.onNodeWithTag("sort_field_capture_date").performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `launcher intent resolves MainActivity with Darktable label and valid adaptive icon`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            setPackage(context.packageName)
        }
        val resolved = pm.queryIntentActivities(launcherIntent, 0)
        assertEquals("Exactly one launchable activity must be registered", 1, resolved.size)
        val info = resolved.first()
        assertEquals(MainActivity::class.java.name, info.activityInfo.name)
        assertTrue(info.activityInfo.exported)
        assertEquals("Darktable", info.loadLabel(pm).toString())

        val icon = info.loadIcon(pm)
        assertTrue("Launcher icon drawable must load non-null", icon != null)
        val adaptive = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        assertTrue("Adaptive launcher icon drawable must inflate cleanly", adaptive != null)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `darkroom edit mode sliders have generous horizontal spacing from screen edges`() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("photo_card_sample_lake_01")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("photo_card_sample_lake_01").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("slider_exposure_compensation")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val rootBounds = composeTestRule.onRoot().getBoundsInRoot()
        val sliderBounds = composeTestRule.onNodeWithTag("slider_exposure_compensation").getBoundsInRoot()

        val leftGap = sliderBounds.left - rootBounds.left
        val rightGap = rootBounds.right - sliderBounds.right
        assertTrue("Left slider edge gap ($leftGap) must be at least 28.dp from screen edge", leftGap >= 28.dp)
        assertTrue("Right slider edge gap ($rightGap) must be at least 28.dp from screen edge", rightGap >= 28.dp)
    }
}



