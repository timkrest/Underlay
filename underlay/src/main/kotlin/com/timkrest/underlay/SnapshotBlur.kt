// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

internal interface SnapshotBlur {

    suspend fun blur(capture: Bitmap, sigma: Float): ImageBitmap

    fun release()
}

private val RAW_PIXELS = Density(1f)

internal fun snapshotBlur(graphicsContext: GraphicsContext): SnapshotBlur =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) BlurEffectOnGpu(graphicsContext) else BoxBlurOnCpu()

internal class BoxBlurOnCpu : SnapshotBlur {

    private var kept: Blurred? = null

    override suspend fun blur(capture: Bitmap, sigma: Float): ImageBitmap {
        val radius = boxBlurRadiusForSigma(sigma)
        kept?.let { if (it.capture === capture && it.radius == radius) return it.image }

        val image = withContext(Dispatchers.Default) { capture.boxBlurred(radius).asImageBitmap() }
        kept = Blurred(capture, radius, image)

        return image
    }

    override fun release() {
        kept = null
    }

    private class Blurred(val capture: Bitmap, val radius: Int, val image: ImageBitmap)
}

@RequiresApi(Build.VERSION_CODES.S)
private class BlurEffectOnGpu(private val graphicsContext: GraphicsContext) : SnapshotBlur {

    private val layer = graphicsContext.createGraphicsLayer()
    private val onCpu = BoxBlurOnCpu()

    override suspend fun blur(capture: Bitmap, sigma: Float): ImageBitmap {
        val image = capture.asImageBitmap()

        return try {
            layer.record(RAW_PIXELS, LayoutDirection.Ltr, IntSize(capture.width, capture.height)) { drawImage(image) }
            layer.renderEffect = BlurEffect(sigma, sigma)
            layer.toImageBitmap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (noRenderingContext: RuntimeException) {
            onCpu.blur(capture, sigma)
        }
    }

    override fun release() {
        graphicsContext.releaseGraphicsLayer(layer)
        onCpu.release()
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
