// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.UnderlayState
import com.timkrest.underlay.blurredUnderlay
import com.timkrest.underlay.rememberUnderlayState
import kotlin.math.roundToInt

private val CARD_SHAPE = RoundedCornerShape(28.dp)
private val CARD_TINT = Color.Black.copy(alpha = 0.28f)
private val CARD_WIDTH = 320.dp
private const val DISABLED_ALPHA = 0.38f

@Composable
internal fun UnderlayCard(
    kind: OverlayKind,
    blurRadius: Dp,
    underlayState: UnderlayState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onBlurRadiusChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(CARD_WIDTH)
            .clip(CARD_SHAPE)
            .blurredUnderlay(
                blurRadius = blurRadius,
                tint = CARD_TINT,
                fallback = MaterialTheme.colorScheme.surface,
                state = underlayState,
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), CARD_SHAPE)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = kind.label,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
        )
        UnderlaySourceBadge(underlayState.source)
        Text(
            text = kind.explanation,
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "blurRadius = ${blurRadius.value.roundToInt()}.dp",
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.bodySmall,
        )
        BlurRadiusSlider(blurRadius = blurRadius, onBlurRadiusChange = onBlurRadiusChange)
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onRefresh, enabled = kind.hasHostWindow) {
                Text(
                    text = "Shuffle backdrop",
                    color = Color.White.copy(alpha = if (kind.hasHostWindow) 1f else DISABLED_ALPHA),
                )
            }
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = Color.White)
            }
        }
    }
}

@Preview
@Composable
private fun UnderlayCardPreview() {
    SampleTheme {
        UnderlayCard(
            kind = OverlayKind.DialogWindow,
            blurRadius = DEFAULT_BLUR_RADIUS,
            underlayState = rememberUnderlayState(),
            onDismiss = { },
            onRefresh = { },
            onBlurRadiusChange = { },
        )
    }
}
