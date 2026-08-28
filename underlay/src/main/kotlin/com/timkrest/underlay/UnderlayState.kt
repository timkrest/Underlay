package com.timkrest.underlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Handle on the snapshot behind one overlay, for content that moves underneath while the overlay
 * stays open.
 *
 * Pass it to [blurredUnderlay][androidx.compose.ui.Modifier.blurredUnderlay] and call [refresh]
 * whenever the host window has changed. Only [UnderlaySource.Snapshot] reads it: the system blur
 * is live and needs no refreshing.
 */
@Stable
public class UnderlayState {

    internal var refreshes: Int by mutableIntStateOf(0)
        private set

    /** Captures the host window again. The snapshot on screen stays until the new one is ready. */
    public fun refresh() {
        refreshes++
    }
}

@Composable
public fun rememberUnderlayState(): UnderlayState = remember { UnderlayState() }
