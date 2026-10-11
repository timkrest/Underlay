// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.backdrop

import com.timkrest.underlay.snapshot.CaptureScale
import kotlin.math.roundToInt

@JvmInline
internal value class BlurRadius private constructor(private val px: Float) {

    val windowPx: Int get() = px.roundToInt()

    val snapshotSigma: Float get() = CaptureScale.snapshotSigma(px)

    companion object {

        val None = BlurRadius(0f)

        fun ofPx(px: Float): BlurRadius = if (px.isFinite() && px > 0f) BlurRadius(px) else None
    }
}
