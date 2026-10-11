// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.UnderlaySource
import kotlin.math.roundToInt

internal val DOCK_MARGIN = 12.dp
internal val RAIL_WIDTH = 300.dp

private val DOCK_SHAPE = RoundedCornerShape(28.dp)
private val TILE_SHAPE = RoundedCornerShape(14.dp)
private val DOCK_PADDING = 14.dp

private val THREE_COLUMNS_MIN_WIDTH = 260.dp
private val TILE_GAP = 8.dp
private const val BORDER_ALPHA = 0.1f
private const val TILE_FILL_ALPHA = 0.14f

internal enum class DockLayout { Bottom, Rail }

@Composable
internal fun ControlDock(
    layout: DockLayout,
    blurRadius: Dp,
    overlaySource: UnderlaySource?,
    openKind: OverlayKind?,
    liveSnapshot: Boolean,
    deviceInfo: String,
    onBlurRadiusChange: (Dp) -> Unit,
    onLiveSnapshotChange: (Boolean) -> Unit,
    onOpen: (OverlayKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = DOCK_SHAPE,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = BORDER_ALPHA)),
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .then(if (layout == DockLayout.Rail) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(DOCK_PADDING),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DockHeader(overlaySource = overlaySource, openKind = openKind, deviceInfo = deviceInfo)
            LauncherGrid(onOpen = onOpen)
            BlurRow(
                blurRadius = blurRadius,
                liveSnapshot = liveSnapshot,
                onBlurRadiusChange = onBlurRadiusChange,
                onLiveSnapshotChange = onLiveSnapshotChange,
            )
        }
    }
}

@Composable
private fun DockHeader(overlaySource: UnderlaySource?, openKind: OverlayKind?, deviceInfo: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = openKind?.label ?: "Overlays",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = deviceInfo,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        UnderlaySourceBadge(source = overlaySource, placeholder = if (openKind == null) "Idle" else "Opening")
    }
}

@Composable
private fun LauncherGrid(onOpen: (OverlayKind) -> Unit) {
    BoxWithConstraints {
        val columns = if (maxWidth / LocalDensity.current.fontScale >= THREE_COLUMNS_MIN_WIDTH) 3 else 2

        Column(verticalArrangement = Arrangement.spacedBy(TILE_GAP)) {
            OverlayKind.entries.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(TILE_GAP),
                ) {
                    row.forEach { kind -> LauncherTile(kind = kind, onOpen = onOpen, modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun LauncherTile(kind: OverlayKind, onOpen: (OverlayKind) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(TILE_SHAPE)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = TILE_FILL_ALPHA))
            .clickable { onOpen(kind) }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = kind.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = kind.hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun BlurRow(
    blurRadius: Dp,
    liveSnapshot: Boolean,
    onBlurRadiusChange: (Dp) -> Unit,
    onLiveSnapshotChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Blur ${blurRadius.value.roundToInt()} dp",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
        BlurRadiusSlider(
            blurRadius = blurRadius,
            onBlurRadiusChange = onBlurRadiusChange,
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = liveSnapshot,
            onClick = { onLiveSnapshotChange(!liveSnapshot) },
            label = { Text(text = "Live", maxLines = 1) },
            border = null,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TILE_FILL_ALPHA),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}

@Preview
@Composable
private fun ControlDockPreview() {
    SampleTheme {
        ControlDock(
            layout = DockLayout.Bottom,
            blurRadius = DEFAULT_BLUR_RADIUS,
            overlaySource = UnderlaySource.Snapshot,
            openKind = OverlayKind.DialogWindow,
            liveSnapshot = false,
            deviceInfo = "API 34 · cross-window blur on",
            onBlurRadiusChange = { },
            onLiveSnapshotChange = { },
            onOpen = { },
        )
    }
}

@Preview(widthDp = 300)
@Composable
private fun ControlRailPreview() {
    SampleTheme {
        ControlDock(
            layout = DockLayout.Rail,
            blurRadius = DEFAULT_BLUR_RADIUS,
            overlaySource = null,
            openKind = null,
            liveSnapshot = true,
            deviceInfo = "API 30 · cross-window blur off",
            onBlurRadiusChange = { },
            onLiveSnapshotChange = { },
            onOpen = { },
        )
    }
}
