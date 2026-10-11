// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.animation.ValueAnimator
import android.os.Build
import android.os.SystemClock
import com.timkrest.underlay.backdrop.SnapshotFadeIn
import com.timkrest.underlay.window.awaitVsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val FADE_IN_MILLIS = 200L

internal class SnapshotFader(private val scope: CoroutineScope, private val onFrame: () -> Unit) {

    private var fadeIn = SnapshotFadeIn.Done
    private var frames: Job? = null

    val alpha: Float get() = fadeIn.alphaAt(SystemClock.uptimeMillis())

    fun fadeIn() {
        fadeIn = SnapshotFadeIn(SystemClock.uptimeMillis(), durationMillis())
        frames?.cancel()
        frames = scope.launch {
            while (fadeIn.isRunningAt(SystemClock.uptimeMillis())) {
                awaitVsync()
                onFrame()
            }
        }
    }

    fun stop() {
        frames?.cancel()
        fadeIn = SnapshotFadeIn.Done
    }

    private fun durationMillis(): Long {
        val areAnimationsOff = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()
        return if (areAnimationsOff) 0L else FADE_IN_MILLIS
    }
}
