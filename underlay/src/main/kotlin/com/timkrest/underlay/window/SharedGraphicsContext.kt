// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.GraphicsContext
import com.timkrest.underlay.R

internal fun View.sharedGraphicsContext(): GraphicsContext? {
    val root = rootView as? ViewGroup ?: return null

    return root.getTag(R.id.underlay_graphics_context) as? GraphicsContext
        ?: GraphicsContext(root).also { root.setTag(R.id.underlay_graphics_context, it) }
}
