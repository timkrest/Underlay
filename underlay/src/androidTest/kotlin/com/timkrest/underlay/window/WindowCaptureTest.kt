// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.timkrest.underlay.matches
import com.timkrest.underlay.snapshot.CaptureScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class WindowCaptureTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theHostWindowIsCapturedWithTheColorOnScreen() {
        showFullScreen(Color.Red)

        val center = Color(captureHostWindow().centerPixel())

        assertTrue(center.matches(Color.Red), "captured $center")
        assertEquals(1f, center.alpha, "an opaque window must capture opaque")
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
        val covered = snapshotPixels * CaptureScale.DOWNSCALE

        assertTrue(covered <= hostPixels, "$side: the snapshot claims $covered of $hostPixels host pixels")
        assertTrue(
            hostPixels - covered < CaptureScale.DOWNSCALE,
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
}
