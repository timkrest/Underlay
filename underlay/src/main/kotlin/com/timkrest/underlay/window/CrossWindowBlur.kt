// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.timkrest.underlay.backdrop.SystemBlurBehind
import com.timkrest.underlay.backdrop.SystemBlurFactory
import java.util.function.Consumer

internal fun crossWindowBlurOrNull(overlay: OverlayWindow?, windowManager: WindowManager?): SystemBlurFactory? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || overlay == null || windowManager == null) return null

    return crossWindowBlur(overlay, windowManager)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun crossWindowBlur(overlay: OverlayWindow, windowManager: WindowManager): SystemBlurFactory =
    { onEnabledChange -> CrossWindowBlur(overlay, windowManager, onEnabledChange) }

@RequiresApi(Build.VERSION_CODES.S)
private class CrossWindowBlur(
    private val overlay: OverlayWindow,
    private val windowManager: WindowManager,
    onEnabledChange: () -> Unit,
) : SystemBlurBehind {

    private val listener = Consumer<Boolean> { if (!isReleased) onEnabledChange() }
    private var isReleased = false

    init {
        windowManager.addCrossWindowBlurEnabledListener(listener)
    }

    override fun request(radiusPx: Int): Boolean {
        if (!windowManager.isCrossWindowBlurEnabled) return false

        return overlay.hasBlurBehind(radiusPx) || overlay.applyBlurBehind(radiusPx)
    }

    override fun withdraw() {
        if (overlay.hasBlurBehind()) overlay.clearBlurBehind()
    }

    override fun release() {
        isReleased = true
        windowManager.removeCrossWindowBlurEnabledListener(listener)
        withdraw()
    }
}
