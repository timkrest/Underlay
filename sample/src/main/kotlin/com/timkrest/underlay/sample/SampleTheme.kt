// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SampleColorScheme = darkColorScheme(
    primary = Color(0xFF9FD0FF),
    onPrimary = Color(0xFF00325A),
    surface = Color(0xFF101318),
    onSurface = Color(0xFFE2E6EC),
    surfaceVariant = Color(0xFF1A1F27),
    onSurfaceVariant = Color(0xFFAFB7C2),
    background = Color(0xFF0B0E12),
    onBackground = Color(0xFFE2E6EC),
    outline = Color(0xFF3A424E),
)

@Composable
internal fun SampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SampleColorScheme, content = content)
}
