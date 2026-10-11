// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.timkrest.underlay.backdrop.SnapshotFadeIn
import com.timkrest.underlay.window.awaitVsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private const val FADE_IN_MILLIS = 200L

internal class SnapshotFader(private val scope: CoroutineScope, private val onFrame: () -> Unit) {

    private var fadeIn = SnapshotFadeIn.Done
    private var frames: Job? = null

    val alpha: Float get() = fadeIn.alphaAt(SystemClock.uptimeMillis())

    fun fadeIn(durationScale: Float) {
        fadeIn = SnapshotFadeIn(SystemClock.uptimeMillis(), (FADE_IN_MILLIS * durationScale).roundToLong())
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
}

internal fun Context.animatorDurationScale(): Float {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()) return 0f

    return Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
}
