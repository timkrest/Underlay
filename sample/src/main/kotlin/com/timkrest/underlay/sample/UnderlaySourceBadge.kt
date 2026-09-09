// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.UnderlaySource

@Composable
internal fun UnderlaySourceBadge(
    source: UnderlaySource?,
    modifier: Modifier = Modifier,
    placeholder: String = "Resolving",
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(source.indicatorColor()),
        )
        Text(
            text = source?.description() ?: placeholder,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun UnderlaySource.description(): String = when (this) {
    UnderlaySource.SystemBlur -> "System blur"
    UnderlaySource.Snapshot -> "Snapshot"
    UnderlaySource.Pending -> "Capturing"
    UnderlaySource.Fallback -> "Fallback"
}

private fun UnderlaySource?.indicatorColor(): Color = when (this) {
    UnderlaySource.SystemBlur -> Color(0xFF7BE495)
    UnderlaySource.Snapshot -> Color(0xFFFFD166)
    UnderlaySource.Pending -> Color(0xFF9AA3AF)
    UnderlaySource.Fallback -> Color(0xFFFF8A80)
    null -> Color(0xFF9AA3AF)
}

@Preview
@Composable
private fun UnderlaySourceBadgePreview() {
    SampleTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (source in UnderlaySource.entries) UnderlaySourceBadge(source)
            UnderlaySourceBadge(source = null, placeholder = "No overlay")
        }
    }
}
