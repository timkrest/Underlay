// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val DEFAULT_BLUR_RADIUS: Dp = 24.dp
internal val MAX_BLUR_RADIUS: Dp = 48.dp

@Composable
internal fun BlurRadiusSlider(
    blurRadius: Dp,
    onBlurRadiusChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Slider(
        value = blurRadius.value,
        onValueChange = { value -> onBlurRadiusChange(value.dp) },
        modifier = modifier,
        valueRange = 0f..MAX_BLUR_RADIUS.value,
    )
}
