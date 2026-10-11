// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import com.timkrest.underlay.snapshot.CaptureScale
import com.timkrest.underlay.snapshot.HostCapture
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val CAPTURE_ATTEMPTS = 3

private val mainHandler = Handler(Looper.getMainLooper())

internal class WindowCapture(private val window: Window, private val isAllowed: () -> Boolean) : HostCapture {

    override suspend fun capture(): ImageBitmap? {
        repeat(CAPTURE_ATTEMPTS) {
            awaitDrawnFrame()
            if (!isAllowed()) return null
            val destination = window.createCaptureBitmap()
            if (destination != null && window.captureInto(destination)) return destination.asImageBitmap()
        }

        return null
    }
}

internal fun Window.createCaptureBitmap(): Bitmap? {
    val view = decorView
    if (!view.isDrawable()) return null

    return createBitmap(
        width = CaptureScale.snapshotFloor(view.width).coerceAtLeast(1),
        height = CaptureScale.snapshotFloor(view.height).coerceAtLeast(1),
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
            hostPixelsCapturedInto(destination),
            destination,
            { result -> continuation.resume(result == PixelCopy.SUCCESS) },
            mainHandler,
        )
    }

private fun hostPixelsCapturedInto(destination: Bitmap) = Rect(
    0,
    0,
    CaptureScale.hostPixels(destination.width),
    CaptureScale.hostPixels(destination.height),
)

private fun View.drawScaledInto(destination: Bitmap): Boolean = try {
    destination.eraseColor(Color.TRANSPARENT)
    Canvas(destination).withSave {
        val scale = 1f / CaptureScale.DOWNSCALE
        scale(scale, scale)
        draw(this)
    }
    true
} catch (softwareRenderingRefused: RuntimeException) {
    false
}

private fun View.isDrawable(): Boolean = isShown && width > 0 && height > 0
