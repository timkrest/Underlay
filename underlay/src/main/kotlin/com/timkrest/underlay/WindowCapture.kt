// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.withFrameNanos
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal const val WINDOW_CAPTURE_DOWN_SCALE = 4

private const val CAPTURE_ATTEMPTS = 3

/**
 * The window as it was last drawn, or null if no attempt succeeded. The buffer is recycled only
 * after every attempt has returned, and not in a `finally`: a cancelled [captureInto] leaves a
 * `PixelCopy` still writing into it, and recycling under that request crashes the framework.
 */
internal suspend fun Window.captureOnFrameBoundary(): Bitmap? {
    var buffer: Bitmap? = null

    repeat(CAPTURE_ATTEMPTS) {
        withFrameNanos { }
        awaitFrameDrawn()
        val destination = buffer ?: createCaptureBitmap() ?: return@repeat
        buffer = destination
        if (captureInto(destination)) return destination
    }

    buffer?.recycle()
    return null
}

/**
 * Waits for the vsync after this one. Compose's clock ticks before a frame is drawn and a capture
 * reads what the window last put on screen, so without this it would return the frame before the
 * change that asked for it. A vsync also arrives when Compose has no more frames to draw.
 */
private suspend fun awaitFrameDrawn(): Unit = suspendCancellableCoroutine { continuation ->
    val choreographer = Choreographer.getInstance()
    val callback = Choreographer.FrameCallback { continuation.resume(Unit) }

    choreographer.postFrameCallback(callback)
    continuation.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
}

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
