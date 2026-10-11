// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.app.Dialog
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.ViewUnderlay

@Composable
internal fun ViewDialogOverlay(
    blurRadius: Dp,
    liveSnapshot: Boolean,
    source: UnderlaySource?,
    onSourceChange: (UnderlaySource?) -> Unit,
    onBlurRadiusChange: (Dp) -> Unit,
    onShuffle: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.surface.toArgb()
    val currentOnSourceChange by rememberUpdatedState(onSourceChange)
    val currentOnBlurRadiusChange by rememberUpdatedState(onBlurRadiusChange)
    val currentOnShuffle by rememberUpdatedState(onShuffle)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val card = remember(context) {
        ViewDialogCard(
            context = context,
            blurRadius = blurRadius,
            fallback = fallback,
            onBlurRadiusChange = { currentOnBlurRadiusChange(it) },
            onShuffle = { currentOnShuffle() },
            onClose = { currentOnDismiss() },
        )
    }
    val dialog = remember(card) {
        Dialog(context, R.style.Theme_Sample_ViewDialog).apply {
            setContentView(card.root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
            setOnDismissListener { currentOnDismiss() }
        }
    }

    SideEffect {
        card.underlay.blurRadius = context.toPx(blurRadius).toFloat()
        card.underlay.liveSnapshot = liveSnapshot
        card.underlay.onSourceChange = ViewUnderlay.OnSourceChangeListener { source -> currentOnSourceChange(source) }
        card.showSource(source)
    }

    DisposableEffect(dialog) {
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            dialog.dismiss()
        }
    }
}
