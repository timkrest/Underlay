// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.snapshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class SnapshotDrawingTest {

    @Test
    fun aSoftwareCanvasStillShowsAGpuSnapshot() {
        assumeTrue("hardware bitmaps arrive in API 26", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        val snapshot = createBitmap(SNAPSHOT_PX, SNAPSHOT_PX).apply { eraseColor(SNAPSHOT_COLOR) }
        val onGpu = assertNotNull(snapshot.copy(Bitmap.Config.HARDWARE, false), "no hardware bitmap")
        val target = createBitmap(VIEW_PX, VIEW_PX)

        CanvasSnapshotPainter().draw(
            canvas = Canvas(target),
            hostSnapshot = onGpu.asImageBitmap(),
            viewOriginInHost = IntOffset.Zero,
            bounds = Rect(0, 0, VIEW_PX, VIEW_PX),
            alpha = OPAQUE,
        )

        assertEquals(SNAPSHOT_COLOR, target.getPixel(VIEW_PX / 2, VIEW_PX / 2))
    }

    private companion object {
        const val SNAPSHOT_PX = 16
        val VIEW_PX = CaptureScale.hostPixels(SNAPSHOT_PX)
        const val OPAQUE = 255
        val SNAPSHOT_COLOR = Color.Red.toArgb()
    }
}
