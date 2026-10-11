// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.blur

import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoxBlurTest {

    @Test
    fun `a sigma no box can approximate rounds to no blur`() {
        assertEquals(0, boxBlurRadiusForSigma(0f))
        assertEquals(0, boxBlurRadiusForSigma(0.5f))
    }

    @Test
    fun `the radius lands on the nearest sigma three box passes can produce`() {
        assertEquals(1, boxBlurRadiusForSigma(0.8f), "flooring the sigma would give no blur")
        assertEquals(3, boxBlurRadiusForSigma(3.7f), "rounding the sigma would overshoot")
        assertEquals(12, boxBlurRadiusForSigma(12f))
    }

    @Test
    fun `a uniform image survives the blur unchanged`() {
        val pixels = IntArray(16 * 16) { argb(255, 30, 60, 90) }
        val original = pixels.copyOf()

        blurPixels(pixels, width = 16, height = 16, radius = 3)

        assertContentEquals(original, pixels)
    }

    @Test
    fun `a single bright pixel spreads into its neighbours`() {
        val size = 9
        val center = size / 2 * size + size / 2
        val pixels = IntArray(size * size) { if (it == center) WHITE else BLACK }

        blurPixels(pixels, width = size, height = size, radius = 1)

        assertTrue(red(pixels[center]) < 255, "the peak was not dimmed")
        for (neighbour in listOf(center - 1, center + 1, center - size, center + size)) {
            assertTrue(red(pixels[neighbour]) > 0, "pixel $neighbour picked up no light")
        }
    }

    @Test
    fun `a symmetric image blurs symmetrically`() {
        val size = 8
        val pixels = IntArray(size * size) { index ->
            val x = index % size
            if (x == 0 || x == size - 1) WHITE else BLACK
        }

        blurPixels(pixels, width = size, height = size, radius = 2)

        for (y in 0 until size) {
            for (x in 0 until size / 2) {
                assertEquals(pixels[y * size + x], pixels[y * size + (size - 1 - x)], "row $y, column $x")
            }
        }
    }

    @Test
    fun `a transparent neighbour does not bleed its color`() {
        val size = 8
        val pixels = IntArray(size * size) { index ->
            if (index % size < size / 2) argb(0, 255, 0, 0) else argb(255, 0, 0, 255)
        }

        blurPixels(pixels, width = size, height = size, radius = 1)

        val edge = pixels[size / 2]
        assertTrue(alpha(edge) in 1..254, "the edge did not turn translucent")
        assertEquals(0, red(edge), "the transparent half bled its red")
        assertEquals(255, blue(edge), "the transparent half darkened the blue")
    }

    @Test
    fun `the blur keeps the brightness it was given`() {
        val size = 32
        val pixels = IntArray(size * size) { index -> argb(255, index % 256, index * 7 % 256, index * 13 % 256) }
        val before = pixels.sumOf { red(it) }

        blurPixels(pixels, width = size, height = size, radius = 7)

        val drift = abs(pixels.sumOf { red(it) } - before).toDouble() / pixels.size
        assertTrue(drift <= 1.0, "the blur shifted brightness by $drift levels per pixel")
    }

    @Test
    fun `a uniform translucent image keeps its color`() {
        val pixels = IntArray(16 * 16) { argb(10, 100, 0, 0) }

        blurPixels(pixels, width = 16, height = 16, radius = 3)

        val drift = abs(red(pixels.first()) - 100)
        assertTrue(drift <= 3, "premultiplying moved the color by $drift levels")
    }

    @Test(timeout = 5_000)
    fun `a radius wider than the image is clamped to it`() {
        val pixels = IntArray(4 * 3) { WHITE }

        blurPixels(pixels, width = 4, height = 3, radius = Int.MAX_VALUE)

        assertTrue(pixels.all { it == WHITE })
    }

    private companion object {
        val WHITE = argb(255, 255, 255, 255)
        val BLACK = argb(255, 0, 0, 0)

        fun alpha(pixel: Int): Int = pixel ushr 24 and 0xFF

        fun red(pixel: Int): Int = pixel ushr 16 and 0xFF

        fun blue(pixel: Int): Int = pixel and 0xFF
    }
}
