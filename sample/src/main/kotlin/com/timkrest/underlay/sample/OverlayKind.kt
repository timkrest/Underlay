// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface OverlayKind {
    val label: String
    val hint: String
    val windows: String
    val explanation: String
    val hasHostWindow: Boolean

    data object TranslucentActivity : OverlayKind {
        override val label = "Translucent"
        override val hint = "no host"
        override val windows = "Own window · no host window"
        override val explanation = "A translucent activity has no host window to capture: the system blur frosts " +
            "the screen behind it, otherwise the card draws the flat fallback."
        override val hasHostWindow = false
    }

    companion object {
        val entries: List<OverlayKind> = listOf(
            HostedKind.DialogWindow,
            HostedKind.PopupWindow,
            HostedKind.BottomSheet,
            HostedKind.ViewDialog,
            TranslucentActivity,
            HostedKind.SameWindow,
        )
    }
}

internal enum class HostedKind(
    override val label: String,
    override val hint: String,
    override val windows: String,
    override val explanation: String,
    override val hasHostWindow: Boolean,
) : OverlayKind {

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

    SameWindow(
        label = "Inline",
        hint = "same window",
        windows = "No own window",
        explanation = "An overlay inside the activity window would blur itself into its own backdrop, so only " +
            "the fallback is drawn.",
        hasHostWindow = false,
    ),
}
