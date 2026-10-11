// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.UnderlayState
import com.timkrest.underlay.rememberUnderlayState

private val SAME_WINDOW_SCRIM = Color.Black.copy(alpha = 0.35f)

@Composable
internal fun OverlayHost(
    kind: OverlayKind?,
    blurRadius: Dp,
    liveSnapshot: Boolean,
    underlayState: UnderlayState,
    onViewSourceChange: (UnderlaySource?) -> Unit,
    onRefresh: () -> Unit,
    onBlurRadiusChange: (Dp) -> Unit,
    onDismiss: () -> Unit,
) {
    val card: @Composable (OverlayKind, Modifier, Shape) -> Unit = { openKind, modifier, shape ->
        UnderlayCard(
            kind = openKind,
            blurRadius = blurRadius,
            underlayState = underlayState,
            onDismiss = onDismiss,
            onRefresh = onRefresh,
            onBlurRadiusChange = onBlurRadiusChange,
            modifier = modifier,
            shape = shape,
            liveSnapshot = liveSnapshot,
        )
    }
    val centeredCard: @Composable (OverlayKind) -> Unit = { openKind -> card(openKind, Modifier.width(CARD_WIDTH), CARD_SHAPE) }

    when (kind) {
        null, OverlayKind.TranslucentActivity -> Unit

        OverlayKind.DialogWindow -> Dialog(onDismissRequest = onDismiss) {
            RemoveDialogDim()
            centeredCard(kind)
        }

        OverlayKind.PopupWindow -> PassThroughPopup(onDismiss = onDismiss) { centeredCard(kind) }

        OverlayKind.BottomSheet -> TransparentBottomSheet(onDismiss = onDismiss) { shape ->
            card(kind, Modifier.fillMaxWidth(), shape)
        }

        OverlayKind.ViewDialog -> ViewDialogOverlay(
            blurRadius = blurRadius,
            liveSnapshot = liveSnapshot,
            onSourceChange = onViewSourceChange,
            onDismiss = onDismiss,
        )

        OverlayKind.SameWindow -> {
            BackHandler(onBack = onDismiss)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SAME_WINDOW_SCRIM),
                contentAlignment = Alignment.Center,
            ) {
                centeredCard(kind)
            }
        }
    }
}

@Composable
private fun PassThroughPopup(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onDismiss)

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false, dismissOnClickOutside = false),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransparentBottomSheet(onDismiss: () -> Unit, content: @Composable (Shape) -> Unit) {
    val shape = BottomSheetDefaults.ExpandedShape

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = shape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        scrimColor = Color.Transparent,
        dragHandle = null,
    ) {
        content(shape)
    }
}

@Composable
private fun RemoveDialogDim() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window

    SideEffect { window?.setDimAmount(0f) }
}

@Preview(heightDp = 480)
@Composable
private fun OverlayHostPreview() {
    SampleTheme {
        OverlayHost(
            kind = OverlayKind.SameWindow,
            blurRadius = DEFAULT_BLUR_RADIUS,
            liveSnapshot = false,
            underlayState = rememberUnderlayState(),
            onViewSourceChange = { },
            onRefresh = { },
            onBlurRadiusChange = { },
            onDismiss = { },
        )
    }
}
