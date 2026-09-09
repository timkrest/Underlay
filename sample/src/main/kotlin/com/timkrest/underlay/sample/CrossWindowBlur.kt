// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import java.util.function.Consumer

@Composable
internal fun isCrossWindowBlurEnabled(): Boolean? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val windowManager = LocalContext.current.getSystemService<WindowManager>() ?: return null
    var isEnabled by remember(windowManager) { mutableStateOf(windowManager.isCrossWindowBlurEnabled) }

    DisposableEffect(windowManager) {
        val listener = Consumer<Boolean> { enabled -> isEnabled = enabled }
        windowManager.addCrossWindowBlurEnabledListener(listener)
        onDispose { windowManager.removeCrossWindowBlurEnabledListener(listener) }
    }

    return isEnabled
}
