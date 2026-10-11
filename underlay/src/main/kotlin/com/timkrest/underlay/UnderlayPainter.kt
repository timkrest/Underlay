// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.withClip
import com.timkrest.underlay.snapshot.SnapshotPlacement
import kotlin.math.roundToInt

internal const val OPAQUE = 255

internal class UnderlayPainter {

    private val fill = Paint()
    private val snapshotPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val snapshotSource = Rect()
    private val snapshotDestination = Rect()

    fun setColorFilter(colorFilter: ColorFilter?) {
        fill.colorFilter = colorFilter
        snapshotPaint.colorFilter = colorFilter
    }

    fun draw(
        canvas: Canvas,
        bounds: Rect,
        controller: UnderlayController?,
        @ColorInt tint: Int,
        @ColorInt fallback: Int,
        alpha: Int = OPAQUE,
    ) {
        val source = controller?.source ?: UnderlaySource.Fallback
        if (source == UnderlaySource.Snapshot) controller?.let { drawSnapshotOf(it, canvas, bounds, alpha) }

        fill.color = if (source == UnderlaySource.Fallback) fallback else tint
        fill.alpha = fill.alpha * alpha / OPAQUE
        canvas.drawRect(bounds, fill)
    }

    fun drawSnapshot(canvas: Canvas, bounds: Rect, hostSnapshot: ImageBitmap, originInHost: IntOffset, alpha: Int) {
        val snapshot = hostSnapshot.drawableOn(canvas) ?: return
        val placement = SnapshotPlacement.of(
            originInHost = originInHost + IntOffset(bounds.left, bounds.top),
            size = IntSize(bounds.width(), bounds.height()),
            snapshot = IntSize(snapshot.width, snapshot.height),
        ) ?: return

        placement.source.copyInto(snapshotSource)
        placement.destination.translate(bounds.left, bounds.top).copyInto(snapshotDestination)
        snapshotPaint.alpha = alpha
        canvas.withClip(bounds) { drawBitmap(snapshot, snapshotSource, snapshotDestination, snapshotPaint) }
    }

    private fun drawSnapshotOf(controller: UnderlayController, canvas: Canvas, bounds: Rect, alpha: Int) {
        val snapshot = controller.snapshot ?: return
        val origin = controller.recordDrawnOrigin() ?: return

        drawSnapshot(canvas, bounds, snapshot, origin, alpha = (controller.snapshotAlpha * alpha).roundToInt())
    }

    private fun IntRect.copyInto(rect: Rect) = rect.set(left, top, right, bottom)
}

private fun ImageBitmap.drawableOn(canvas: Canvas): Bitmap? {
    val bitmap = asAndroidBitmap()
    if (canvas.isHardwareAccelerated || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return bitmap
    if (bitmap.config != Bitmap.Config.HARDWARE) return bitmap

    return bitmap.copy(Bitmap.Config.ARGB_8888, false)
}
