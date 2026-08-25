package com.timkrest.underlay

import android.app.Activity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.getSystemService
import androidx.core.content.res.use

internal class UnderlayWindows(val overlay: OverlayWindow?, val host: Window?)

@Composable
internal fun rememberUnderlayWindows(): UnderlayWindows {
    val view = LocalView.current
    val activity = LocalActivity.current
    val windowManager = LocalContext.current.getSystemService<WindowManager>()

    return remember(view, activity, windowManager) {
        resolveUnderlayWindows(view, activity, windowManager)
    }
}

private fun resolveUnderlayWindows(view: View, activity: Activity?, windowManager: WindowManager?): UnderlayWindows {
    val activityWindow = activity?.window

    if (view.rootView === activityWindow?.decorView) {
        val overlay = if (activityWindow.isOverlayWindow()) OverlayWindow.OfWindow(activityWindow) else null
        return UnderlayWindows(overlay = overlay, host = null)
    }

    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
    val overlay = when {
        dialogWindow != null -> OverlayWindow.OfWindow(dialogWindow)
        windowManager != null -> OverlayWindow.OfRootView(view.rootView, windowManager)
        else -> null
    }
    return UnderlayWindows(overlay = overlay, host = activityWindow)
}

private fun Window.isOverlayWindow(): Boolean =
    isFloating ||
        context.theme
            .obtainStyledAttributes(intArrayOf(android.R.attr.windowIsTranslucent))
            .use { it.getBoolean(0, false) }
