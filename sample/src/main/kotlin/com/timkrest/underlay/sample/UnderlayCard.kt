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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.UnderlayState
import com.timkrest.underlay.blurredUnderlay
import com.timkrest.underlay.rememberUnderlayState
import kotlin.math.roundToInt

internal val CARD_WIDTH = 320.dp
internal val CARD_TINT = Color.Black.copy(alpha = 0.28f)
internal val CARD_CORNER = 28.dp
internal val CARD_PADDING = 24.dp
internal val CARD_SPACING = 14.dp
internal val CARD_SHAPE = RoundedCornerShape(CARD_CORNER)
internal val CARD_SECONDARY_TEXT = Color.White.copy(alpha = 0.76f)
private val CARD_BORDER = Color.White.copy(alpha = 0.16f)
private const val DISABLED_ALPHA = 0.38f

@Composable
private fun cardButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = Color.White,
    disabledContentColor = Color.White.copy(alpha = DISABLED_ALPHA),
)

@Composable
internal fun UnderlayCard(
    kind: OverlayKind,
    blurRadius: Dp,
    underlayState: UnderlayState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onBlurRadiusChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CARD_SHAPE,
    liveSnapshot: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .blurredUnderlay(
                blurRadius = blurRadius,
                tint = CARD_TINT,
                fallback = MaterialTheme.colorScheme.surface,
                liveSnapshot = liveSnapshot,
                state = underlayState,
            )
            .border(1.dp, CARD_BORDER, shape)
            .padding(CARD_PADDING),
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
    ) {
        Text(
            text = kind.label,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
        )
        UnderlaySourceBadge(underlayState.source)
        Text(
            text = kind.explanation,
            color = CARD_SECONDARY_TEXT,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "blurRadius = ${blurRadius.value.roundToInt()}.dp",
            color = CARD_SECONDARY_TEXT,
            style = MaterialTheme.typography.bodySmall,
        )
        BlurRadiusSlider(blurRadius = blurRadius, onBlurRadiusChange = onBlurRadiusChange)
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onRefresh, enabled = kind.hasHostWindow, colors = cardButtonColors()) {
                Text(text = "Shuffle backdrop")
            }
            TextButton(onClick = onDismiss, colors = cardButtonColors()) {
                Text(text = "Close")
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
            modifier = Modifier.width(CARD_WIDTH),
        )
    }
}
