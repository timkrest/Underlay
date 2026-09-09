// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.graphics.Bitmap
import android.view.Window
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

internal class HostSnapshot(
    private val host: Window,
    private val blur: SnapshotBlur,
    nodeScope: CoroutineScope,
    private val onChange: () -> Unit,
) {

    private val onMain = nodeScope + Dispatchers.Main.immediate

    var image: ImageBitmap? = null
        private set

    var hasFailed: Boolean = false
        private set

    private var pristine: Bitmap? = null
    private var sigma: Float = 0f
    private var captureJob: Job? = null
    private var blurJob: Job? = null

    fun capture(sigma: Float) {
        this.sigma = sigma
        hasFailed = false
        captureJob?.cancel()
        blurJob?.cancel()
        onChange()
        captureJob = onMain.launch {
            val captured = capturePristine()
            if (captured == null) {
                hasFailed = true
                onChange()
            } else {
                pristine = captured
                blurPristine()
            }
        }
    }

    fun reblur(sigma: Float) {
        if (sigma == this.sigma) return

        this.sigma = sigma
        blurPristine()
    }

    fun discard() {
        captureJob?.cancel()
        blurJob?.cancel()
        pristine = null
        image = null
        hasFailed = false
    }

    private suspend fun capturePristine(): Bitmap? = try {
        host.captureOnFrameBoundary()
    } catch (noMemoryForCapture: OutOfMemoryError) {
        null
    }

    private fun blurPristine() {
        val captured = pristine ?: return
        val sigma = sigma

        hasFailed = false
        blurJob?.cancel()
        onChange()
        blurJob = onMain.launch {
            try {
                image = if (sigma > 0f) blur.blur(captured, sigma) else captured.asImageBitmap()
            } catch (noMemoryForBlur: OutOfMemoryError) {
                hasFailed = true
            }
            onChange()
        }
    }
}
