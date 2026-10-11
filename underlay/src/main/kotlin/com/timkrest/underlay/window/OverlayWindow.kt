// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi

internal data class BlurBehind(val isOn: Boolean, val radiusPx: Int) {

    companion object {
        val Off = BlurBehind(isOn = false, radiusPx = 0)

        fun on(radiusPx: Int) = BlurBehind(isOn = true, radiusPx = radiusPx)
    }
}

internal sealed interface OverlayWindow {

    val isSecure: Boolean

    @get:RequiresApi(Build.VERSION_CODES.S)
    val blurBehind: BlurBehind?

    @RequiresApi(Build.VERSION_CODES.S)
    fun setBlurBehind(blurBehind: BlurBehind): Boolean

    data class OfWindow(private val window: Window) : OverlayWindow {

        override val isSecure: Boolean get() = window.isSecure

        @get:RequiresApi(Build.VERSION_CODES.S)
        override val blurBehind: BlurBehind get() = window.attributes.blurBehind

        @RequiresApi(Build.VERSION_CODES.S)
        override fun setBlurBehind(blurBehind: BlurBehind): Boolean {
            window.attributes = window.attributes.apply { this.blurBehind = blurBehind }
            return true
        }
    }

    data class OfRootView(private val rootView: View, private val windowManager: WindowManager) : OverlayWindow {

        private val params: WindowManager.LayoutParams? get() = rootView.layoutParams as? WindowManager.LayoutParams

        override val isSecure: Boolean get() = params?.isSecure == true

        @get:RequiresApi(Build.VERSION_CODES.S)
        override val blurBehind: BlurBehind? get() = params?.blurBehind

        @RequiresApi(Build.VERSION_CODES.S)
        override fun setBlurBehind(blurBehind: BlurBehind): Boolean {
            val params = params ?: return false
            val previous = params.blurBehind
            params.blurBehind = blurBehind

            return try {
                windowManager.updateViewLayout(rootView, params)
                true
            } catch (viewNotAttached: IllegalArgumentException) {
                params.blurBehind = previous
                false
            }
        }
    }
}

internal val Window.isSecure: Boolean get() = attributes.isSecure

private val WindowManager.LayoutParams.isSecure: Boolean
    get() = flags and WindowManager.LayoutParams.FLAG_SECURE != 0

private var WindowManager.LayoutParams.blurBehind: BlurBehind
    @RequiresApi(Build.VERSION_CODES.S)
    get() = BlurBehind(isOn = flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND != 0, radiusPx = blurBehindRadius)

    @RequiresApi(Build.VERSION_CODES.S)
    set(value) {
        flags = if (value.isOn) {
            flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        } else {
            flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        }
        blurBehindRadius = value.radiusPx
    }
