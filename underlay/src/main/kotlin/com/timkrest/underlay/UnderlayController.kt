// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.app.Activity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.getSystemService
import com.timkrest.underlay.backdrop.BlurRadius
import com.timkrest.underlay.backdrop.UnderlayBackdrop
import com.timkrest.underlay.blur.SnapshotBlur
import com.timkrest.underlay.snapshot.HostSnapshot
import com.timkrest.underlay.snapshot.OriginInHost
import com.timkrest.underlay.window.DecorViewEvents
import com.timkrest.underlay.window.UnderlayWindows
import com.timkrest.underlay.window.WindowCapture
import com.timkrest.underlay.window.crossWindowBlurOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus

internal class UnderlayController(
    private val blur: SnapshotBlur,
    scope: CoroutineScope,
    private val onSourceChange: (UnderlaySource) -> Unit,
    private val onRedraw: () -> Unit,
) {

    private class Bound(
        val view: View,
        val offsetInView: () -> Offset,
        val observedTree: ViewTreeObserver,
        val backdrop: UnderlayBackdrop,
        val windows: UnderlayWindows,
    )

    private val onMain = scope + Dispatchers.Main.immediate
    private val originInHost = OriginInHost()
    private val fader = SnapshotFader(onMain, onRedraw)
    private val beforeDraw = ViewTreeObserver.OnPreDrawListener {
        onBeforeDraw()
        true
    }
    private var bound: Bound? = null
    private var drawnOrigin: IntOffset? = null

    var source: UnderlaySource? = null
        private set

    val snapshot: ImageBitmap? get() = bound?.backdrop?.image

    val snapshotAlpha: Float get() = fader.alpha

    fun recordDrawnOrigin(): IntOffset? = currentOrigin().also { drawnOrigin = it }

    fun bind(
        view: View,
        activity: Activity?,
        radius: BlurRadius,
        isLive: Boolean,
        offsetInView: () -> Offset = { Offset.Zero },
    ) {
        val windowManager = view.context.getSystemService<WindowManager>()
        val windows = UnderlayWindows.resolve(view, activity, windowManager)
        val host = windows.host
        val backdrop = UnderlayBackdrop(
            systemBlur = crossWindowBlurOrNull(windows.overlay, windowManager),
            snapshot = host?.let { HostSnapshot(WindowCapture(it, windows::mayShowHost), blur, onMain, ::onBackdropChange) },
            hostEvents = host?.let {
                { onResize, onDraw -> DecorViewEvents(it.decorView, onResize, onDraw, ::dropSnapshotIfHostTurnedSecure) }
            },
            onChange = ::onBackdropChange,
        )
        val observedTree = view.viewTreeObserver.also { it.addOnPreDrawListener(beforeDraw) }

        bound = Bound(view, offsetInView, observedTree, backdrop, windows)
        backdrop.start(radius, isLive)
    }

    fun unbind() {
        bound?.let {
            val tree = it.observedTree.takeIf(ViewTreeObserver::isAlive) ?: it.view.viewTreeObserver
            tree.removeOnPreDrawListener(beforeDraw)
            it.backdrop.release()
        }
        bound = null
        drawnOrigin = null
        fader.stop()
    }

    fun release() {
        unbind()
        blur.release()
        source = null
    }

    fun setRadius(radius: BlurRadius) {
        bound?.backdrop?.setRadius(radius)
    }

    fun setLive(isLive: Boolean) {
        bound?.backdrop?.setLive(isLive)
    }

    fun recapture() {
        bound?.backdrop?.recapture()
    }

    private fun dropSnapshotIfHostTurnedSecure() {
        val bound = bound ?: return
        if (bound.backdrop.image != null && !bound.windows.mayShowHost()) bound.backdrop.startOver()
    }

    private fun onBeforeDraw() {
        val backdrop = bound?.backdrop ?: return
        backdrop.reassertSystemBlur()
        dropSnapshotIfHostTurnedSecure()
        if (source == UnderlaySource.Snapshot && currentOrigin() != drawnOrigin) onRedraw()
    }

    private fun currentOrigin(): IntOffset? {
        val bound = bound ?: return null
        val host = bound.windows.host ?: return null

        return originInHost.of(bound.view, host, bound.offsetInView())
    }

    private fun onBackdropChange() {
        val bound = bound ?: return
        val backdrop = bound.backdrop
        val wasShowingSnapshot = source == UnderlaySource.Snapshot

        report(backdrop.source)
        if (!wasShowingSnapshot && backdrop.image != null) fader.fadeIn(bound.view.context.animatorDurationScale())
        onRedraw()
    }

    private fun report(source: UnderlaySource) {
        if (source == this.source) return

        this.source = source
        onSourceChange(source)
    }
}
