// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.app.Dialog
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.ViewUnderlay
import com.timkrest.underlay.blurredUnderlay

@Composable
internal fun ViewDialogOverlay(
    blurRadius: Dp,
    liveSnapshot: Boolean,
    onSourceChange: (UnderlaySource?) -> Unit,
    onBlurRadiusChange: (Dp) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.surface.toArgb()
    val currentBlurRadius by rememberUpdatedState(blurRadius)
    val currentLiveSnapshot by rememberUpdatedState(liveSnapshot)
    val currentOnSourceChange by rememberUpdatedState(onSourceChange)
    val currentOnBlurRadiusChange by rememberUpdatedState(onBlurRadiusChange)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    DisposableEffect(context) {
        lateinit var underlay: ViewUnderlay
        val card = viewDialogCard(
            context = context,
            blurRadius = currentBlurRadius,
            onBlurRadiusChange = { radius ->
                underlay.blurRadius = context.toPx(radius).toFloat()
                currentOnBlurRadiusChange(radius)
            },
            onClose = { currentOnDismiss() },
        )
        underlay = card.blurredUnderlay(
            blurRadius = context.toPx(currentBlurRadius).toFloat(),
            tint = CARD_TINT.toArgb(),
            fallback = fallback,
            liveSnapshot = currentLiveSnapshot,
            onSourceChange = { source -> currentOnSourceChange(source) },
        )
        val dialog = Dialog(context, R.style.Theme_Sample_ViewDialog).apply {
            setContentView(card, ViewGroup.LayoutParams(context.toPx(CARD_WIDTH), ViewGroup.LayoutParams.WRAP_CONTENT))
            setOnDismissListener { currentOnDismiss() }
            show()
        }

        onDispose {
            dialog.setOnDismissListener(null)
            dialog.dismiss()
            currentOnSourceChange(null)
        }
    }
}
