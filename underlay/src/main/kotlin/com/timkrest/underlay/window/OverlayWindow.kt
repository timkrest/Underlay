// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi

internal sealed interface OverlayWindow {

    val isSecure: Boolean

    @RequiresApi(Build.VERSION_CODES.S)
    fun hasBlurBehind(radiusPx: Int? = null): Boolean

    @RequiresApi(Build.VERSION_CODES.S)
    fun applyBlurBehind(radiusPx: Int): Boolean

    @RequiresApi(Build.VERSION_CODES.S)
    fun clearBlurBehind()

    data class OfWindow(private val window: Window) : OverlayWindow {

        override val isSecure: Boolean get() = window.isSecure

        @RequiresApi(Build.VERSION_CODES.S)
        override fun hasBlurBehind(radiusPx: Int?): Boolean = window.attributes.hasBlurBehind(radiusPx)

        @RequiresApi(Build.VERSION_CODES.S)
        override fun applyBlurBehind(radiusPx: Int): Boolean {
            window.attributes = window.attributes.apply { setBlurBehind(radiusPx) }
            return true
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun clearBlurBehind() {
            window.attributes = window.attributes.apply { setBlurBehind(radiusPx = null) }
        }
    }

    data class OfRootView(private val rootView: View, private val windowManager: WindowManager) : OverlayWindow {

        private val params: WindowManager.LayoutParams? get() = rootView.layoutParams as? WindowManager.LayoutParams

        override val isSecure: Boolean get() = params?.isSecure == true

        @RequiresApi(Build.VERSION_CODES.S)
        override fun hasBlurBehind(radiusPx: Int?): Boolean = params?.hasBlurBehind(radiusPx) == true

        @RequiresApi(Build.VERSION_CODES.S)
        override fun applyBlurBehind(radiusPx: Int): Boolean = update(radiusPx)

        @RequiresApi(Build.VERSION_CODES.S)
        override fun clearBlurBehind() {
            update(radiusPx = null)
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun update(radiusPx: Int?): Boolean {
            val params = params ?: return false
            val previousFlags = params.flags
            val previousRadius = params.blurBehindRadius
            params.setBlurBehind(radiusPx)

            return try {
                windowManager.updateViewLayout(rootView, params)
                true
            } catch (viewNotAttached: IllegalArgumentException) {
                params.flags = previousFlags
                params.blurBehindRadius = previousRadius
                false
            }
        }
    }
}

internal val Window.isSecure: Boolean get() = attributes.isSecure

private val WindowManager.LayoutParams.isSecure: Boolean
    get() = flags and WindowManager.LayoutParams.FLAG_SECURE != 0

@RequiresApi(Build.VERSION_CODES.S)
private fun WindowManager.LayoutParams.hasBlurBehind(radiusPx: Int?): Boolean =
    flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND != 0 && (radiusPx == null || blurBehindRadius == radiusPx)

@RequiresApi(Build.VERSION_CODES.S)
private fun WindowManager.LayoutParams.setBlurBehind(radiusPx: Int?) {
    flags = if (radiusPx != null) {
        flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
    } else {
        flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
    }
    blurBehindRadius = radiusPx ?: 0
}
