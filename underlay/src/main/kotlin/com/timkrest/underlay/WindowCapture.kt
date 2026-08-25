package com.timkrest.underlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal const val WINDOW_CAPTURE_DOWN_SCALE = 4

internal fun Window.createCaptureBitmap(): Bitmap? {
    val view = decorView
    if (!view.isDrawable()) return null

    return createBitmap(
        width = (view.width / WINDOW_CAPTURE_DOWN_SCALE).coerceAtLeast(1),
        height = (view.height / WINDOW_CAPTURE_DOWN_SCALE).coerceAtLeast(1),
    )
}

internal suspend fun Window.captureInto(destination: Bitmap): Boolean {
    val view = decorView
    if (!view.isDrawable()) return false

    return copyPixelsInto(destination) || view.drawScaledInto(destination)
}

private suspend fun Window.copyPixelsInto(destination: Bitmap): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

    return try {
        requestPixelCopy(destination)
    } catch (noBackingSurface: IllegalArgumentException) {
        false
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun Window.requestPixelCopy(destination: Bitmap): Boolean =
    suspendCancellableCoroutine { continuation ->
        PixelCopy.request(
            this,
            destination,
            { result -> continuation.resume(result == PixelCopy.SUCCESS) },
            Handler(Looper.getMainLooper()),
        )
    }

private fun View.drawScaledInto(destination: Bitmap): Boolean = try {
    destination.eraseColor(Color.TRANSPARENT)
    Canvas(destination).withSave {
        scale(1f / WINDOW_CAPTURE_DOWN_SCALE, 1f / WINDOW_CAPTURE_DOWN_SCALE)
        draw(this)
    }
    true
} catch (softwareRenderingRefused: RuntimeException) {
    false
}

private fun View.isDrawable(): Boolean = isShown && width > 0 && height > 0
