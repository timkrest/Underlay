package com.timkrest.underlay

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

private const val BLUR_PASSES = 3
private const val BOX_VARIANCE_DIVISOR = 12f
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL_MASK = 0xFF
private const val OPAQUE = 0xFF

/** The box radius whose [BLUR_PASSES] passes land closest to a Gaussian of [sigma]. */
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

/**
 * [BLUR_PASSES] box passes over ARGB_8888 [pixels], in place. Channels are averaged premultiplied,
 * so a transparent neighbour does not bleed its color into the result.
 */
internal fun blurPixels(pixels: IntArray, width: Int, height: Int, radius: Int) {
    require(width >= 0 && height >= 0) { "negative size ${width}x$height" }
    require(pixels.size == width * height) { "pixels.size=${pixels.size} does not match ${width}x$height" }

    val maxRadius = (minOf(width, height) / 2).coerceAtLeast(0)
    val blurRadius = radius.coerceIn(0, maxRadius)
    if (blurRadius == 0) return

    premultiply(pixels)

    val buffer = IntArray(pixels.size)
    repeat(BLUR_PASSES) {
        blurRows(pixels, buffer, width, height, blurRadius)
        blurColumns(buffer, pixels, width, height, blurRadius)
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
    val window = radius * 2 + 1
    val last = length - 1
    val channels = ChannelSums()

    for (line in 0 until lines) {
        val start = line * lineStride
        channels.reset()
        for (offset in -radius..radius) {
            channels.add(source[start + offset.coerceIn(0, last) * pixelStride])
        }

        for (index in 0 until length) {
            target[start + index * pixelStride] = channels.average(window)
            channels.add(source[start + (index + radius + 1).coerceIn(0, last) * pixelStride])
            channels.subtract(source[start + (index - radius).coerceIn(0, last) * pixelStride])
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
    (channel * alpha + CHANNEL_MASK / 2) / CHANNEL_MASK
}

private fun unpremultiply(pixels: IntArray) = scaleChannels(pixels) { channel, alpha ->
    ((channel * CHANNEL_MASK + alpha / 2) / alpha).coerceAtMost(CHANNEL_MASK)
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

    /** Truncating instead would lose half a level on every pass and visibly darken the result. */
    fun average(window: Int): Int {
        val rounding = window / 2
        return ((alpha + rounding) / window shl ALPHA_SHIFT) or
            ((red + rounding) / window shl RED_SHIFT) or
            ((green + rounding) / window shl GREEN_SHIFT) or
            ((blue + rounding) / window)
    }
}
