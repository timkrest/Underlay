// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.snapshot

import android.view.View
import android.view.Window
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

internal class OriginInHost {

    private val viewOnScreen = IntArray(2)
    private val hostOnScreen = IntArray(2)

    fun of(view: View, host: Window, offsetInView: Offset = Offset.Zero): IntOffset {
        view.getLocationOnScreen(viewOnScreen)
        host.decorView.getLocationOnScreen(hostOnScreen)

        return IntOffset(
            x = viewOnScreen[0] - hostOnScreen[0] + offsetInView.x.roundToInt(),
            y = viewOnScreen[1] - hostOnScreen[1] + offsetInView.y.roundToInt(),
        )
    }
}
