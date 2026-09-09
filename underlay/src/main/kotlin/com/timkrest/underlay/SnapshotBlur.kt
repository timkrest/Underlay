// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/** Blurs a window capture. [sigma] is a Gaussian standard deviation in snapshot pixels. */
internal interface SnapshotBlur {

    suspend fun blur(capture: Bitmap, sigma: Float): ImageBitmap
}

internal fun snapshotBlur(layer: GraphicsLayer): SnapshotBlur =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) BlurEffectOnGpu(layer) else BoxBlurOnCpu()

/**
 * Keeps the last result: a slider drag walks through sigmas that share one box radius, and each
 * blur is six passes over every pixel.
 */
internal class BoxBlurOnCpu : SnapshotBlur {

    private var blurred: ImageBitmap? = null
    private var blurredFrom: Bitmap? = null
    private var blurredRadius = NO_RADIUS

    override suspend fun blur(capture: Bitmap, sigma: Float): ImageBitmap {
        val radius = boxBlurRadiusForSigma(sigma)
        blurred?.let { if (radius == blurredRadius && capture === blurredFrom) return it }

        val result = withContext(Dispatchers.Default) { capture.boxBlurred(radius).asImageBitmap() }
        blurred = result
        blurredFrom = capture
        blurredRadius = radius

        return result
    }

    private companion object {
        const val NO_RADIUS = -1
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private class BlurEffectOnGpu(private val layer: GraphicsLayer) : SnapshotBlur {

    private val onCpu = BoxBlurOnCpu()

    override suspend fun blur(capture: Bitmap, sigma: Float): ImageBitmap {
        val image = capture.asImageBitmap()

        return try {
            // drawImage works in pixels, so the density and direction record asks for do not matter.
            layer.record(Density(1f), LayoutDirection.Ltr, IntSize(capture.width, capture.height)) { drawImage(image) }
            layer.renderEffect = BlurEffect(sigma, sigma)
            layer.toImageBitmap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (noRenderingContext: RuntimeException) {
            onCpu.blur(capture, sigma)
        }
    }
}

private fun Bitmap.boxBlurred(radius: Int): Bitmap {
    if (radius <= 0) return this

    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    blurPixels(pixels, width, height, radius)

    return createBitmap(width, height).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
