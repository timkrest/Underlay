// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Runs on a device: the blur goes through `RenderEffect` from API 31 and box passes below it. */
@RunWith(AndroidJUnit4::class)
class SnapshotBlurTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theSeamBetweenTwoHalvesComesOutMixed() {
        val blurred = blurredHalves()

        val seam = blurred.pixelAt(blurred.width / 2, blurred.height / 2)
        assertTrue(seam.red in MIXED, "expected red mixed into the seam, got $seam")
        assertTrue(seam.blue in MIXED, "expected blue mixed into the seam, got $seam")
    }

    @Test
    fun theCpuBlurAnswersASigmaOfTheSameRadiusFromWhatItKeeps() {
        val engine = BoxBlurOnCpu()
        val capture = halves()

        val blurred = runBlocking { engine.blur(capture, SIGMA) }
        val nudged = runBlocking { engine.blur(capture, SIGMA + SAME_RADIUS_NUDGE) }

        assertSame(blurred, nudged, "the same radius over the same capture was blurred twice")
    }

    @Test
    fun theCpuBlurBlursAgainForAnotherRadius() {
        val engine = BoxBlurOnCpu()
        val capture = halves()

        val blurred = runBlocking { engine.blur(capture, SIGMA) }
        val weaker = runBlocking { engine.blur(capture, WEAKER_SIGMA) }

        assertNotSame(blurred, weaker, "a new radius came back from the cache")
    }

    @Test
    fun theCpuBlurBlursAgainForAnotherCapture() {
        val engine = BoxBlurOnCpu()

        val blurred = runBlocking { engine.blur(halves(), SIGMA) }
        val recaptured = runBlocking { engine.blur(halves(), SIGMA) }

        assertNotSame(blurred, recaptured, "a fresh capture came back from the cache")
    }

    private fun blurredHalves(): ImageBitmap {
        lateinit var layer: GraphicsLayer
        compose.setContent {
            layer = rememberGraphicsLayer()
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxHeight().weight(1f).background(Color.Red))
                Box(Modifier.fillMaxHeight().weight(1f).background(Color.Blue))
            }
        }
        compose.waitForIdle()

        return runBlocking(Dispatchers.Main) {
            val window = compose.activity.window
            val capture = assertNotNull(window.createCaptureBitmap(), "the host window is not drawable")
            assertTrue(window.captureInto(capture), "the host window could not be captured")
            snapshotBlur(layer).blur(capture, SIGMA)
        }
    }

    private fun halves(): Bitmap {
        val pixels = IntArray(SIZE * SIZE) { if (it % SIZE < SIZE / 2) OPAQUE_RED else OPAQUE_BLUE }

        return createBitmap(SIZE, SIZE).apply { setPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE) }
    }

    private companion object {
        const val SIGMA = 8f
        const val SAME_RADIUS_NUDGE = 0.05f
        const val WEAKER_SIGMA = 2f
        const val SIZE = 64
        const val OPAQUE_RED = 0xFFFF0000.toInt()
        const val OPAQUE_BLUE = 0xFF0000FF.toInt()
        val MIXED = 0.15f..0.85f
    }
}
