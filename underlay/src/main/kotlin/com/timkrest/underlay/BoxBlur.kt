package com.timkrest.underlay

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val BLUR_PASSES = 3
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL_MASK = 0xFF
private const val OPAQUE = 0xFF

private val SIGMA_TO_BOX_SIZE = (3.0 * sqrt(2.0 * PI) / 4.0).toFloat()

/**
 * Box blur radius approximating a Gaussian of [sigma], via the SVG filter specification's three-box
 * approximation: `box size = sigma * 3 * sqrt(2*pi) / 4`. Returns 0 below one box pixel.
 */
internal fun boxBlurRadiusForSigma(sigma: Float): Int {
    val boxSize = (sigma * SIGMA_TO_BOX_SIZE).roundToInt()
    return ((boxSize - 1) / 2).coerceAtLeast(0)
}

/**
 * Blurs [width] x [height] ARGB_8888 [pixels] in place with three box passes. The radius is clamped
 * to half the shorter side; 0 leaves them untouched. Channels are averaged premultiplied, so
 * non-opaque pixels do not bleed color into transparent neighbours.
 */
internal fun blurPixels(pixels: IntArray, width: Int, height: Int, radius: Int) {
    require(width >= 0 && height >= 0) { "negative size ${width}x$height" }
    require(pixels.size == width * height) { "pixels.size=${pixels.size} does not match ${width}x$height" }

    val maxRadius = (minOf(width, height) / 2).coerceAtLeast(0)
    val blurRadius = radius.coerceIn(0, maxRadius)
    if (blurRadius == 0) return

    premultiply(pixels)

    val buffer = IntArray(pixels.size)
    val channels = ChannelSums()
    repeat(BLUR_PASSES) {
        blurRows(pixels, buffer, width, height, blurRadius, channels)
        blurColumns(buffer, pixels, width, height, blurRadius, channels)
    }

    unpremultiply(pixels)
}

private fun blurRows(source: IntArray, target: IntArray, width: Int, height: Int, radius: Int, channels: ChannelSums) {
    val window = radius * 2 + 1
    val lastColumn = width - 1

    for (y in 0 until height) {
        val rowOffset = y * width
        channels.reset()
        for (offset in -radius..radius) {
            channels.add(source[rowOffset + offset.coerceIn(0, lastColumn)])
        }

        for (x in 0 until width) {
            target[rowOffset + x] = channels.average(window)
            channels.add(source[rowOffset + (x + radius + 1).coerceIn(0, lastColumn)])
            channels.subtract(source[rowOffset + (x - radius).coerceIn(0, lastColumn)])
        }
    }
}

private fun blurColumns(source: IntArray, target: IntArray, width: Int, height: Int, radius: Int, channels: ChannelSums) {
    val window = radius * 2 + 1
    val lastRow = height - 1

    for (x in 0 until width) {
        channels.reset()
        for (offset in -radius..radius) {
            channels.add(source[offset.coerceIn(0, lastRow) * width + x])
        }

        for (y in 0 until height) {
            target[y * width + x] = channels.average(window)
            channels.add(source[(y + radius + 1).coerceIn(0, lastRow) * width + x])
            channels.subtract(source[(y - radius).coerceIn(0, lastRow) * width + x])
        }
    }
}

private inline fun scaleChannels(pixels: IntArray, scale: (channel: Int, alpha: Int) -> Int) {
    for (index in pixels.indices) {
        val pixel = pixels[index]
        val alpha = pixel ushr ALPHA_SHIFT and CHANNEL_MASK
        if (alpha == OPAQUE) continue
        if (alpha == 0) {
            pixels[index] = 0
            continue
        }
        pixels[index] = (alpha shl ALPHA_SHIFT) or
            (scale(pixel ushr RED_SHIFT and CHANNEL_MASK, alpha) shl RED_SHIFT) or
            (scale(pixel ushr GREEN_SHIFT and CHANNEL_MASK, alpha) shl GREEN_SHIFT) or
            scale(pixel and CHANNEL_MASK, alpha)
    }
}

private fun premultiply(pixels: IntArray) = scaleChannels(pixels) { channel, alpha ->
    channel * alpha / CHANNEL_MASK
}

private fun unpremultiply(pixels: IntArray) = scaleChannels(pixels) { channel, alpha ->
    (channel * CHANNEL_MASK / alpha).coerceAtMost(CHANNEL_MASK)
}

private class ChannelSums {

    private var alpha = 0
    private var red = 0
    private var green = 0
    private var blue = 0

    fun reset() {
        alpha = 0
        red = 0
        green = 0
        blue = 0
    }

    fun add(pixel: Int) {
        alpha += pixel ushr ALPHA_SHIFT and CHANNEL_MASK
        red += pixel ushr RED_SHIFT and CHANNEL_MASK
        green += pixel ushr GREEN_SHIFT and CHANNEL_MASK
        blue += pixel and CHANNEL_MASK
    }

    fun subtract(pixel: Int) {
        alpha -= pixel ushr ALPHA_SHIFT and CHANNEL_MASK
        red -= pixel ushr RED_SHIFT and CHANNEL_MASK
        green -= pixel ushr GREEN_SHIFT and CHANNEL_MASK
        blue -= pixel and CHANNEL_MASK
    }

    fun average(window: Int): Int =
        (alpha / window shl ALPHA_SHIFT) or
            (red / window shl RED_SHIFT) or
            (green / window shl GREEN_SHIFT) or
            (blue / window)
}
