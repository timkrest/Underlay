package com.timkrest.underlay

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi

internal sealed interface OverlayWindow {

    @RequiresApi(Build.VERSION_CODES.S)
    fun applyBlurBehind(radiusPx: Int)

    @RequiresApi(Build.VERSION_CODES.S)
    fun clearBlurBehind()

    data class OfWindow(private val window: Window) : OverlayWindow {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun applyBlurBehind(radiusPx: Int) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = radiusPx }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun clearBlurBehind() {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = 0 }
        }
    }

    data class OfRootView(private val rootView: View, private val windowManager: WindowManager) : OverlayWindow {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun applyBlurBehind(radiusPx: Int) = updateLayoutParams {
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            blurBehindRadius = radiusPx
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun clearBlurBehind() = updateLayoutParams {
            flags = flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            blurBehindRadius = 0
        }

        private inline fun updateLayoutParams(block: WindowManager.LayoutParams.() -> Unit) {
            val params = rootView.layoutParams as? WindowManager.LayoutParams ?: return
            params.block()
            try {
                windowManager.updateViewLayout(rootView, params)
            } catch (viewNotAttached: IllegalArgumentException) {
                return
            }
        }
    }
}
