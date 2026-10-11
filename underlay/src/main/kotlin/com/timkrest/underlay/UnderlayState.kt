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
 * The source of one overlay's underlay, and a way to retake its snapshot. Pass it to
 * [blurredUnderlay][androidx.compose.ui.Modifier.blurredUnderlay].
 */
@Stable
public class UnderlayState {

    internal var refreshes: Int by mutableIntStateOf(0)
        private set

    /** Null while no overlay uses this state. */
    public var source: UnderlaySource? by mutableStateOf(null)
        private set

    private var reporter: Any? = null

    /**
     * Retakes the snapshot after the host changed without resizing. The old one stays until the new
     * one is ready.
     */
    public fun refresh() {
        refreshes++
    }

    internal fun report(reporter: Any, source: UnderlaySource?) {
        this.reporter = reporter
        this.source = source
    }

    internal fun withdraw(reporter: Any) {
        if (this.reporter !== reporter) return

        this.reporter = null
        source = null
    }
}

/** Remembers an [UnderlayState] for as long as the calling composable stays in composition. */
@Composable
public fun rememberUnderlayState(): UnderlayState = remember { UnderlayState() }
