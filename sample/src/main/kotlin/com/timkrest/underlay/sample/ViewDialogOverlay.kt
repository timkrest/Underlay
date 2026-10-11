// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.app.Dialog
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.ViewUnderlay
import com.timkrest.underlay.blurredUnderlay

@Composable
internal fun ViewDialogOverlay(
    blurRadius: Dp,
    liveSnapshot: Boolean,
    onSourceChange: (UnderlaySource?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val blurRadiusPx = with(LocalDensity.current) { blurRadius.toPx() }
    val fallback = MaterialTheme.colorScheme.surface.toArgb()
    val currentOnSourceChange by rememberUpdatedState(onSourceChange)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var underlay by remember { mutableStateOf<ViewUnderlay?>(null) }

    DisposableEffect(context) {
        val card = viewDialogCard(context, onClose = { currentOnDismiss() })
        underlay = card.blurredUnderlay(
            blurRadius = blurRadiusPx,
            tint = CARD_TINT.toArgb(),
            fallback = fallback,
            liveSnapshot = liveSnapshot,
            onSourceChange = { source -> currentOnSourceChange(source) },
        )
        val dialog = Dialog(context).apply {
            setContentView(card, ViewGroup.LayoutParams(context.dp(CARD_WIDTH.value), ViewGroup.LayoutParams.WRAP_CONTENT))
            window?.setBackgroundDrawable(Color.Transparent.toArgb().toDrawable())
            window?.setDimAmount(0f)
            setOnDismissListener { currentOnDismiss() }
            show()
        }

        onDispose {
            dialog.setOnDismissListener(null)
            dialog.dismiss()
            underlay = null
            currentOnSourceChange(null)
        }
    }

    SideEffect {
        underlay?.blurRadius = blurRadiusPx
        underlay?.liveSnapshot = liveSnapshot
    }
}

@Preview
@Composable
private fun ViewDialogCardPreview() {
    SampleTheme {
        AndroidView(factory = { context -> viewDialogCard(context, onClose = {}) })
    }
}
