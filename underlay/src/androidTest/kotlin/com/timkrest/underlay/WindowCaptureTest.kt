// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
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
    fun aCaptureIsTheLargestExactDownscaleThatFitsTheWindow() {
        showFullScreen(Color.Blue)
        val decorView = compose.activity.window.decorView

        val captured = captureHostWindow()

        assertExactDownscale(hostPixels = decorView.width, snapshotPixels = captured.width, side = "width")
        assertExactDownscale(hostPixels = decorView.height, snapshotPixels = captured.height, side = "height")
    }

    private fun assertExactDownscale(hostPixels: Int, snapshotPixels: Int, side: String) {
        val covered = snapshotPixels * WINDOW_CAPTURE_DOWN_SCALE

        assertTrue(covered <= hostPixels, "$side: the snapshot claims $covered of $hostPixels host pixels")
        assertTrue(
            hostPixels - covered < WINDOW_CAPTURE_DOWN_SCALE,
            "$side: another snapshot pixel would have fit, ${hostPixels - covered} host pixels are uncovered",
        )
    }

    private fun showFullScreen(color: Color) {
        compose.setContent {
            Box(Modifier.fillMaxSize().background(color))
        }
        compose.waitForIdle()
    }

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
        const val CHANNEL_TOLERANCE = 8
    }
}
