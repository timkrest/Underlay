// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.blur

import androidx.compose.ui.graphics.ImageBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.timkrest.underlay.halves
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class BoxBlurOnCpuTest {

    private val blur = BoxBlurOnCpu()
    private val capture = halves()

    @Test
    fun aSigmaOfTheSameBoxRadiusComesFromTheCache() {
        val blurred = blurOf(capture, SIGMA)

        assertSame(blurred, blurOf(capture, SIGMA + SAME_RADIUS_NUDGE))
    }

    @Test
    fun anotherRadiusOrAnotherCaptureIsBlurredAgain() {
        val blurred = blurOf(capture, SIGMA)

        assertNotSame(blurred, blurOf(capture, WEAKER_SIGMA))
        assertNotSame(blurred, blurOf(halves(), SIGMA))
    }

    @Test
    fun aClearedCacheBlursTheSameCaptureAgain() {
        val blurred = blurOf(capture, SIGMA)

        blur.clearCache()

        assertNotSame(blurred, blurOf(capture, SIGMA))
    }

    private fun blurOf(capture: ImageBitmap, sigma: Float) = runBlocking { blur.blur(capture, sigma) }

    private companion object {
        const val SIGMA = 8f
        const val SAME_RADIUS_NUDGE = 0.05f
        const val WEAKER_SIGMA = 2f
    }
}
