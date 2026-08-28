package com.timkrest.underlay

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

/** Reads a snapshot pixel through a software copy: the GPU blur hands back a hardware bitmap. */
internal fun ImageBitmap.pixelAt(x: Int, y: Int): Color {
    val readable = asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        ?: error("a ${width}x$height snapshot could not be copied for reading")

    return Color(readable.getPixel(x, y))
}
