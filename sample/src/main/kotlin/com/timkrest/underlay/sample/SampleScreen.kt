// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.UnderlaySource

private const val HERO_RATIO = 1.8f
private const val WIDE_HERO_RATIO = 3f

@Composable
internal fun SampleScreen(modifier: Modifier = Modifier) {
    var blurRadiusDp by rememberSaveable { mutableFloatStateOf(DEFAULT_BLUR_RADIUS.value) }
    var openKind by rememberSaveable { mutableStateOf<OverlayKind?>(null) }
    var backdropShift by rememberSaveable { mutableIntStateOf(0) }
    var liveSnapshot by rememberSaveable { mutableStateOf(false) }
    var overlaySource by remember { mutableStateOf<UnderlaySource?>(null) }
    val blurRadius = blurRadiusDp.dp
    val onBlurRadiusChange: (Dp) -> Unit = { radius -> blurRadiusDp = radius.value }
    val tiles = rememberGradientTiles()
    val crossWindowBlur = rememberCrossWindowBlurEnabled()
    val context = LocalContext.current
    val translucentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        openKind = null
        result.data?.getFloatExtra(TranslucentOverlayActivity.EXTRA_BLUR_RADIUS, blurRadiusDp)?.let { blurRadiusDp = it }
    }

    AdaptiveLayout(
        modifier = modifier,
        feed = { layout, contentPadding ->
            if (tiles != null) {
                Backdrop(
                    tiles = tiles,
                    contentPadding = contentPadding,
                    heroRatio = if (layout == DockLayout.Rail) WIDE_HERO_RATIO else HERO_RATIO,
                    shift = backdropShift,
                )
            }
        },
        dock = { layout ->
            ControlDock(
                layout = layout,
                blurRadius = blurRadius,
                overlaySource = overlaySource,
                openKind = openKind,
                liveSnapshot = liveSnapshot,
                deviceInfo = deviceInfo(crossWindowBlur),
                onBlurRadiusChange = onBlurRadiusChange,
                onLiveSnapshotChange = { liveSnapshot = it },
                onOpen = { kind ->
                    openKind = kind
                    if (kind == OverlayKind.TranslucentActivity) {
                        translucentLauncher.launch(TranslucentOverlayActivity.intent(context, blurRadius))
                    }
                },
            )
        },
    )

    OverlayHost(
        kind = openKind,
        source = overlaySource,
        blurRadius = blurRadius,
        liveSnapshot = liveSnapshot,
        onSourceChange = { source -> overlaySource = source },
        onShuffle = { backdropShift++ },
        onBlurRadiusChange = onBlurRadiusChange,
        onDismiss = { openKind = null },
    )
}

/** The feed under a floating dock: along the bottom in portrait, a rail at the end in landscape. */
@Composable
private fun AdaptiveLayout(
    feed: @Composable (layout: DockLayout, contentPadding: PaddingValues) -> Unit,
    dock: @Composable (layout: DockLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layout = if (maxWidth > maxHeight) DockLayout.Rail else DockLayout.Bottom
        val density = LocalDensity.current
        var dockHeight by remember { mutableStateOf(0.dp) }
        val dockReserve = when (layout) {
            DockLayout.Bottom -> PaddingValues(bottom = dockHeight + DOCK_MARGIN * 2)
            DockLayout.Rail -> PaddingValues(end = RAIL_WIDTH + DOCK_MARGIN * 2)
        }

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
