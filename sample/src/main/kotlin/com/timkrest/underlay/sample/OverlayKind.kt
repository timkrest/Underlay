// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

internal enum class OverlayKind(val label: String, val explanation: String) {

    DialogWindow(
        label = "Dialog",
        explanation = "Own window. Host window available.",
    ),

    PopupWindow(
        label = "Popup",
        explanation = "Own window via WindowManager. Host window available.",
    ),

    TranslucentActivity(
        label = "Translucent activity",
        explanation = "Own window. No host window to capture.",
    ),

    SameWindow(
        label = "Same window",
        explanation = "No own window. Fallback only.",
    ),
}
