// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.UnderlaySource

/** Everything the screen shows, owned by [SampleScreen] and changed only through `copy`. */
@Immutable
internal data class SampleState(
    val blurRadius: Dp = DEFAULT_BLUR_RADIUS,
    val openKind: OverlayKind? = null,
    val liveSnapshot: Boolean = false,
    val backdropShift: Int = 0,
    /** Reported by the open overlay; not saved, the overlay reports it again after a restore. */
    val overlaySource: UnderlaySource? = null,
) {

    companion object {

        val Saver = listSaver<SampleState, Any>(
            save = { state ->
                listOf(state.blurRadius.value, state.openKind?.name.orEmpty(), state.liveSnapshot, state.backdropShift)
            },
            restore = { saved ->
                SampleState(
                    blurRadius = (saved[0] as Float).dp,
                    openKind = (saved[1] as String).takeIf { it.isNotEmpty() }?.let(OverlayKind::valueOf),
                    liveSnapshot = saved[2] as Boolean,
                    backdropShift = saved[3] as Int,
                )
            },
        )
    }
}
