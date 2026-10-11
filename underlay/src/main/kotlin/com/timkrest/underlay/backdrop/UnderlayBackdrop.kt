// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.backdrop

import androidx.compose.ui.graphics.ImageBitmap
import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.snapshot.HostSnapshot

internal class UnderlayBackdrop(
    systemBlur: SystemBlurFactory?,
    private val snapshot: HostSnapshot?,
    hostEvents: HostWindowEventsFactory?,
    private val onChange: () -> Unit,
) {

    private enum class Rung { SystemBlur, Snapshot }

    private var rung: Rung? = null
    private var radius = BlurRadius.None
    private var isLive = false
    private var isReleased = false

    private val systemBlur = systemBlur?.invoke(::climbLadder)
    private val hostEvents = hostEvents?.invoke(::startOver, ::onHostDrawn)

    val image: ImageBitmap? get() = snapshot?.image

    val source: UnderlaySource
        get() = when {
            rung == Rung.SystemBlur -> UnderlaySource.SystemBlur
            snapshot == null -> UnderlaySource.Fallback
            else -> snapshot.source
        }

    fun start(radius: BlurRadius, isLive: Boolean) {
        this.radius = radius
        this.isLive = isLive
        climbLadder()
    }

    fun setLive(isLive: Boolean) {
        this.isLive = isLive
        followHostDraws()
    }

    fun setRadius(radius: BlurRadius) {
        this.radius = radius
        if (rung == Rung.SystemBlur) climbLadder() else snapshot?.reblur(radius.snapshotSigma)
    }

    fun reassertSystemBlur() {
        if (rung == Rung.SystemBlur) climbLadder()
    }

    fun recapture() {
        if (rung == Rung.SystemBlur) return

        snapshot?.capture(radius.snapshotSigma)
    }

    fun startOver() {
        if (isReleased) return

        snapshot?.discard()
        recapture()
        onChange()
    }

    fun release() {
        isReleased = true
        hostEvents?.release()
        systemBlur?.release()
        snapshot?.discard()
    }

    private fun climbLadder() {
        if (isReleased) return

        val reached = if (systemBlur?.request(radius.windowPx) == true) Rung.SystemBlur else Rung.Snapshot
        if (reached == rung) return

        rung = reached
        when (reached) {
            Rung.SystemBlur -> snapshot?.discard()
            Rung.Snapshot -> {
                systemBlur?.withdraw()
                snapshot?.capture(radius.snapshotSigma)
            }
        }
        followHostDraws()
        onChange()
    }

    private fun onHostDrawn() {
        if (isReleased) return

        snapshot?.captureWhenIdle(radius.snapshotSigma)
    }

    private fun followHostDraws() {
        hostEvents?.followDraws(isLive && snapshot != null && rung == Rung.Snapshot)
    }
}
