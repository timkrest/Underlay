// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TILE_SIZE = 512
private const val BLOBS_PER_TILE = 3
private const val BLOB_STRIDE = 173
private const val BLOB_ALPHA = 0.8f
private const val HIGHLIGHT_ALPHA = 0.35f
private const val RING_STROKE = 6f
private const val RING_ALPHA = 0.55f
private const val RINGS_PER_TILE = 2

private val PALETTE = listOf(
    Color(0xFFFF5F6D) to Color(0xFFFFC371),
    Color(0xFF4776E6) to Color(0xFF8E54E9),
    Color(0xFF11998E) to Color(0xFF38EF7D),
    Color(0xFFFC466B) to Color(0xFF3F5EFB),
    Color(0xFFF7971E) to Color(0xFFFFD200),
    Color(0xFF00C9FF) to Color(0xFF92FE9D),
    Color(0xFFEE0979) to Color(0xFFFF6A00),
    Color(0xFF614385) to Color(0xFF516395),
    Color(0xFF0F2027) to Color(0xFF2C5364),
    Color(0xFFFF9A9E) to Color(0xFFFAD0C4),
    Color(0xFFA18CD1) to Color(0xFFFBC2EB),
    Color(0xFF43E97B) to Color(0xFF38F9D7),
)

@Immutable
internal class GradientTiles(val images: List<ImageBitmap>)

@Composable
internal fun rememberGradientTiles(): GradientTiles? {
    val tiles by produceState<GradientTiles?>(null) {
        value = withContext(Dispatchers.Default) { gradientTiles() }
    }

    return tiles
}

internal fun gradientTiles(): GradientTiles {
    val bitmaps = PALETTE.indices.map(::gradientTile)

    return GradientTiles(images = bitmaps.map { it.asImageBitmap() })
}

private fun gradientTile(index: Int): Bitmap {
    val (start, end) = PALETTE[index]
    val software = createBitmap(TILE_SIZE, TILE_SIZE)
    val canvas = Canvas(software)
    canvas.drawPaint(gradientPaint(start, end))
    drawBlobs(canvas, index)
    drawRings(canvas, index)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return software
    return software.copy(Bitmap.Config.HARDWARE, false)?.also { software.recycle() } ?: software
}

private fun gradientPaint(start: Color, end: Color) = Paint().apply {
    shader = LinearGradient(
        0f,
        0f,
        TILE_SIZE.toFloat(),
        TILE_SIZE.toFloat(),
        start.toArgb(),
        end.toArgb(),
        Shader.TileMode.CLAMP,
    )
}

private fun drawBlobs(canvas: Canvas, index: Int) {
    val size = TILE_SIZE.toFloat()
    repeat(BLOBS_PER_TILE) { step ->
        val (accent, _) = PALETTE[(index + step + 1) % PALETTE.size]
        val color = if (step == 0) Color.White.copy(alpha = HIGHLIGHT_ALPHA) else accent.copy(alpha = BLOB_ALPHA)
        val x = (index * BLOB_STRIDE + step * BLOB_STRIDE * 2) % TILE_SIZE
        val y = (index * BLOB_STRIDE * 3 + step * BLOB_STRIDE) % TILE_SIZE
        val radius = size / (1.6f + step)
        val paint = Paint().apply {
            shader = RadialGradient(
                x.toFloat(),
                y.toFloat(),
                radius,
                color.toArgb(),
                Color.Transparent.toArgb(),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(x.toFloat(), y.toFloat(), radius, paint)
    }
}

private fun drawRings(canvas: Canvas, index: Int) {
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = RING_STROKE
        color = Color.White.copy(alpha = RING_ALPHA).toArgb()
    }
    repeat(RINGS_PER_TILE) { step ->
        val x = (index * BLOB_STRIDE * 2 + step * BLOB_STRIDE) % TILE_SIZE
        val y = (index * BLOB_STRIDE + step * BLOB_STRIDE * 2) % TILE_SIZE
        canvas.drawCircle(x.toFloat(), y.toFloat(), TILE_SIZE / (4f + step * 2), paint)
    }
}
