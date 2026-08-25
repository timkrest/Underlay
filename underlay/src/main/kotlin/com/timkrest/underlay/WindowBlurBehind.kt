package com.timkrest.underlay

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.content.getSystemService
import java.util.function.Consumer

/**
 * Asks the system to blur behind [overlay], and reports whether it agreed. The flag is set once per
 * overlay and cleared when it goes away or the system turns cross-window blur off; [radius] alone
 * updates in place.
 */
@Composable
internal fun rememberWindowBlurBehind(overlay: OverlayWindow?, radius: Dp): Boolean {
    val windowManager = LocalContext.current.getSystemService<WindowManager>()
    val radiusPx = with(LocalDensity.current) { radius.roundToPx() }
    val isBlurActive = rememberCrossWindowBlurEnabled(overlay, windowManager)
    val blurredOverlay = overlay.takeIf { isBlurActive }
    val applied = remember(blurredOverlay) { AppliedBlurRadius() }

    // An effect keyed on the radius would clear the flag and re-add it on every step of a drag.
    // The guard keeps an unrelated recomposition from touching the window at all.
    SideEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurredOverlay != null && applied.px != radiusPx) {
            blurredOverlay.applyBlurBehind(radiusPx)
            applied.px = radiusPx
        }
    }

    DisposableEffect(blurredOverlay) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blurredOverlay?.clearBlurBehind()
        }
    }

    return isBlurActive
}

/** The radius already on the window. */
private class AppliedBlurRadius {
    var px: Int? = null
}

@Composable
private fun rememberCrossWindowBlurEnabled(overlay: OverlayWindow?, windowManager: WindowManager?): Boolean {
    var isEnabled by remember(overlay, windowManager) {
        mutableStateOf(overlay != null && windowManager.isCrossWindowBlurEnabled())
    }

    DisposableEffect(overlay, windowManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && overlay != null && windowManager != null) {
            val listener = Consumer<Boolean> { enabled -> isEnabled = enabled }
            windowManager.addCrossWindowBlurEnabledListener(listener)

            onDispose { windowManager.removeCrossWindowBlurEnabledListener(listener) }
        } else {
            onDispose { }
        }
    }

    return isEnabled
}

private fun WindowManager?.isCrossWindowBlurEnabled(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && this != null && isCrossWindowBlurEnabled
