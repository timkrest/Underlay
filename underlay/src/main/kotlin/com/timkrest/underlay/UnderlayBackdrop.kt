package com.timkrest.underlay

import android.view.WindowManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope

/** Picks where one overlay's blur comes from: the system flag while it is on, a snapshot otherwise. */
internal class UnderlayBackdrop(
    windows: UnderlayWindows,
    windowManager: WindowManager?,
    blur: SnapshotBlur,
    scope: CoroutineScope,
    private val onChange: () -> Unit,
) {

    private val systemBlur = systemBlurBehind(windows.overlay, windowManager, ::chooseSource)
    private val snapshot = windows.host?.let { host -> HostSnapshot(host, blur, scope, onChange) }

    private var radius: Dp = Dp.Hairline
    private var density: Density = Density(1f)

    val image: ImageBitmap? get() = snapshot?.image

    val source: UnderlaySource
        get() = when {
            systemBlur?.isEnabled == true -> UnderlaySource.SystemBlur
            snapshot?.image != null -> UnderlaySource.Snapshot
            snapshot == null || snapshot.hasFailed -> UnderlaySource.Fallback
            else -> UnderlaySource.Pending
        }

    fun start(radius: Dp, density: Density) {
        this.radius = radius
        this.density = density
        chooseSource()
    }

    fun reblur(radius: Dp, density: Density) {
        this.radius = radius
        this.density = density
        if (systemBlur?.isEnabled == true) systemBlur.request(radiusPx()) else snapshot?.reblur(sigma())
    }

    fun recapture() {
        if (systemBlur?.isEnabled == true) return

        snapshot?.capture(sigma())
    }

    fun dropAndRecapture() {
        snapshot?.discard()
        recapture()
        onChange()
    }

    fun release() {
        systemBlur?.release()
        snapshot?.discard()
    }

    private fun chooseSource() {
        val systemBlur = systemBlur
        if (systemBlur != null && systemBlur.isEnabled) {
            systemBlur.request(radiusPx())
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
