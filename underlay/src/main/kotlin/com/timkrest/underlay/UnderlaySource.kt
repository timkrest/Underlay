// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

/**
 * Where the underlay currently comes from.
 *
 * Reported through the `onSourceChange` callback of
 * [blurredUnderlay][androidx.compose.ui.Modifier.blurredUnderlay] and read as [UnderlayState.source].
 */
public enum class UnderlaySource {

    /**
     * The system blurs whatever is behind the overlay window, via `FLAG_BLUR_BEHIND`. Needs API
     * 31+, cross-window blur enabled by the system, and an overlay that owns a separate window.
     * Only the tint is drawn on top.
     */
    SystemBlur,

    /**
     * A snapshot of the host window, blurred and drawn under the tint. Needs a host window that is
     * not the overlay's own. A re-capture that fails keeps the snapshot already on screen, unless
     * the host resized and it no longer fits - that one drops through [Pending] instead.
     */
    Snapshot,

    /**
     * A snapshot is being captured and blurred. Only the tint is drawn, so the host window shows
     * through clear until [Snapshot] takes over instead of the overlay flashing [Fallback]. Always
     * followed by [Snapshot], or by [Fallback] if the capture keeps failing.
     */
    Pending,

    /**
     * The solid fallback color is drawn. Reported when the caller is not in a window of its own -
     * blurring the host would then mean blurring the overlay into its own backdrop - when there is
     * no host window to capture, and when capturing it failed.
     */
    Fallback,
}
