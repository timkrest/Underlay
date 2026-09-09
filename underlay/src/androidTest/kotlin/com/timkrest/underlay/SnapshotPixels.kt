// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.roundToInt

/** Reads a snapshot pixel through a software copy: the GPU blur hands back a hardware bitmap. */
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

internal fun Modifier.reportCenterOnScreen(onCenter: (Offset) -> Unit): Modifier = onGloballyPositioned { coordinates ->
    onCenter(coordinates.localToScreen(Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)))
}
