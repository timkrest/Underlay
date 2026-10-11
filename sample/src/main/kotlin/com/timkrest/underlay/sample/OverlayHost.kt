// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.rememberUnderlayState
import kotlinx.coroutines.launch

private val SAME_WINDOW_SCRIM = Color.Black.copy(alpha = 0.35f)

/** Hosts the overlay kinds that live inside this activity; [OverlayKind.TranslucentActivity] is its own. */
@Composable
internal fun OverlayHost(
    kind: OverlayKind?,
    source: UnderlaySource?,
    blurRadius: Dp,
    liveSnapshot: Boolean,
    onSourceChange: (UnderlaySource?) -> Unit,
    onShuffle: () -> Unit,
    onBlurRadiusChange: (Dp) -> Unit,
    onDismiss: () -> Unit,
) {
    if (kind == null || kind == OverlayKind.TranslucentActivity) return

    key(kind) {
        HostedOverlay(
            kind = kind,
            source = source,
            blurRadius = blurRadius,
            liveSnapshot = liveSnapshot,
            onSourceChange = onSourceChange,
            onShuffle = onShuffle,
            onBlurRadiusChange = onBlurRadiusChange,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun HostedOverlay(
    kind: OverlayKind,
    source: UnderlaySource?,
    blurRadius: Dp,
    liveSnapshot: Boolean,
    onSourceChange: (UnderlaySource?) -> Unit,
    onShuffle: () -> Unit,
    onBlurRadiusChange: (Dp) -> Unit,
    onDismiss: () -> Unit,
) {
    val underlayState = rememberUnderlayState()
    val currentOnSourceChange by rememberUpdatedState(onSourceChange)
    val underlay = Modifier.sampleUnderlay(
        blurRadius = blurRadius,
        liveSnapshot = liveSnapshot,
        state = underlayState,
        onSourceChange = { currentOnSourceChange(it) },
    )

    DisposableEffect(Unit) {
        onDispose { currentOnSourceChange(null) }
    }

    @Composable
    fun Card(
        modifier: Modifier,
        shape: Shape = CARD_SHAPE,
        onClose: () -> Unit = onDismiss,
    ) = OverlayCard(
        kind = kind,
        source = source,
        blurRadius = blurRadius,
        onBlurRadiusChange = onBlurRadiusChange,
        onShuffle = {
            onShuffle()
            underlayState.refresh()
        },
        onClose = onClose,
        modifier = modifier,
        shape = shape,
    )

    when (kind) {
        OverlayKind.DialogWindow -> FullWindowDialog(onDismiss = onDismiss, modifier = underlay) {
            Card(modifier = Modifier.width(cardWidth()).background(CARD_FILL))
        }

        OverlayKind.PopupWindow -> PassThroughPopup(onDismiss = onDismiss) {
            Card(modifier = Modifier.width(cardWidth()).then(underlay))
        }

        OverlayKind.BottomSheet -> TransparentBottomSheet(onDismiss = onDismiss) { shape, hide ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(underlay)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
                shape = shape,
                onClose = hide,
            )
        }

        OverlayKind.ViewDialog -> ViewDialogOverlay(
            blurRadius = blurRadius,
            liveSnapshot = liveSnapshot,
            source = source,
            onSourceChange = onSourceChange,
            onBlurRadiusChange = onBlurRadiusChange,
            onShuffle = onShuffle,
            onDismiss = onDismiss,
        )

        OverlayKind.SameWindow -> ModalScrim(onDismiss = onDismiss) {
            Card(modifier = Modifier.width(cardWidth()).then(underlay))
        }

        OverlayKind.TranslucentActivity -> Unit
    }
}

/** A dialog whose window spans the screen, so the underlay on it covers everything behind. */
@Composable
private fun FullWindowDialog(onDismiss: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        RemoveDialogDim()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier)
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.clickable(interactionSource = null, indication = null, onClick = {})) {
                content()
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
private fun TransparentBottomSheet(onDismiss: () -> Unit, content: @Composable (shape: Shape, hide: () -> Unit) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val shape = BottomSheetDefaults.ExpandedShape
    val hide: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = shape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        scrimColor = Color.Transparent,
        dragHandle = null,
    ) {
        content(shape, hide)
    }
}

@Composable
private fun ModalScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SAME_WINDOW_SCRIM)
            .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.clickable(interactionSource = null, indication = null, onClick = {})) {
            content()
        }
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
            source = UnderlaySource.Fallback,
            blurRadius = DEFAULT_BLUR_RADIUS,
            liveSnapshot = false,
            onSourceChange = { },
            onShuffle = { },
            onBlurRadiusChange = { },
            onDismiss = { },
        )
    }
}
