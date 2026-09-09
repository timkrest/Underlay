// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
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
private const val CIRCLES_PER_TILE = 5
private const val CIRCLE_ALPHA = 46

private val GRADIENTS = listOf(
    Color(0xFFFF5F6D) to Color(0xFFFFC371),
    Color(0xFF4776E6) to Color(0xFF8E54E9),
    Color(0xFF11998E) to Color(0xFF38EF7D),
    Color(0xFFFC466B) to Color(0xFF3F5EFB),
    Color(0xFFF7971E) to Color(0xFFFFD200),
    Color(0xFF00C9FF) to Color(0xFF92FE9D),
    Color(0xFFEE0979) to Color(0xFFFF6A00),
    Color(0xFF614385) to Color(0xFF516395),
)

@Immutable
internal class GradientTiles(val images: List<ImageBitmap>, val areHardware: Boolean)

@Composable
internal fun rememberGradientTiles(): GradientTiles? {
    val tiles by produceState<GradientTiles?>(null) {
        value = withContext(Dispatchers.Default) { gradientTiles() }
    }

    return tiles
}

internal fun gradientTiles(): GradientTiles {
    val bitmaps = GRADIENTS.mapIndexed { index, (start, end) -> gradientTile(index, start, end) }

    return GradientTiles(
        images = bitmaps.map { it.asImageBitmap() },
        areHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bitmaps.all { it.config == Bitmap.Config.HARDWARE },
    )
}

private fun gradientTile(index: Int, start: Color, end: Color): Bitmap {
    val software = createBitmap(TILE_SIZE, TILE_SIZE)
    val canvas = Canvas(software)
    canvas.drawPaint(gradientPaint(start, end))
    drawCircles(canvas, seed = index)

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

private fun drawCircles(canvas: Canvas, seed: Int) {
    val paint = Paint().apply {
        isAntiAlias = true
        color = Color.White.toArgb()
        alpha = CIRCLE_ALPHA
    }
    repeat(CIRCLES_PER_TILE) { step ->
        val position = (seed * 37 + step * 61) % TILE_SIZE
        canvas.drawCircle(
            position.toFloat(),
            (TILE_SIZE - position).toFloat(),
            TILE_SIZE / (3f + step),
            paint,
        )
    }
}
