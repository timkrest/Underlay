// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

internal enum class OverlayKind(val label: String, val explanation: String, val hasHostWindow: Boolean) {

    DialogWindow(
        label = "Dialog",
        explanation = "Own window. Host window available.",
        hasHostWindow = true,
    ),

    PopupWindow(
        label = "Popup",
        explanation = "Own window via WindowManager. Host window available. Touches outside pass through: scroll the grid under it.",
        hasHostWindow = true,
    ),

    BottomSheet(
        label = "Bottom sheet",
        explanation = "Material 3 ModalBottomSheet. Own window, host window available. Drag it: the snapshot stays in place.",
        hasHostWindow = true,
    ),

    ViewDialog(
        label = "View dialog",
        explanation = "android.app.Dialog built from Views, backed by View.blurredUnderlay. Host window available.",
        hasHostWindow = true,
    ),

    TranslucentActivity(
        label = "Translucent",
        explanation = "Own window. No host window to capture.",
        hasHostWindow = false,
    ),

    SameWindow(
        label = "Same window",
        explanation = "No own window. Fallback only.",
        hasHostWindow = false,
    ),
}
