// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.backdrop

internal class SnapshotFadeIn(private val startedAtMillis: Long, private val durationMillis: Long) {

    fun alphaAt(nowMillis: Long): Float {
        if (durationMillis <= 0L) return 1f

        return ((nowMillis - startedAtMillis).toFloat() / durationMillis).coerceIn(0f, 1f)
    }

    fun isRunningAt(nowMillis: Long): Boolean = alphaAt(nowMillis) < 1f

    companion object {
        val Done = SnapshotFadeIn(startedAtMillis = 0L, durationMillis = 0L)
    }
}
