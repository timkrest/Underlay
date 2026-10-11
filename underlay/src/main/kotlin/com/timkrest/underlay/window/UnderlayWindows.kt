// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.app.Activity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.res.use

internal class UnderlayWindows(val overlay: OverlayWindow?, val host: Window?) {

    companion object {

        fun resolve(view: View, activity: Activity?, windowManager: WindowManager?): UnderlayWindows {
            val activityWindow = activity?.window

            if (view.rootView === activityWindow?.decorView) {
                val overlay = if (activityWindow.isOverlay()) OverlayWindow.OfWindow(activityWindow) else null
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

        private fun Window.isOverlay(): Boolean =
            isFloating ||
                context.theme
                    .obtainStyledAttributes(intArrayOf(android.R.attr.windowIsTranslucent))
                    .use { it.getBoolean(0, false) }
    }
}
