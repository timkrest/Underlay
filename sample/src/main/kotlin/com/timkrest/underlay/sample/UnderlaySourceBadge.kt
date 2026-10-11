// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.UnderlaySource

internal const val BADGE_FILL_ALPHA = 0.18f
internal const val BADGE_BORDER_ALPHA = 0.35f
private val IDLE_COLOR = Color(0xFF9AA3AF)

@Composable
internal fun UnderlaySourceBadge(
    source: UnderlaySource?,
    modifier: Modifier = Modifier,
    placeholder: String = "Resolving",
) {
    val color = source.indicatorColor()

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = BADGE_FILL_ALPHA))
            .border(1.dp, color.copy(alpha = BADGE_BORDER_ALPHA), CircleShape)
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = source?.description() ?: placeholder,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

internal fun UnderlaySource.description(): String = when (this) {
    UnderlaySource.SystemBlur -> "System blur"
    UnderlaySource.Snapshot -> "Snapshot"
    UnderlaySource.Pending -> "Capturing"
    UnderlaySource.Fallback -> "Fallback"
}

internal fun UnderlaySource?.indicatorColor(): Color = when (this) {
    UnderlaySource.SystemBlur -> Color(0xFF7BE495)
    UnderlaySource.Snapshot -> Color(0xFFFFD166)
    UnderlaySource.Fallback -> Color(0xFFFF8A80)
    UnderlaySource.Pending, null -> IDLE_COLOR
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
