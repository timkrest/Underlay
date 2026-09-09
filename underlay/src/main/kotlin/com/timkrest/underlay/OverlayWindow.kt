// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi

internal sealed interface OverlayWindow {

    @RequiresApi(Build.VERSION_CODES.S)
    fun takesBlurBehind(radiusPx: Int): Boolean

    @RequiresApi(Build.VERSION_CODES.S)
    fun dropsBlurBehind(): Boolean

    data class OfWindow(private val window: Window) : OverlayWindow {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun takesBlurBehind(radiusPx: Int): Boolean {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = radiusPx }

            return true
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun dropsBlurBehind(): Boolean {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = 0 }

            return true
        }
    }

    data class OfRootView(private val rootView: View, private val windowManager: WindowManager) : OverlayWindow {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun takesBlurBehind(radiusPx: Int): Boolean = updateLayoutParams {
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            blurBehindRadius = radiusPx
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun dropsBlurBehind(): Boolean = updateLayoutParams {
            flags = flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            blurBehindRadius = 0
        }

        private inline fun updateLayoutParams(block: WindowManager.LayoutParams.() -> Unit): Boolean {
            val params = rootView.layoutParams as? WindowManager.LayoutParams ?: return false
            params.block()

            return try {
                windowManager.updateViewLayout(rootView, params)
                true
            } catch (viewNotAttached: IllegalArgumentException) {
                false
            }
        }
    }
}
