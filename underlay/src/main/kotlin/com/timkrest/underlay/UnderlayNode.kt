// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.core.content.getSystemService

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
    ObserverModifierNode {

    private lateinit var blur: SnapshotBlur

    private var backdrop: UnderlayBackdrop? = null
    private var activity: Activity? = null
    private var configuration: Configuration? = null
    private var refreshes: Int = 0
    private var reportedSource: UnderlaySource? = null

    override fun onAttach() {
        blur = snapshotBlur(requireGraphicsContext())

        observeHostContext()
        bindWindows()
    }

    override fun onDetach() {
        unbindWindows()
        blur.release()
        reportedSource = null
        state?.source = null
        activity = null
        configuration = null
    }

    override fun onObservedReadsChanged() {
        val previousActivity = activity
        val previousConfiguration = configuration
        val previousRefreshes = refreshes
        observeHostContext()

        when {
            activity !== previousActivity -> {
                unbindWindows()
                bindWindows()
            }
            configuration != previousConfiguration || refreshes != previousRefreshes -> backdrop?.recapture()
        }
    }

    override fun onDensityChange() {
        backdrop?.reblur(blurRadius, requireDensity())
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

        if (isRadiusNew) backdrop?.reblur(blurRadius, requireDensity())
        if (isLivenessNew) backdrop?.follow(liveSnapshot)
        if (isStateNew) {
            previousState?.source = null
            state?.source = reportedSource
            observeHostContext()
        }
        if (isRepaintNeeded) invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        when (source()) {
            UnderlaySource.Snapshot -> {
                drawHostSnapshot()
                drawRect(tint)
            }
            UnderlaySource.SystemBlur, UnderlaySource.Pending -> drawRect(tint)
            UnderlaySource.Fallback -> drawRect(fallback)
        }
        drawContent()
    }

    private fun DrawScope.drawHostSnapshot() {
        val backdrop = backdrop ?: return
        val image = backdrop.image ?: return
        val host = backdrop.host ?: return
        val positionInRoot = requireLayoutCoordinates().positionInRoot()

        drawSnapshot(image, originInHost(requireView(), host, positionInRoot))
    }

    private fun bindWindows() {
        val view = requireView()
        val windowManager = view.context.getSystemService<WindowManager>()
        val windows = resolveUnderlayWindows(view, activity, windowManager)

        val backdrop = UnderlayBackdrop(windows, windowManager, blur, coroutineScope, ::onBackdropChanged)
        this.backdrop = backdrop
        backdrop.start(blurRadius, requireDensity(), liveSnapshot)
    }

    private fun unbindWindows() {
        backdrop?.release()
        backdrop = null
    }

    private fun onBackdropChanged() {
        if (!isAttached) return

        report()
        invalidateDraw()
    }

    private fun observeHostContext() {
        observeReads {
            activity = currentValueOf(LocalActivity)
            configuration = currentValueOf(LocalConfiguration)
            refreshes = state?.refreshes ?: 0
        }
    }

    private fun source(): UnderlaySource = backdrop?.source ?: UnderlaySource.Fallback

    private fun report() {
        val source = source()
        if (source == reportedSource) return

        reportedSource = source
        state?.source = source
        onSourceChange?.invoke(source)
    }
}
