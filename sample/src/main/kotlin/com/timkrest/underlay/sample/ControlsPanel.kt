package com.timkrest.underlay.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.UnderlaySource
import kotlin.math.roundToInt

private const val BUTTONS_PER_ROW = 2
private const val BATTERY_SAVER_HINT =
    "Battery Saver switches cross-window blur off. Turn it on to watch this fall through to Snapshot."

@Composable
internal fun ControlsPanel(
    blurRadius: Dp,
    overlaySource: UnderlaySource?,
    areTilesHardware: Boolean,
    onBlurRadiusChange: (Dp) -> Unit,
    onOpen: (OverlayKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 16.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BlurRadiusRow(blurRadius = blurRadius, overlaySource = overlaySource)
            Slider(
                value = blurRadius.value,
                onValueChange = { onBlurRadiusChange(it.roundToInt().dp) },
                valueRange = 0f..MAX_BLUR_RADIUS.value,
            )
            Hint(text = tilesHint(areTilesHardware))
            if (overlaySource == UnderlaySource.SystemBlur) {
                Hint(text = BATTERY_SAVER_HINT)
            }
            OverlayKindButtons(onOpen = onOpen)
        }
    }
}

@Composable
private fun BlurRadiusRow(blurRadius: Dp, overlaySource: UnderlaySource?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Blur radius ${blurRadius.value.roundToInt()} dp",
            style = MaterialTheme.typography.titleMedium,
        )
        UnderlaySourceBadge(source = overlaySource, placeholder = "No overlay")
    }
}

@Composable
private fun OverlayKindButtons(onOpen: (OverlayKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OverlayKind.entries.chunked(BUTTONS_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { kind ->
                    FilledTonalButton(
                        onClick = { onOpen(kind) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = kind.label, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun tilesHint(areTilesHardware: Boolean): String =
    if (areTilesHardware) "Tiles: hardware bitmaps" else "Tiles: software bitmaps"

@Preview
@Composable
private fun ControlsPanelPreview() {
    SampleTheme {
        ControlsPanel(
            blurRadius = DEFAULT_BLUR_RADIUS,
            overlaySource = UnderlaySource.Snapshot,
            areTilesHardware = true,
            onBlurRadiusChange = { },
            onOpen = { },
        )
    }
}
