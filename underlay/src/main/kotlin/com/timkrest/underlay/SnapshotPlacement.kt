// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.view.View
import android.view.Window
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

internal class SnapshotPlacement(val source: IntRect, val destination: IntRect)

internal fun originInHost(view: View, host: Window, positionInRoot: Offset): IntOffset {
    val viewLocation = IntArray(2)
    val hostLocation = IntArray(2)
    view.getLocationOnScreen(viewLocation)
    host.decorView.getLocationOnScreen(hostLocation)

    return IntOffset(
        x = viewLocation[0] - hostLocation[0] + positionInRoot.x.roundToInt(),
        y = viewLocation[1] - hostLocation[1] + positionInRoot.y.roundToInt(),
    )
}

internal fun DrawScope.drawSnapshot(snapshot: ImageBitmap, originInHost: IntOffset) {
    val placement = snapshotPlacement(
        originInHost = originInHost,
        size = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        snapshot = IntSize(snapshot.width, snapshot.height),
    ) ?: return

    clipRect {
        drawImage(
            image = snapshot,
            srcOffset = placement.source.topLeft,
            srcSize = placement.source.size,
            dstOffset = placement.destination.topLeft,
            dstSize = placement.destination.size,
            filterQuality = FilterQuality.Low,
        )
    }
}

internal fun snapshotPlacement(originInHost: IntOffset, size: IntSize, snapshot: IntSize): SnapshotPlacement? {
    val overlap = hostPixelsUnder(originInHost, size, snapshot) ?: return null
    val source = overlap.inSnapshotPixels()

    return SnapshotPlacement(source = source, destination = source.inHostPixels().translate(-originInHost))
}

private fun hostPixelsUnder(originInHost: IntOffset, size: IntSize, snapshot: IntSize): IntRect? {
    val overlap = IntRect(
        left = originInHost.x.coerceAtLeast(0),
        top = originInHost.y.coerceAtLeast(0),
        right = (originInHost.x + size.width).coerceAtMost(snapshot.width * WINDOW_CAPTURE_DOWN_SCALE),
        bottom = (originInHost.y + size.height).coerceAtMost(snapshot.height * WINDOW_CAPTURE_DOWN_SCALE),
    )

    return overlap.takeIf { it.width > 0 && it.height > 0 }
}

private fun IntRect.inSnapshotPixels(): IntRect = IntRect(
    left = snapshotPixelsFloor(left),
    top = snapshotPixelsFloor(top),
    right = snapshotPixelsCeil(right),
    bottom = snapshotPixelsCeil(bottom),
)

private fun IntRect.inHostPixels(): IntRect = IntRect(
    offset = IntOffset(left * WINDOW_CAPTURE_DOWN_SCALE, top * WINDOW_CAPTURE_DOWN_SCALE),
    size = IntSize(width * WINDOW_CAPTURE_DOWN_SCALE, height * WINDOW_CAPTURE_DOWN_SCALE),
)

internal fun snapshotPixelsFloor(hostPixels: Int): Int = hostPixels / WINDOW_CAPTURE_DOWN_SCALE

private fun snapshotPixelsCeil(hostPixels: Int): Int =
    (hostPixels + WINDOW_CAPTURE_DOWN_SCALE - 1) / WINDOW_CAPTURE_DOWN_SCALE
