// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Handle on the underlay of one overlay: where it currently comes from, and a way to take a fresh
 * snapshot for content that moves underneath while the overlay stays open.
 *
 * Pass it to [blurredUnderlay][androidx.compose.ui.Modifier.blurredUnderlay]. Call [refresh]
 * whenever the host window has changed; only [UnderlaySource.Snapshot] reads it, the system blur is
 * live and needs no refreshing.
 */
@Stable
public class UnderlayState {

    internal var refreshes: Int by mutableIntStateOf(0)
        private set

    /**
     * Where the underlay comes from, as Compose state. Null while no overlay carries this state:
     * before it is shown and after it is dismissed.
     */
    public var source: UnderlaySource? by mutableStateOf(null)
        internal set

    /**
     * Captures the host window again, from the main thread. The snapshot on screen stays until the
     * new one is ready.
     */
    public fun refresh() {
        refreshes++
    }
}

/** Remembers an [UnderlayState] for as long as the calling composable stays in composition. */
@Composable
public fun rememberUnderlayState(): UnderlayState = remember { UnderlayState() }
