// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

/**
 * Where the underlay comes from. New sources may be added, so give a `when` an `else` branch.
 */
public enum class UnderlaySource {

    /** `FLAG_BLUR_BEHIND` on the overlay window. Needs API 31+ and cross-window blur turned on. */
    SystemBlur,

    /** A blurred snapshot of the host window. A failed re-capture keeps the old one if it still fits. */
    Snapshot,

    /** A snapshot is on its way. Only the tint is drawn meanwhile. */
    Pending,

    /** The fallback color: no window of its own, no host window, or the capture failed. */
    Fallback,
}
