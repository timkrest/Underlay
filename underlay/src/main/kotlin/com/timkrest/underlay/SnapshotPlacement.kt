package com.timkrest.underlay

import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

internal class SnapshotPlacement(private val view: View, private val host: Window?) {

    private val viewLocation = IntArray(2)
    private val hostLocation = IntArray(2)

    var originInHost: IntOffset by mutableStateOf(IntOffset.Zero)
        private set

    fun onPositioned(coordinates: LayoutCoordinates) {
        if (host == null) return

        view.getLocationOnScreen(viewLocation)
        host.decorView.getLocationOnScreen(hostLocation)
        val positionInView = coordinates.positionInRoot()

        originInHost = IntOffset(
            x = viewLocation[0] - hostLocation[0] + positionInView.x.roundToInt(),
            y = viewLocation[1] - hostLocation[1] + positionInView.y.roundToInt(),
        )
    }
}

@Composable
internal fun rememberSnapshotPlacement(host: Window?): SnapshotPlacement {
    val view = LocalView.current
    return remember(view, host) { SnapshotPlacement(view, host) }
}

internal fun DrawScope.drawSnapshot(snapshot: ImageBitmap, originInHost: IntOffset) {
    val destination = IntSize(size.width.roundToInt(), size.height.roundToInt())
    val source = snapshotSourceRect(
        originInHost = originInHost,
        destination = destination,
        snapshot = IntSize(snapshot.width, snapshot.height),
    )

    drawImage(
        image = snapshot,
        srcOffset = source.topLeft,
        srcSize = source.size,
        dstOffset = IntOffset.Zero,
        dstSize = destination,
        filterQuality = FilterQuality.Low,
    )
}

/**
 * The part of a downscaled host-window [snapshot] behind a composable of size [destination], whose
 * top left is [originInHost] host pixels into that window. Always lands inside the snapshot: an
 * origin past its edge is pulled back, a destination reaching past it is cut short.
 */
internal fun snapshotSourceRect(originInHost: IntOffset, destination: IntSize, snapshot: IntSize): IntRect {
    val lastColumn = (snapshot.width - 1).coerceAtLeast(0)
    val lastRow = (snapshot.height - 1).coerceAtLeast(0)
    val left = snapshotPixelsFloor(originInHost.x).coerceIn(0, lastColumn)
    val top = snapshotPixelsFloor(originInHost.y).coerceIn(0, lastRow)

    return IntRect(
        offset = IntOffset(left, top),
        size = IntSize(
            width = snapshotPixelsCeil(destination.width).coerceIn(1, (snapshot.width - left).coerceAtLeast(1)),
            height = snapshotPixelsCeil(destination.height).coerceIn(1, (snapshot.height - top).coerceAtLeast(1)),
        ),
    )
}

private fun snapshotPixelsFloor(hostPixels: Int): Int = hostPixels / WINDOW_CAPTURE_DOWN_SCALE

private fun snapshotPixelsCeil(hostPixels: Int): Int =
    (hostPixels + WINDOW_CAPTURE_DOWN_SCALE - 1) / WINDOW_CAPTURE_DOWN_SCALE
