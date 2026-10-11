// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.snapshot

internal object CaptureScale {

    const val DOWNSCALE = 4

    fun snapshotFloor(hostPixels: Int): Int = hostPixels / DOWNSCALE

    fun snapshotCeil(hostPixels: Int): Int = (hostPixels + DOWNSCALE - 1) / DOWNSCALE

    fun hostPixels(snapshotPixels: Int): Int = snapshotPixels * DOWNSCALE

    fun snapshotSigma(hostSigma: Float): Float = hostSigma / DOWNSCALE
}
