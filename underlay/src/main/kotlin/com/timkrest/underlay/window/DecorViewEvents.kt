// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.ui.unit.IntSize
import com.timkrest.underlay.backdrop.HostWindowEvents

internal class DecorViewEvents(
    private val decorView: View,
    private val onResize: () -> Unit,
    onDraw: () -> Unit,
    onPreDraw: () -> Unit,
) : HostWindowEvents {

    private var size = IntSize(decorView.width, decorView.height)
    private var isFollowingDraws = false

    private val layoutListener = View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
        onLayout(IntSize(right - left, bottom - top))
    }
    private val drawListener = ViewTreeObserver.OnDrawListener { onDraw() }
    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        onPreDraw()
        true
    }

    init {
        decorView.addOnLayoutChangeListener(layoutListener)
        decorView.viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }

    override fun followDraws(isFollowing: Boolean) {
        val observer = decorView.viewTreeObserver
        if (isFollowing == isFollowingDraws || !observer.isAlive) return

        isFollowingDraws = isFollowing
        if (isFollowing) observer.addOnDrawListener(drawListener) else observer.removeOnDrawListener(drawListener)
    }

    override fun release() {
        followDraws(isFollowing = false)
        decorView.removeOnLayoutChangeListener(layoutListener)
        val observer = decorView.viewTreeObserver
        if (observer.isAlive) observer.removeOnPreDrawListener(preDrawListener)
    }

    private fun onLayout(newSize: IntSize) {
        if (newSize == size) return

        size = newSize
        onResize()
    }
}
