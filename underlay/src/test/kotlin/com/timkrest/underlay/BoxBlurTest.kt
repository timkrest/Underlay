// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BoxBlurTest {

    @Test
    fun `a sigma no box can approximate rounds to no blur`() {
        assertEquals(0, boxBlurRadiusForSigma(0f))
        assertEquals(0, boxBlurRadiusForSigma(0.5f))
    }

    @Test
    fun `the radius lands on the nearest sigma three box passes can produce`() {
        assertEquals(1, boxBlurRadiusForSigma(1f))
        assertEquals(2, boxBlurRadiusForSigma(2f))
        assertEquals(12, boxBlurRadiusForSigma(12f))
    }

    @Test
    fun `radius zero leaves the pixels alone`() {
        val pixels = intArrayOf(RED, GREEN, BLUE, WHITE)
        val original = pixels.copyOf()

        blurPixels(pixels, width = 2, height = 2, radius = 0)

        assertContentEquals(original, pixels)
    }

    @Test
    fun `a uniform image survives the blur unchanged`() {
        val pixels = IntArray(16 * 16) { opaque(red = 30, green = 60, blue = 90) }
        val original = pixels.copyOf()

        blurPixels(pixels, width = 16, height = 16, radius = 3)

        assertContentEquals(original, pixels)
    }

    @Test
    fun `a single bright pixel spreads into its neighbours`() {
        val size = 9
        val center = size / 2 * size + size / 2
        val pixels = IntArray(size * size) { if (it == center) WHITE else opaque(0, 0, 0) }

        blurPixels(pixels, width = size, height = size, radius = 1)

        assertTrue(red(pixels[center]) < 255, "the peak must be dimmed, was ${red(pixels[center])}")
        assertTrue(red(pixels[center - 1]) > 0, "the left neighbour must pick up light")
        assertTrue(red(pixels[center + 1]) > 0, "the right neighbour must pick up light")
        assertTrue(red(pixels[center - size]) > 0, "the neighbour above must pick up light")
        assertTrue(red(pixels[center + size]) > 0, "the neighbour below must pick up light")
    }

    @Test
    fun `a symmetric image blurs symmetrically`() {
        val size = 8
        val pixels = IntArray(size * size) { index ->
            val x = index % size
            if (x == 0 || x == size - 1) WHITE else opaque(0, 0, 0)
        }

        blurPixels(pixels, width = size, height = size, radius = 2)

        for (y in 0 until size) {
            for (x in 0 until size / 2) {
                assertEquals(
                    pixels[y * size + x],
                    pixels[y * size + (size - 1 - x)],
                    "row $y is not symmetric at column $x",
                )
            }
        }
    }

    @Test
    fun `fully transparent pixels do not bleed their color`() {
        val pixels = IntArray(8 * 8) { 0x00FF0000 }

        blurPixels(pixels, width = 8, height = 8, radius = 2)

        assertTrue(pixels.all { it == 0 }, "transparent pixels kept color: ${pixels.first().toUInt().toString(16)}")
    }

    @Test
    fun `a translucent edge does not darken the opaque side`() {
        val size = 8
        val pixels = IntArray(size * size) { index ->
            if (index % size < size / 2) opaque(255, 0, 0) else 0
        }

        blurPixels(pixels, width = size, height = size, radius = 1)

        val leftmost = pixels[size * (size / 2)]
        assertEquals(255, alpha(leftmost), "the far side of the opaque half must stay opaque")
        assertEquals(255, red(leftmost), "the far side of the opaque half must keep its color")
    }

    @Test
    fun `the blur keeps the brightness it was given`() {
        val size = 32
        val pixels = IntArray(size * size) { index -> opaque(index % 256, index * 7 % 256, index * 13 % 256) }
        val before = pixels.sumOf { red(it) }

        blurPixels(pixels, width = size, height = size, radius = 7)

        val drift = abs(pixels.sumOf { red(it) } - before).toDouble() / pixels.size
        assertTrue(drift <= 1.0, "the blur shifted brightness by $drift levels per pixel")
    }

    @Test
    fun `a uniform translucent image keeps its color`() {
        val alpha = 10
        val pixels = IntArray(16 * 16) { (alpha shl 24) or (100 shl 16) }

        blurPixels(pixels, width = 16, height = 16, radius = 3)

        val drift = abs(red(pixels.first()) - 100)
        assertTrue(drift <= 3, "premultiplying moved the color by $drift levels")
    }

    @Test
    fun `a radius wider than the image is clamped instead of overrunning`() {
        val pixels = IntArray(4 * 3) { WHITE }

        blurPixels(pixels, width = 4, height = 3, radius = 999)

        assertTrue(pixels.all { it == WHITE })
    }

    @Test
    fun `a size that does not match the array is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            blurPixels(IntArray(10), width = 4, height = 3, radius = 1)
        }
    }

    private companion object {
        const val WHITE = 0xFFFFFFFF.toInt()
        const val RED = 0xFFFF0000.toInt()
        const val GREEN = 0xFF00FF00.toInt()
        const val BLUE = 0xFF0000FF.toInt()

        fun opaque(red: Int, green: Int, blue: Int): Int =
            (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

        fun alpha(pixel: Int): Int = pixel ushr 24 and 0xFF

        fun red(pixel: Int): Int = pixel ushr 16 and 0xFF
    }
}
