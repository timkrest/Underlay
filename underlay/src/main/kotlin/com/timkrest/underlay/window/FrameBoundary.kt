// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.view.Choreographer
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun awaitDrawnFrame() {
    withFrameNanos { }
    awaitVsync()
}

internal suspend fun awaitVsync(): Unit = suspendCancellableCoroutine { continuation ->
    val choreographer = Choreographer.getInstance()
    val callback = Choreographer.FrameCallback { continuation.resume(Unit) }

    choreographer.postFrameCallback(callback)
    continuation.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
}
