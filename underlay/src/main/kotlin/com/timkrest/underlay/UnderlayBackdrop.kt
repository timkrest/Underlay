// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope

internal class UnderlayBackdrop(
    private val windows: UnderlayWindows,
    windowManager: WindowManager?,
    blur: SnapshotBlur,
    scope: CoroutineScope,
    private val onChange: () -> Unit,
) {

    private val systemBlur = systemBlurBehind(windows.overlay, windowManager, ::chooseSource)
    private val snapshot = windows.host?.let { host -> HostSnapshot(host, blur, scope, onChange) }

    private val onHostLayoutChange = View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
        onHostResized(IntSize(right - left, bottom - top))
    }

    private var radius: Dp = Dp.Hairline
    private var density: Density = Density(1f)
    private var hostSize = IntSize.Zero
    private var isSystemBlurApplied: Boolean? = null

    val host: Window? get() = windows.host

    val image: ImageBitmap? get() = snapshot?.image

    val source: UnderlaySource
        get() = when {
            isSystemBlurApplied == true -> UnderlaySource.SystemBlur
            snapshot?.image != null -> UnderlaySource.Snapshot
            snapshot == null || snapshot.hasFailed -> UnderlaySource.Fallback
            else -> UnderlaySource.Pending
        }

    fun start(radius: Dp, density: Density) {
        this.radius = radius
        this.density = density
        windows.host?.decorView?.let { decorView ->
            hostSize = IntSize(decorView.width, decorView.height)
            decorView.addOnLayoutChangeListener(onHostLayoutChange)
        }
        chooseSource()
    }

    fun reblur(radius: Dp, density: Density) {
        this.radius = radius
        this.density = density
        if (isSystemBlurApplied == true) chooseSource() else snapshot?.reblur(sigma())
    }

    fun recapture() {
        if (isSystemBlurApplied == true) return

        snapshot?.capture(sigma())
    }

    fun release() {
        windows.host?.decorView?.removeOnLayoutChangeListener(onHostLayoutChange)
        systemBlur?.release()
        snapshot?.discard()
    }

    private fun onHostResized(size: IntSize) {
        if (size == hostSize) return

        hostSize = size
        snapshot?.discard()
        recapture()
        onChange()
    }

    private fun chooseSource() {
        val isApplied = systemBlur?.request(radiusPx()) == true
        if (isApplied == isSystemBlurApplied) return

        isSystemBlurApplied = isApplied
        if (isApplied) {
            snapshot?.discard()
        } else {
            systemBlur?.withdraw()
            snapshot?.capture(sigma())
        }
        onChange()
    }

    private fun radiusPx(): Int = with(density) { radius.roundToPx() }

    private fun sigma(): Float = with(density) { radius.toPx() / WINDOW_CAPTURE_DOWN_SCALE }
}
