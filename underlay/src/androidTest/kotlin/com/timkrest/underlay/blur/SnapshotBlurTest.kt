// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.blur

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.timkrest.underlay.pixelAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SnapshotBlurTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theSeamBetweenTwoHalvesComesOutMixed() {
        lateinit var graphicsContext: GraphicsContext
        compose.setContent { graphicsContext = LocalGraphicsContext.current }
        compose.waitForIdle()

        val blurred = runBlocking(Dispatchers.Main) {
            val blur = SnapshotBlur.bestAvailable { graphicsContext }
            try {
                blur.blur(halves(), SIGMA)
            } finally {
                blur.release()
            }
        }

        val seam = blurred.pixelAt(blurred.width / 2, blurred.height / 2)
        assertTrue(seam.red in MIXED && seam.blue in MIXED, "the seam was not mixed: $seam")
    }

    private companion object {
        const val SIGMA = 8f
        val MIXED = 0.15f..0.85f
    }
}

internal fun halves(size: Int = 64): ImageBitmap {
    val pixels = IntArray(size * size) { if (it % size < size / 2) OPAQUE_RED else OPAQUE_BLUE }

    return createBitmap(size, size).apply { setPixels(pixels, 0, size, 0, 0, size, size) }.asImageBitmap()
}

private const val OPAQUE_RED = 0xFFFF0000.toInt()
private const val OPAQUE_BLUE = 0xFF0000FF.toInt()
