// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.snapshot

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

internal class SnapshotPlacement(val source: IntRect, val destination: IntRect) {

    companion object {

        fun of(originInHost: IntOffset, size: IntSize, snapshot: IntSize): SnapshotPlacement? {
            val overlap = hostPixelsUnder(originInHost, size, snapshot) ?: return null
            val source = overlap.inSnapshotPixels()

            return SnapshotPlacement(source = source, destination = source.inHostPixels().translate(-originInHost))
        }

        private fun hostPixelsUnder(originInHost: IntOffset, size: IntSize, snapshot: IntSize): IntRect? {
            val overlap = IntRect(
                left = originInHost.x.coerceAtLeast(0),
                top = originInHost.y.coerceAtLeast(0),
                right = (originInHost.x + size.width).coerceAtMost(CaptureScale.hostPixels(snapshot.width)),
                bottom = (originInHost.y + size.height).coerceAtMost(CaptureScale.hostPixels(snapshot.height)),
            )

            return overlap.takeIf { it.width > 0 && it.height > 0 }
        }

        private fun IntRect.inSnapshotPixels(): IntRect = IntRect(
            left = CaptureScale.snapshotFloor(left),
            top = CaptureScale.snapshotFloor(top),
            right = CaptureScale.snapshotCeil(right),
            bottom = CaptureScale.snapshotCeil(bottom),
        )

        private fun IntRect.inHostPixels(): IntRect = IntRect(
            offset = IntOffset(CaptureScale.hostPixels(left), CaptureScale.hostPixels(top)),
            size = IntSize(CaptureScale.hostPixels(width), CaptureScale.hostPixels(height)),
        )
    }
}
