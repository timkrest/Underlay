// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.compose.ui.unit.IntOffset
import com.timkrest.underlay.snapshot.CanvasSnapshotPainter
import kotlin.math.roundToInt

private const val OPAQUE = 255

internal class UnderlayDrawable(
    private val view: View,
    private val replaced: Drawable?,
    @ColorInt tint: Int,
    @ColorInt fallback: Int,
) : Drawable() {

    private val fill = Paint()
    private val painter = CanvasSnapshotPainter()
    private var drawableAlpha = OPAQUE
    private var drawnOrigin: IntOffset? = null

    var controller: UnderlayController? = null
        set(value) {
            field = value
            drawnOrigin = null
            invalidateSelf()
        }

    @ColorInt
    var tintColor: Int = tint
        set(value) {
            field = value
            invalidateSelf()
        }

    @ColorInt
    var fallbackColor: Int = fallback
        set(value) {
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val source = controller?.source ?: UnderlaySource.Fallback
        if (source == UnderlaySource.Snapshot) drawHostSnapshot(canvas)
        fill(canvas, if (source == UnderlaySource.Fallback) fallbackColor else tintColor)
    }

    fun invalidateIfMoved() {
        val controller = controller ?: return
        if (controller.source != UnderlaySource.Snapshot) return

        if (controller.snapshotOrigin(view) != drawnOrigin) invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha
        invalidateSelf()
    }

    override fun getAlpha(): Int = drawableAlpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fill.colorFilter = colorFilter
        painter.setColorFilter(colorFilter)
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        replaced?.bounds = bounds
    }

    override fun getOutline(outline: Outline) {
        if (replaced != null) replaced.getOutline(outline) else super.getOutline(outline)
    }

    override fun getPadding(padding: Rect): Boolean = replaced?.getPadding(padding) ?: super.getPadding(padding)

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun drawHostSnapshot(canvas: Canvas) {
        val controller = controller ?: return
        val snapshot = controller.snapshot ?: return
        val origin = controller.snapshotOrigin(view) ?: return
        drawnOrigin = origin

        painter.draw(canvas, snapshot, origin, bounds, alpha = (controller.snapshotAlpha * drawableAlpha).roundToInt())
    }

    private fun fill(canvas: Canvas, @ColorInt color: Int) {
        fill.color = color
        fill.alpha = fill.alpha * drawableAlpha / OPAQUE
        canvas.drawRect(bounds, fill)
    }
}
