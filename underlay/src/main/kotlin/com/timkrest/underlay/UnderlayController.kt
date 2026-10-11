// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.app.Activity
import android.content.Context
import android.view.View
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

    private class Bound(val backdrop: UnderlayBackdrop, val windows: UnderlayWindows, val context: Context)

    private val onMain = scope + Dispatchers.Main.immediate
    private val originInHost = OriginInHost()
    private val fader = SnapshotFader(onMain, onRedraw)
    private var bound: Bound? = null
    private var drawnOrigin: IntOffset? = null

    var reportedSource: UnderlaySource? = null
        private set

    val source: UnderlaySource get() = bound?.backdrop?.source ?: UnderlaySource.Fallback

    val snapshot: ImageBitmap? get() = bound?.backdrop?.image

    val snapshotAlpha: Float get() = fader.alpha

    fun originToDraw(view: View, offsetInView: Offset = Offset.Zero): IntOffset? =
        originOf(view, offsetInView).also { drawnOrigin = it }

    fun needsRedrawBeforeDraw(view: View, offsetInView: Offset = Offset.Zero): Boolean {
        val backdrop = bound?.backdrop ?: return false
        backdrop.reassertSystemBlur()
        dropSnapshotIfHostTurnedSecure()

        return backdrop.source == UnderlaySource.Snapshot && originOf(view, offsetInView) != drawnOrigin
    }

    fun bind(view: View, activity: Activity?, radius: BlurRadius, isLive: Boolean) {
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

        bound = Bound(backdrop, windows, view.context)
        backdrop.start(radius, isLive)
    }

    fun unbind() {
        bound?.backdrop?.release()
        bound = null
        drawnOrigin = null
        fader.stop()
    }

    fun release() {
        unbind()
        blur.release()
        reportedSource = null
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

    private fun originOf(view: View, offsetInView: Offset): IntOffset? =
        bound?.windows?.host?.let { host -> originInHost.of(view, host, offsetInView) }

    private fun onBackdropChange() {
        val bound = bound ?: return
        val backdrop = bound.backdrop
        val wasShowingSnapshot = reportedSource == UnderlaySource.Snapshot

        report(backdrop.source)
        if (!wasShowingSnapshot && backdrop.image != null) fader.fadeIn(bound.context.animatorDurationScale())
        onRedraw()
    }

    private fun report(source: UnderlaySource) {
        if (source == reportedSource) return

        reportedSource = source
        onSourceChange(source)
    }
}
