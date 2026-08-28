package com.timkrest.underlay

import android.view.View
import android.view.Window
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

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

    drawImage(
        image = snapshot,
        srcOffset = placement.source.topLeft,
        srcSize = placement.source.size,
        dstOffset = placement.destination.topLeft,
        dstSize = placement.destination.size,
        filterQuality = FilterQuality.Low,
    )
}

internal class SnapshotPlacement(val source: IntRect, val destination: IntRect)

/**
 * Which part of the snapshot goes under a composable of [size] sitting at [originInHost], and where.
 *
 * Both rectangles cover the same host pixels, so the snapshot is never stretched: a composable
 * reaching past the host window gets a backdrop only under the part that overlaps it. Null when
 * there is no overlap.
 */
internal fun snapshotPlacement(originInHost: IntOffset, size: IntSize, snapshot: IntSize): SnapshotPlacement? {
    val left = originInHost.x.coerceAtLeast(0)
    val top = originInHost.y.coerceAtLeast(0)
    val right = (originInHost.x + size.width).coerceAtMost(snapshot.width * WINDOW_CAPTURE_DOWN_SCALE)
    val bottom = (originInHost.y + size.height).coerceAtMost(snapshot.height * WINDOW_CAPTURE_DOWN_SCALE)
    if (right <= left || bottom <= top) return null

    val source = IntRect(
        left = snapshotPixelsFloor(left),
        top = snapshotPixelsFloor(top),
        right = snapshotPixelsCeil(right),
        bottom = snapshotPixelsCeil(bottom),
    )

    return SnapshotPlacement(
        source = source,
        destination = IntRect(
            offset = IntOffset(
                x = source.left * WINDOW_CAPTURE_DOWN_SCALE - originInHost.x,
                y = source.top * WINDOW_CAPTURE_DOWN_SCALE - originInHost.y,
            ),
            size = IntSize(
                width = source.width * WINDOW_CAPTURE_DOWN_SCALE,
                height = source.height * WINDOW_CAPTURE_DOWN_SCALE,
            ),
        ),
    )
}

private fun snapshotPixelsFloor(hostPixels: Int): Int = hostPixels / WINDOW_CAPTURE_DOWN_SCALE

private fun snapshotPixelsCeil(hostPixels: Int): Int =
    (hostPixels + WINDOW_CAPTURE_DOWN_SCALE - 1) / WINDOW_CAPTURE_DOWN_SCALE
