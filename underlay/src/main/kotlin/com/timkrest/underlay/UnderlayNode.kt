package com.timkrest.underlay

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
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
import androidx.compose.ui.unit.IntSize
import androidx.core.content.getSystemService

internal class UnderlayNode(
    private var blurRadius: Dp,
    private var tint: Color,
    private var fallback: Color,
    private var state: UnderlayState?,
    private var onSourceChange: ((UnderlaySource) -> Unit)?,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode {

    private var windowManager: WindowManager? = null
    private var layer: GraphicsLayer? = null
    private var blur: SnapshotBlur? = null

    private var host: Window? = null
    private var backdrop: UnderlayBackdrop? = null

    private var activity: Activity? = null
    private var configuration: Configuration? = null
    private var refreshes: Int = 0
    private var hostSize = IntSize.Zero
    private var reportedSource: UnderlaySource? = null

    private val onHostLayoutChange = View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
        onHostResized(IntSize(right - left, bottom - top))
    }

    override fun onAttach() {
        windowManager = requireView().context.getSystemService()
        layer = requireGraphicsContext().createGraphicsLayer().also { blur = snapshotBlur(it) }

        observeHostContext()
        bindWindows()
    }

    override fun onDetach() {
        unbindWindows()
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
        blur = null
        reportedSource = null
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
        state: UnderlayState?,
        onSourceChange: ((UnderlaySource) -> Unit)?,
    ) {
        val isRadiusNew = this.blurRadius != blurRadius
        val isStateNew = this.state !== state
        val isRepaintNeeded = isRadiusNew || this.tint != tint || this.fallback != fallback

        this.blurRadius = blurRadius
        this.tint = tint
        this.fallback = fallback
        this.state = state
        this.onSourceChange = onSourceChange
        if (!isAttached) return

        if (isRadiusNew) backdrop?.reblur(blurRadius, requireDensity())
        if (isStateNew) observeHostContext()
        if (isRepaintNeeded) invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        when (source()) {
            UnderlaySource.SystemBlur -> drawRect(tint)
            UnderlaySource.Snapshot -> {
                val image = backdrop?.image
                val host = host
                if (image != null && host != null) {
                    // Read here, not on placement: an ancestor can move this composable without re-placing it.
                    drawSnapshot(image, originInHost(requireView(), host, requireLayoutCoordinates().positionInRoot()))
                }
                drawRect(tint)
            }
            UnderlaySource.Pending -> drawRect(tint)
            UnderlaySource.Fallback -> drawRect(fallback)
        }
        drawContent()
    }

    private fun bindWindows() {
        val windows = resolveUnderlayWindows(requireView(), activity, windowManager)
        val engine = blur ?: return

        host = windows.host
        windows.host?.decorView?.let { decorView ->
            hostSize = IntSize(decorView.width, decorView.height)
            decorView.addOnLayoutChangeListener(onHostLayoutChange)
        }
        val backdrop = UnderlayBackdrop(windows, windowManager, engine, coroutineScope, ::onBackdropChanged)
        this.backdrop = backdrop
        backdrop.start(blurRadius, requireDensity())
    }

    private fun unbindWindows() {
        host?.decorView?.removeOnLayoutChangeListener(onHostLayoutChange)
        backdrop?.release()

        host = null
        backdrop = null
        hostSize = IntSize.Zero
    }

    private fun onHostResized(size: IntSize) {
        if (size == hostSize) return

        hostSize = size
        backdrop?.dropAndRecapture()
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
        onSourceChange?.invoke(source)
    }
}
