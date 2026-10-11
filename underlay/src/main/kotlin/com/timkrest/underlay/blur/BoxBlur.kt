// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.blur

import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

private const val BLUR_PASSES = 3
private const val BOX_VARIANCE_DIVISOR = 12f

internal fun boxBlurRadiusForSigma(sigma: Float): Int {
    if (sigma <= 0f) return 0

    val window = sqrt(BOX_VARIANCE_DIVISOR * sigma * sigma / BLUR_PASSES + 1f)
    val narrower = floor((window - 1f) / 2f).toInt().coerceAtLeast(0)
    val wider = narrower + 1

    return if (abs(sigmaOfRadius(narrower) - sigma) <= abs(sigmaOfRadius(wider) - sigma)) narrower else wider
}

private fun sigmaOfRadius(radius: Int): Float {
    val window = 2f * radius + 1f
    return sqrt((window * window - 1f) * BLUR_PASSES / BOX_VARIANCE_DIVISOR)
}

internal fun blurPixels(
    pixels: IntArray,
    width: Int,
    height: Int,
    radius: Int,
    scratch: IntArray = IntArray(pixels.size),
    beforeEachPass: () -> Unit = {},
) {
    require(pixels.size == width * height) { "pixels.size=${pixels.size} does not match ${width}x$height" }
    require(scratch.size >= pixels.size) { "scratch.size=${scratch.size} cannot hold ${pixels.size} pixels" }

    val blurRadius = radius.coerceIn(0, maxOf(minOf(width, height) / 2, 0))
    if (blurRadius == 0) return

    premultiply(pixels)
    repeat(BLUR_PASSES) {
        beforeEachPass()
        blurRows(pixels, scratch, width, height, blurRadius)
        blurColumns(scratch, pixels, width, height, blurRadius)
    }
    unpremultiply(pixels)
}

private fun blurRows(source: IntArray, target: IntArray, width: Int, height: Int, radius: Int) =
    blurLines(source, target, lines = height, lineStride = width, length = width, pixelStride = 1, radius = radius)

private fun blurColumns(source: IntArray, target: IntArray, width: Int, height: Int, radius: Int) =
    blurLines(source, target, lines = width, lineStride = 1, length = height, pixelStride = width, radius = radius)

private fun blurLines(
    source: IntArray,
    target: IntArray,
    lines: Int,
    lineStride: Int,
    length: Int,
    pixelStride: Int,
    radius: Int,
) {
    val boxSize = radius * 2 + 1
    val rounding = boxSize / 2
    val last = length - 1

    for (line in 0 until lines) {
        val start = line * lineStride
        fun pixelAt(index: Int) = source[start + index.coerceIn(0, last) * pixelStride]

        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0
        for (offset in -radius..radius) {
            val pixel = pixelAt(offset)
            alpha += pixel.alpha
            red += pixel.red
            green += pixel.green
            blue += pixel.blue
        }

        for (index in 0 until length) {
            target[start + index * pixelStride] = argb(
                alpha = (alpha + rounding) / boxSize,
                red = (red + rounding) / boxSize,
                green = (green + rounding) / boxSize,
                blue = (blue + rounding) / boxSize,
            )
            val entering = pixelAt(index + radius + 1)
            val leaving = pixelAt(index - radius)
            alpha += entering.alpha - leaving.alpha
            red += entering.red - leaving.red
            green += entering.green - leaving.green
            blue += entering.blue - leaving.blue
        }
    }
}
