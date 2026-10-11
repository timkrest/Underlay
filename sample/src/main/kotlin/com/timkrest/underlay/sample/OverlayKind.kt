// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

internal enum class OverlayKind(
    val label: String,
    val hint: String,
    val windows: String,
    val explanation: String,
    val hasHostWindow: Boolean,
) {

    DialogWindow(
        label = "Dialog",
        hint = "own window",
        windows = "Own window · host window",
        explanation = "Compose Dialog sized to the screen, the underlay fills it: the system blur and the snapshot " +
            "frost the same picture.",
        hasHostWindow = true,
    ),

    PopupWindow(
        label = "Popup",
        hint = "pass-through",
        windows = "Own window · host window",
        explanation = "Popup window sized to this card: the system blur still frosts the whole screen behind " +
            "it, the snapshot only the card. Touches outside pass through: scroll the feed, switch Live on.",
        hasHostWindow = true,
    ),

    BottomSheet(
        label = "Sheet",
        hint = "Material 3",
        windows = "Own window · host window",
        explanation = "Material 3 ModalBottomSheet. Its window spans the screen, so the system blurs all of it " +
            "and the snapshot only the sheet. Drag it: the snapshot stays put.",
        hasHostWindow = true,
    ),

    ViewDialog(
        label = "Views",
        hint = "View dialog",
        windows = "Own window · host window",
        explanation = "android.app.Dialog built from Views, the underlay on its root through View.blurredUnderlay.",
        hasHostWindow = true,
    ),

    TranslucentActivity(
        label = "Translucent",
        hint = "no host",
        windows = "Own window · no host window",
        explanation = "A translucent activity has no host window to capture: the system blur frosts the screen " +
            "behind it, otherwise the card draws the flat fallback.",
        hasHostWindow = false,
    ),

    SameWindow(
        label = "Inline",
        hint = "same window",
        windows = "No own window",
        explanation = "An overlay inside the activity window would blur itself into its own backdrop, so only " +
            "the fallback is drawn.",
        hasHostWindow = false,
    ),
}
