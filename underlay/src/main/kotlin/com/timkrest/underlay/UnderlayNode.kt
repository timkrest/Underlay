// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.app.Activity
import android.content.res.Configuration
import android.view.ViewTreeObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.timkrest.underlay.backdrop.BlurRadius
import com.timkrest.underlay.blur.SnapshotBlur
import com.timkrest.underlay.snapshot.drawSnapshot
import com.timkrest.underlay.window.findActivity

internal class UnderlayNode(
    private var blurRadius: Dp,
    private var tint: Color,
    private var fallback: Color,
    private var liveSnapshot: Boolean,
    private var state: UnderlayState?,
    private var onSourceChange: ((UnderlaySource) -> Unit)?,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode,
    GlobalPositionAwareModifierNode {

    private var controller: UnderlayController? = null
    private var activity: Activity? = null
    private var configuration: Configuration? = null
    private var refreshes = 0
    private var positionInRoot = Offset.Zero
    private var observedTree: ViewTreeObserver? = null
    private val beforeDraw = ViewTreeObserver.OnPreDrawListener {
        if (controller?.needsRedrawBeforeDraw(requireView(), positionInRoot) == true) invalidateDraw()
        true
    }

    override fun onAttach() {
        controller = UnderlayController(
            blur = SnapshotBlur.bestAvailable { requireGraphicsContext() },
            scope = coroutineScope,
            onSourceChange = ::onSourceReported,
            onRedraw = ::redraw,
        )
        readHostContext()
        bindWindows()
        observedTree = requireView().viewTreeObserver.also { it.addOnPreDrawListener(beforeDraw) }
    }

    override fun onDetach() {
        observedTree?.takeIf { it.isAlive }?.removeOnPreDrawListener(beforeDraw)
        observedTree = null
        controller?.release()
        controller = null
        state?.withdraw(this)
        activity = null
        configuration = null
        positionInRoot = Offset.Zero
    }

    override fun onObservedReadsChanged() {
        val previousActivity = activity
        val previousConfiguration = configuration
        val previousRefreshes = refreshes
        readHostContext()

        when {
            activity !== previousActivity -> {
                controller?.unbind()
                bindWindows()
            }
            configuration != previousConfiguration || refreshes != previousRefreshes -> controller?.recapture()
        }
    }

    override fun onDensityChange() {
        controller?.setRadius(blurRadiusInPx())
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        positionInRoot = coordinates.positionInRoot()
    }

    fun update(
        blurRadius: Dp,
        tint: Color,
        fallback: Color,
        liveSnapshot: Boolean,
        state: UnderlayState?,
        onSourceChange: ((UnderlaySource) -> Unit)?,
    ) {
        val isRadiusNew = this.blurRadius != blurRadius
        val isLivenessNew = this.liveSnapshot != liveSnapshot
        val previousState = this.state
        val isStateNew = previousState !== state
        val isRepaintNeeded = this.tint != tint || this.fallback != fallback

        this.blurRadius = blurRadius
        this.tint = tint
        this.fallback = fallback
        this.liveSnapshot = liveSnapshot
        this.state = state
        this.onSourceChange = onSourceChange
        if (!isAttached) return

        if (isRadiusNew) controller?.setRadius(blurRadiusInPx())
        if (isLivenessNew) controller?.setLive(liveSnapshot)
        if (isStateNew) {
            previousState?.withdraw(this)
            state?.report(this, controller?.reportedSource)
            readHostContext()
        }
        if (isRepaintNeeded) invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val source = controller?.source ?: UnderlaySource.Fallback
        if (source == UnderlaySource.Snapshot) drawHostSnapshot()
        drawRect(if (source == UnderlaySource.Fallback) fallback else tint)
        drawContent()
    }

    private fun DrawScope.drawHostSnapshot() {
        val controller = controller ?: return
        val snapshot = controller.snapshot ?: return
        val origin = controller.originToDraw(requireView(), requireLayoutCoordinates().positionInRoot()) ?: return

        drawSnapshot(snapshot, origin, controller.snapshotAlpha)
    }

    private fun bindWindows() {
        controller?.bind(requireView(), activity, blurRadiusInPx(), liveSnapshot)
    }

    private fun readHostContext() {
        observeReads {
            activity = currentValueOf(LocalContext).findActivity()
            configuration = currentValueOf(LocalConfiguration)
            refreshes = state?.refreshes ?: 0
        }
    }

    private fun blurRadiusInPx(): BlurRadius = BlurRadius.ofPx(with(requireDensity()) { blurRadius.toPx() })

    private fun onSourceReported(source: UnderlaySource) {
        if (!isAttached) return

        state?.report(this, source)
        onSourceChange?.invoke(source)
    }

    private fun redraw() {
        if (isAttached) invalidateDraw()
    }
}
