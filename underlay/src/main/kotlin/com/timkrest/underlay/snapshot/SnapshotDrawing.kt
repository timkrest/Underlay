// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.snapshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.withClip
import kotlin.math.roundToInt

internal fun DrawScope.drawSnapshot(hostSnapshot: ImageBitmap, originInHost: IntOffset, alpha: Float) {
    val snapshot = hostSnapshot.drawableOn(drawContext.canvas.nativeCanvas) ?: return
    val placement = SnapshotPlacement.of(
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
            alpha = alpha,
            filterQuality = FilterQuality.Low,
        )
    }
}

internal class CanvasSnapshotPainter {

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val source = Rect()
    private val destination = Rect()

    fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    fun draw(canvas: Canvas, hostSnapshot: ImageBitmap, viewOriginInHost: IntOffset, bounds: Rect, alpha: Int) {
        val snapshot = hostSnapshot.drawableOn(canvas) ?: return
        val placement = SnapshotPlacement.of(
            originInHost = viewOriginInHost + IntOffset(bounds.left, bounds.top),
            size = IntSize(bounds.width(), bounds.height()),
            snapshot = IntSize(snapshot.width, snapshot.height),
        ) ?: return

        placement.source.copyInto(source)
        placement.destination.translate(bounds.left, bounds.top).copyInto(destination)
        paint.alpha = alpha
        canvas.withClip(bounds) { drawBitmap(snapshot.asAndroidBitmap(), source, destination, paint) }
    }

    private fun IntRect.copyInto(rect: Rect) = rect.set(left, top, right, bottom)
}

// ponytail: a software canvas reads the GPU snapshot back on every draw; cache the copy per snapshot if a software layer shows up in a profile
private fun ImageBitmap.drawableOn(canvas: Canvas): ImageBitmap? {
    if (canvas.isHardwareAccelerated || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return this
    val bitmap = asAndroidBitmap()
    if (bitmap.config != Bitmap.Config.HARDWARE) return this

    return bitmap.copy(Bitmap.Config.ARGB_8888, false)?.asImageBitmap()
}
