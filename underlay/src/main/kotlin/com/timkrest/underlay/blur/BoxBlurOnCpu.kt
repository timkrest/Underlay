// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.blur

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class BoxBlurOnCpu : SnapshotBlur {

    private val buffersInUse = Mutex()
    private var buffers = Buffers(size = 0)
    private var lastResult: Blurred? = null

    override suspend fun blur(capture: ImageBitmap, sigma: Float): ImageBitmap {
        val radius = boxBlurRadiusForSigma(sigma)
        lastResult?.takeIf { it.capture === capture && it.radius == radius }?.let { return it.image }
        if (radius == 0) return capture

        val image = buffersInUse.withLock {
            withContext(Dispatchers.Default) { blurred(capture.asAndroidBitmap(), radius) }
        }
        lastResult = Blurred(capture, radius, image)

        return image
    }

    override fun clearCache() {
        lastResult = null
    }

    override fun release() = clearCache()

    private fun blurred(capture: Bitmap, radius: Int): ImageBitmap {
        val width = capture.width
        val height = capture.height
        val buffers = buffersFor(width * height)

        capture.getPixels(buffers.pixels, 0, width, 0, 0, width, height)
        blurPixels(buffers.pixels, width, height, radius, buffers.scratch)

        return createBitmap(width, height)
            .apply { setPixels(buffers.pixels, 0, width, 0, 0, width, height) }
            .asImageBitmap()
    }

    private fun buffersFor(size: Int): Buffers {
        if (buffers.pixels.size != size) buffers = Buffers(size)
        return buffers
    }

    private class Buffers(size: Int) {
        val pixels = IntArray(size)
        val scratch = IntArray(size)
    }

    private class Blurred(val capture: ImageBitmap, val radius: Int, val image: ImageBitmap)
}
