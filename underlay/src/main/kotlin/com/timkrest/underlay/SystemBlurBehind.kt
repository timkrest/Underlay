// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import java.util.function.Consumer

/** `FLAG_BLUR_BEHIND` on the overlay window. Battery saver turns it off while the overlay is open. */
internal interface SystemBlurBehind {

    val isEnabled: Boolean

    fun request(radiusPx: Int)

    fun withdraw()

    fun release()
}

internal fun systemBlurBehind(
    overlay: OverlayWindow?,
    windowManager: WindowManager?,
    onEnabledChange: () -> Unit,
): SystemBlurBehind? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || overlay == null || windowManager == null) return null

    return CrossWindowBlur(overlay, windowManager, onEnabledChange)
}

@RequiresApi(Build.VERSION_CODES.S)
private class CrossWindowBlur(
    private val overlay: OverlayWindow,
    private val windowManager: WindowManager,
    onEnabledChange: () -> Unit,
) : SystemBlurBehind {

    private val listener = Consumer<Boolean> { if (!isReleased) onEnabledChange() }
    private var appliedRadiusPx: Int? = null
    private var isReleased = false

    init {
        windowManager.addCrossWindowBlurEnabledListener(listener)
    }

    override val isEnabled: Boolean get() = windowManager.isCrossWindowBlurEnabled

    override fun request(radiusPx: Int) {
        if (appliedRadiusPx == radiusPx) return

        overlay.applyBlurBehind(radiusPx)
        appliedRadiusPx = radiusPx
    }

    override fun withdraw() {
        if (appliedRadiusPx == null) return

        overlay.clearBlurBehind()
        appliedRadiusPx = null
    }

    override fun release() {
        isReleased = true
        windowManager.removeCrossWindowBlurEnabledListener(listener)
        withdraw()
    }
}
