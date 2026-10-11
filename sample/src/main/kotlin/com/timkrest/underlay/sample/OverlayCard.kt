// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.UnderlayState
import com.timkrest.underlay.blurredUnderlay
import kotlin.math.roundToInt

internal val UNDERLAY_TINT = Color.Black.copy(alpha = 0.32f)
internal val CARD_FILL = Color.White.copy(alpha = 0.08f)
internal val CARD_BORDER = Color.White.copy(alpha = 0.18f)
internal val CARD_SECONDARY_TEXT = Color.White.copy(alpha = 0.76f)
internal val CARD_CORNER = 28.dp
internal val CARD_PADDING = 24.dp
internal val CARD_SPACING = 12.dp
internal val CARD_SHAPE = RoundedCornerShape(CARD_CORNER)
internal val CARD_MAX_WIDTH = 340.dp
internal val CARD_MARGIN = 20.dp
private const val DISABLED_ALPHA = 0.38f

@Composable
internal fun Modifier.sampleUnderlay(
    blurRadius: Dp,
    liveSnapshot: Boolean,
    state: UnderlayState? = null,
    onSourceChange: ((UnderlaySource) -> Unit)? = null,
): Modifier = blurredUnderlay(
    blurRadius = blurRadius,
    tint = UNDERLAY_TINT,
    fallback = MaterialTheme.colorScheme.surface,
    liveSnapshot = liveSnapshot,
    state = state,
    onSourceChange = onSourceChange,
)

@Composable
internal fun cardWidth(): Dp {
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return minOf(CARD_MAX_WIDTH, windowWidth - CARD_MARGIN * 2)
}

/** [modifier] brings the backdrop: the underlay, or [CARD_FILL] over an underlay that fills the window. */
@Composable
internal fun OverlayCard(
    kind: OverlayKind,
    source: UnderlaySource?,
    blurRadius: Dp,
    onBlurRadiusChange: (Dp) -> Unit,
    onShuffle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CARD_SHAPE,
) {
    Column(
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .border(1.dp, CARD_BORDER, shape)
            .padding(CARD_PADDING),
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
    ) {
        CardHeader(kind = kind, source = source)
        BlurControl(blurRadius = blurRadius, onBlurRadiusChange = onBlurRadiusChange)
        CardActions(
            shuffleEnabled = kind.hasHostWindow,
            onShuffle = onShuffle,
            onClose = onClose,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun CardHeader(kind: OverlayKind, source: UnderlaySource?) {
    Text(
        text = kind.windows.uppercase(),
        color = CARD_SECONDARY_TEXT,
        style = MaterialTheme.typography.labelSmall,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = kind.label,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        UnderlaySourceBadge(source)
    }
    Text(
        text = kind.explanation,
        color = CARD_SECONDARY_TEXT,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun BlurControl(blurRadius: Dp, onBlurRadiusChange: (Dp) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BlurRadiusSlider(
            blurRadius = blurRadius,
            onBlurRadiusChange = onBlurRadiusChange,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${blurRadius.value.roundToInt()} dp",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CardActions(shuffleEnabled: Boolean, onShuffle: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ButtonDefaults.textButtonColors(
        contentColor = Color.White,
        disabledContentColor = Color.White.copy(alpha = DISABLED_ALPHA),
    )

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onShuffle, enabled = shuffleEnabled, colors = colors) {
            Text(text = "Shuffle feed")
        }
        TextButton(onClick = onClose, colors = colors) {
            Text(text = "Close")
        }
    }
}

@Preview
@Composable
private fun OverlayCardPreview() {
    SampleTheme {
        OverlayCard(
            kind = HostedKind.DialogWindow,
            source = UnderlaySource.Snapshot,
            blurRadius = DEFAULT_BLUR_RADIUS,
            onBlurRadiusChange = { },
            onShuffle = { },
            onClose = { },
            modifier = Modifier
                .width(CARD_MAX_WIDTH)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}
