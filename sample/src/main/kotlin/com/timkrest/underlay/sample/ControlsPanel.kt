// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
private val OVERLAY_ROWS = OverlayKind.entries.chunked(BUTTONS_PER_ROW)

@Composable
internal fun ControlsPanel(
    blurRadius: Dp,
    overlaySource: UnderlaySource?,
    areTilesHardware: Boolean?,
    crossWindowBlur: Boolean?,
    liveSnapshot: Boolean,
    onBlurRadiusChange: (Dp) -> Unit,
    onLiveSnapshotChange: (Boolean) -> Unit,
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
            PanelHeader(blurRadius = blurRadius, overlaySource = overlaySource)
            BlurRadiusSlider(blurRadius = blurRadius, onBlurRadiusChange = onBlurRadiusChange)
            LiveSnapshotRow(liveSnapshot = liveSnapshot, onLiveSnapshotChange = onLiveSnapshotChange)
            tilesHint(areTilesHardware)?.let { hint -> Hint(text = hint) }
            Hint(text = crossWindowBlurHint(crossWindowBlur))
            OverlayKindButtons(onOpen = onOpen)
        }
    }
}

@Composable
private fun PanelHeader(blurRadius: Dp, overlaySource: UnderlaySource?) {
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
private fun LiveSnapshotRow(liveSnapshot: Boolean, onLiveSnapshotChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Live snapshot", style = MaterialTheme.typography.titleMedium)
            Hint(text = "Capture the host again every time it draws. Open a Popup and scroll the grid under it.")
        }
        Switch(checked = liveSnapshot, onCheckedChange = onLiveSnapshotChange)
    }
}

@Composable
private fun OverlayKindButtons(onOpen: (OverlayKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OVERLAY_ROWS.forEach { row ->
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

private fun tilesHint(areTilesHardware: Boolean?): String? = when (areTilesHardware) {
    null -> null
    true -> "Tiles: hardware bitmaps"
    false -> "Tiles: software bitmaps"
}

private fun crossWindowBlurHint(isEnabled: Boolean?): String = when (isEnabled) {
    null -> "Cross-window blur needs API 31. SystemBlur is unavailable."
    true -> "Cross-window blur is on. Turn on Battery Saver to see an open overlay switch to Snapshot."
    false -> "Cross-window blur is off on this device. SystemBlur is unavailable."
}

@Preview
@Composable
private fun ControlsPanelPreview() {
    SampleTheme {
        ControlsPanel(
            blurRadius = DEFAULT_BLUR_RADIUS,
            overlaySource = UnderlaySource.Snapshot,
            areTilesHardware = true,
            crossWindowBlur = true,
            liveSnapshot = false,
            onBlurRadiusChange = { },
            onLiveSnapshotChange = { },
            onOpen = { },
        )
    }
}
