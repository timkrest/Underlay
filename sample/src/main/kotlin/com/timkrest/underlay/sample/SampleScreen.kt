// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val HERO_RATIO = 1.8f
private const val WIDE_HERO_RATIO = 3f

@Composable
internal fun SampleScreen(modifier: Modifier = Modifier) {
    var state by rememberSaveable(stateSaver = SampleState.Saver) { mutableStateOf(SampleState()) }
    val tiles = rememberGradientTiles()
    val crossWindowBlur = rememberCrossWindowBlurEnabled()
    val translucentLauncher = rememberLauncherForActivityResult(TranslucentOverlayActivity.Contract) { radius ->
        state = state.copy(openKind = null, blurRadius = radius ?: state.blurRadius)
    }
    val onBlurRadiusChange: (Dp) -> Unit = { radius -> state = state.copy(blurRadius = radius) }

    AdaptiveLayout(
        modifier = modifier,
        feed = { layout, contentPadding ->
            if (tiles != null) {
                Backdrop(
                    tiles = tiles,
                    contentPadding = contentPadding,
                    heroRatio = if (layout == DockLayout.Rail) WIDE_HERO_RATIO else HERO_RATIO,
                    shift = state.backdropShift,
                )
            }
        },
        dock = { layout ->
            ControlDock(
                layout = layout,
                blurRadius = state.blurRadius,
                overlaySource = state.overlaySource,
                openKind = state.openKind,
                liveSnapshot = state.liveSnapshot,
                deviceInfo = deviceInfo(crossWindowBlur),
                onBlurRadiusChange = onBlurRadiusChange,
                onLiveSnapshotChange = { live -> state = state.copy(liveSnapshot = live) },
                onOpen = { kind ->
                    state = state.copy(openKind = kind)
                    if (kind == OverlayKind.TranslucentActivity) translucentLauncher.launch(state.blurRadius)
                },
            )
        },
    )

    val openKind = state.openKind
    if (openKind is HostedKind) {
        OverlayHost(
            kind = openKind,
            source = state.overlaySource,
            blurRadius = state.blurRadius,
            liveSnapshot = state.liveSnapshot,
            onSourceChange = { source -> state = state.copy(overlaySource = source) },
            onShuffle = { state = state.copy(backdropShift = state.backdropShift + 1) },
            onBlurRadiusChange = onBlurRadiusChange,
            onDismiss = { state = state.copy(openKind = null) },
        )
    }
}

@Composable
private fun AdaptiveLayout(
    feed: @Composable (layout: DockLayout, contentPadding: PaddingValues) -> Unit,
    dock: @Composable (layout: DockLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSize = LocalWindowInfo.current.containerSize
    val layout = if (windowSize.width > windowSize.height) DockLayout.Rail else DockLayout.Bottom
    val density = LocalDensity.current
    var dockHeight by remember { mutableStateOf(0.dp) }
    val dockReserve = when (layout) {
        DockLayout.Bottom -> PaddingValues(bottom = dockHeight + DOCK_MARGIN * 2)
        DockLayout.Rail -> PaddingValues(end = RAIL_WIDTH + DOCK_MARGIN * 2)
    }

    Box(modifier = modifier.fillMaxSize()) {
        feed(layout, WindowInsets.safeDrawing.asPaddingValues() + dockReserve + PaddingValues(FEED_GUTTER))
        Box(
            modifier = Modifier
                .align(if (layout == DockLayout.Rail) Alignment.CenterEnd else Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(DOCK_MARGIN)
                .then(if (layout == DockLayout.Rail) Modifier.width(RAIL_WIDTH) else Modifier)
                .onSizeChanged { size -> dockHeight = with(density) { size.height.toDp() } },
        ) {
            dock(layout)
        }
    }
}

@Composable
private operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection) + other.calculateStartPadding(layoutDirection),
        top = calculateTopPadding() + other.calculateTopPadding(),
        end = calculateEndPadding(layoutDirection) + other.calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding() + other.calculateBottomPadding(),
    )
}

private fun deviceInfo(crossWindowBlur: Boolean?): String {
    val blur = when (crossWindowBlur) {
        null -> "no cross-window blur below 31"
        true -> "cross-window blur on"
        false -> "cross-window blur off"
    }
    return "API ${Build.VERSION.SDK_INT} · $blur"
}

@Preview
@Composable
private fun SampleScreenPreview() {
    SampleTheme {
        SampleScreen()
    }
}
