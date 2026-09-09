// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.timkrest.underlay.UnderlayState
import com.timkrest.underlay.rememberUnderlayState

@Composable
internal fun OverlayHost(
    kind: OverlayKind?,
    blurRadius: Dp,
    underlayState: UnderlayState,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val card: @Composable (OverlayKind) -> Unit = { openKind ->
        UnderlayCard(
            kind = openKind,
            blurRadius = blurRadius,
            underlayState = underlayState,
            onDismiss = onDismiss,
            onRefresh = onRefresh,
        )
    }

    when (kind) {
        null, OverlayKind.TranslucentActivity -> Unit

        OverlayKind.DialogWindow -> Dialog(onDismissRequest = onDismiss) {
            RemoveDialogDim()
            card(kind)
        }

        OverlayKind.PopupWindow -> Popup(
            alignment = Alignment.Center,
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true),
        ) {
            card(kind)
        }

        OverlayKind.SameWindow -> {
            BackHandler(onBack = onDismiss)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                card(kind)
            }
        }
    }
}

@Composable
private fun RemoveDialogDim() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window

    DisposableEffect(window) {
        window?.setDimAmount(0f)
        onDispose { }
    }
}

@Preview(heightDp = 480)
@Composable
private fun OverlayHostPreview() {
    SampleTheme {
        OverlayHost(
            kind = OverlayKind.SameWindow,
            blurRadius = DEFAULT_BLUR_RADIUS,
            underlayState = rememberUnderlayState(),
            onRefresh = { },
            onDismiss = { },
        )
    }
}
