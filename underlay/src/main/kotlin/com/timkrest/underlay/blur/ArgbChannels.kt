// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.blur

import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL_MASK = 0xFF
private const val OPAQUE = 0xFF

internal fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
    (alpha shl ALPHA_SHIFT) or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue

internal fun premultiply(pixels: IntArray) = scaleColorChannels(pixels) { channel, alpha ->
    (channel * alpha + CHANNEL_MASK / 2) / CHANNEL_MASK
}

internal fun unpremultiply(pixels: IntArray) = scaleColorChannels(pixels) { channel, alpha ->
    ((channel * CHANNEL_MASK + alpha / 2) / alpha).coerceAtMost(CHANNEL_MASK)
}

private inline fun scaleColorChannels(pixels: IntArray, scale: (channel: Int, alpha: Int) -> Int) {
    for (index in pixels.indices) {
        val pixel = pixels[index]
        val alpha = pixel.alpha
        pixels[index] = when (alpha) {
            OPAQUE -> pixel
            0 -> 0
            else -> argb(alpha, scale(pixel.red, alpha), scale(pixel.green, alpha), scale(pixel.blue, alpha))
        }
    }
}
