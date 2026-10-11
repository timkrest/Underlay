// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.snapshot

import androidx.compose.ui.graphics.ImageBitmap
import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.blur.SnapshotBlur
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

internal class HostSnapshot(
    private val host: HostCapture,
    private val blur: SnapshotBlur,
    private val scope: CoroutineScope,
    private val onChange: () -> Unit,
) {

    var image: ImageBitmap? = null
        private set

    private var hasFailed = false
    private var pristine: ImageBitmap? = null
    private var sigma = 0f
    private var work: Job? = null
    private var isRecaptureWanted = false

    val source: UnderlaySource
        get() = when {
            image != null -> UnderlaySource.Snapshot
            hasFailed -> UnderlaySource.Fallback
            else -> UnderlaySource.Pending
        }

    private val isBusy: Boolean get() = work?.isActive == true

    fun capture(sigma: Float) {
        this.sigma = sigma
        work?.cancel()
        work = scope.launch { captureUntilCaughtUp() }
    }

    fun captureWhenIdle(sigma: Float) {
        if (isBusy) {
            this.sigma = sigma
            isRecaptureWanted = true
        } else {
            capture(sigma)
        }
    }

    fun reblur(sigma: Float) {
        if (sigma == this.sigma) return

        this.sigma = sigma
        if (isBusy) return

        val captured = pristine ?: return
        work = scope.launch {
            blurUntilCaughtUp(captured)
            if (isRecaptureWanted) captureUntilCaughtUp()
        }
    }

    fun discard() {
        work?.cancel()
        blur.forgetCaptures()
        image = null
        pristine = null
        hasFailed = false
        isRecaptureWanted = false
    }

    private suspend fun captureUntilCaughtUp() {
        do {
            isRecaptureWanted = false
            val captured = captureOrNull()
            if (captured == null) {
                hasFailed = true
                onChange()
            } else {
                pristine = captured
                blurUntilCaughtUp(captured)
            }
        } while (isRecaptureWanted)
    }

    private suspend fun blurUntilCaughtUp(captured: ImageBitmap) {
        hasFailed = false
        onChange()
        do {
            val sigma = sigma
            val blurred = blurOrNull(captured, sigma)
            if (blurred == null) {
                hasFailed = true
                onChange()
                return
            }
            image = blurred
            onChange()
        } while (sigma != this.sigma)
    }

    private suspend fun captureOrNull(): ImageBitmap? {
        val captured = try {
            host.capture()
        } catch (noMemoryForCapture: OutOfMemoryError) {
            null
        }
        currentCoroutineContext().ensureActive()

        return captured
    }

    private suspend fun blurOrNull(captured: ImageBitmap, sigma: Float): ImageBitmap? {
        val blurred = try {
            if (sigma > 0f) blur.blur(captured, sigma) else captured
        } catch (noMemoryForBlur: OutOfMemoryError) {
            null
        }
        currentCoroutineContext().ensureActive()

        return blurred
    }
}
