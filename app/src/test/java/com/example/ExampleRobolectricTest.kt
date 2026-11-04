package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.DarktableEngine
import com.example.model.ColorFilterMode
import com.example.model.ColorLabel
import com.example.model.EditParams
import com.example.model.PhotoItem
import com.example.model.RatingComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

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
}
