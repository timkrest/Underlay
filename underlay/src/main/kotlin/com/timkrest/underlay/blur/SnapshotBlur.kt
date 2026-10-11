// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.blur

import android.os.Build
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap

internal interface SnapshotBlur {

    suspend fun blur(capture: ImageBitmap, sigma: Float): ImageBitmap

    fun clearCache()

    fun release()

    companion object {

        fun bestAvailable(graphicsContext: () -> GraphicsContext?): SnapshotBlur {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return BoxBlurOnCpu()

            return graphicsContext()?.let(::BlurEffectOnGpu) ?: BoxBlurOnCpu()
        }
    }
}
