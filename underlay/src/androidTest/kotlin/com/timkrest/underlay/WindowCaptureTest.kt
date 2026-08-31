package com.timkrest.underlay

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import android.graphics.Color as PixelColor

/** Runs on a device: neither `PixelCopy` nor `decorView.draw()` is reachable from a JVM test. */
@RunWith(AndroidJUnit4::class)
class WindowCaptureTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theHostWindowIsCapturedWithTheColorOnScreen() {
        showFullScreen(Color.Red)

        val center = captureHostWindow().centerPixel()

        assertChannel(expected = 255, actual = PixelColor.red(center), name = "red")
        assertChannel(expected = 0, actual = PixelColor.green(center), name = "green")
        assertChannel(expected = 0, actual = PixelColor.blue(center), name = "blue")
        assertEquals(255, PixelColor.alpha(center), "an opaque window must capture opaque")
    }

    @Test
    fun aCaptureIsDownscaledByTheDeclaredFactor() {
        showFullScreen(Color.Blue)
        val decorView = compose.activity.window.decorView

        val captured = captureHostWindow()

        assertEquals(decorView.width / WINDOW_CAPTURE_DOWN_SCALE, captured.width)
        assertEquals(decorView.height / WINDOW_CAPTURE_DOWN_SCALE, captured.height)
    }

    private fun showFullScreen(color: Color) {
        compose.setContent {
            Box(Modifier.fillMaxSize().background(color))
        }
        compose.waitForIdle()
    }

    /** Driven from the test thread so the main looper can deliver the `PixelCopy` result. */
    private fun captureHostWindow(): Bitmap = runBlocking(Dispatchers.Main) {
        val window = compose.activity.window
        val destination = assertNotNull(window.createCaptureBitmap(), "the host window is not drawable")
        assertTrue(window.captureInto(destination), "the host window could not be captured")
        destination
    }

    private fun Bitmap.centerPixel(): Int = getPixel(width / 2, height / 2)

    private fun assertChannel(expected: Int, actual: Int, name: String) {
        assertTrue(abs(expected - actual) <= CHANNEL_TOLERANCE, "$name channel: expected ~$expected, was $actual")
    }

    private companion object {
        /** A round trip through the GPU is not always bit exact. */
        const val CHANNEL_TOLERANCE = 8
    }
}
