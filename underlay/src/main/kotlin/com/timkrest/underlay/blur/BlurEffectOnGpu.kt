// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.blur

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.cancellation.CancellationException

private val RAW_PIXELS = Density(1f)

@RequiresApi(Build.VERSION_CODES.S)
internal class BlurEffectOnGpu(private val graphicsContext: GraphicsContext) : SnapshotBlur {

    private val layer = graphicsContext.createGraphicsLayer()
    private val onCpu = BoxBlurOnCpu()

    override suspend fun blur(capture: ImageBitmap, sigma: Float): ImageBitmap = try {
        val radius = platformRadiusForSigma(sigma)
        if (radius > 0f) blurOnLayer(capture, radius) else capture
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (noRenderingContext: RuntimeException) {
        onCpu.blur(capture, sigma)
    }

    override fun clearCache() = onCpu.clearCache()

    override fun release() {
        graphicsContext.releaseGraphicsLayer(layer)
        onCpu.release()
    }

    private suspend fun blurOnLayer(capture: ImageBitmap, radius: Float): ImageBitmap {
        layer.record(RAW_PIXELS, LayoutDirection.Ltr, IntSize(capture.width, capture.height)) { drawImage(capture) }
        layer.renderEffect = BlurEffect(radius, radius)

        return layer.toImageBitmap()
    }
}
