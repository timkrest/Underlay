// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import java.util.function.Consumer

internal interface SystemBlurBehind {

    fun request(radiusPx: Int): Boolean

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

    override fun request(radiusPx: Int): Boolean {
        if (!windowManager.isCrossWindowBlurEnabled) return false
        if (appliedRadiusPx == radiusPx) return true
        if (!overlay.takesBlurBehind(radiusPx)) return false

        appliedRadiusPx = radiusPx
        return true
    }

    override fun withdraw() {
        if (appliedRadiusPx == null) return

        overlay.dropsBlurBehind()
        appliedRadiusPx = null
    }

    override fun release() {
        isReleased = true
        windowManager.removeCrossWindowBlurEnabledListener(listener)
        withdraw()
    }
}
