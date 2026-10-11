// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.graphics.createBitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.abs
import kotlin.math.roundToInt

private const val CHANNEL_TOLERANCE = 8f / 255f
private const val OPAQUE_RED = 0xFFFF0000.toInt()
private const val OPAQUE_BLUE = 0xFF0000FF.toInt()

internal fun ImageBitmap.pixelAt(x: Int, y: Int): Color {
    val readable = asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        ?: error("a ${width}x$height snapshot could not be copied for reading")

    return Color(readable.getPixel(x, y))
}

internal fun screenshot(): Bitmap {
    val screen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        ?: error("the screen could not be captured")

    return screen.copy(Bitmap.Config.ARGB_8888, false)
        ?: error("a ${screen.width}x${screen.height} screenshot could not be copied for reading")
}

internal fun Bitmap.colorAt(point: Offset): Color = Color(getPixel(point.x.roundToInt(), point.y.roundToInt()))

internal fun Color.matches(other: Color): Boolean =
    abs(red - other.red) <= CHANNEL_TOLERANCE &&
        abs(green - other.green) <= CHANNEL_TOLERANCE &&
        abs(blue - other.blue) <= CHANNEL_TOLERANCE

internal fun Color.tinted(tint: Color): Color = Color(
    red = tint.red * tint.alpha + red * (1f - tint.alpha),
    green = tint.green * tint.alpha + green * (1f - tint.alpha),
    blue = tint.blue * tint.alpha + blue * (1f - tint.alpha),
)

internal fun Modifier.reportCenterOnScreen(onCenter: (Offset) -> Unit): Modifier = onGloballyPositioned { coordinates ->
    onCenter(coordinates.localToScreen(Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)))
}

internal fun halves(size: Int = 64): ImageBitmap {
    val pixels = IntArray(size * size) { if (it % size < size / 2) OPAQUE_RED else OPAQUE_BLUE }

    return createBitmap(size, size).apply { setPixels(pixels, 0, size, 0, 0, size, size) }.asImageBitmap()
}
