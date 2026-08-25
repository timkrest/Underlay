package com.timkrest.underlay

import android.graphics.Bitmap
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CAPTURE_ATTEMPTS = 3

/** What the snapshot step has to offer right now. */
internal sealed interface UnderlaySnapshot {

    /** A capture is on its way. Nothing to draw yet, but something is coming. */
    data object Pending : UnderlaySnapshot

    class Ready(val image: ImageBitmap) : UnderlaySnapshot

    /** No host window to capture, or the capture kept failing. */
    data object Unavailable : UnderlaySnapshot
}

/**
 * The host window, captured once and blurred with [radius].
 *
 * Capture and blur are separate state. A radius change re-blurs the pixels on hand. A resize drops
 * both during composition, so no frame draws a snapshot sized for the old window. A configuration
 * change replaces the capture without dropping what is on screen.
 */
@Composable
internal fun rememberBlurredSnapshot(host: Window?, radius: Dp): UnderlaySnapshot {
    val hostSize = rememberHostWindowSize(host)
    val configuration = LocalConfiguration.current
    val boxRadius = with(LocalDensity.current) {
        boxBlurRadiusForSigma(sigma = radius.toPx() / WINDOW_CAPTURE_DOWN_SCALE)
    }

    var capture by remember(host, hostSize) { mutableStateOf<Bitmap?>(null) }
    var blurred by remember(host, hostSize) { mutableStateOf<ImageBitmap?>(null) }
    var givenUp by remember(host, hostSize) { mutableStateOf(false) }

    LaunchedEffect(host, hostSize, configuration) {
        if (host == null) return@LaunchedEffect
        givenUp = false
        val captured = try {
            host.captureOnFrameBoundary()
        } catch (noMemoryForCapture: OutOfMemoryError) {
            null
        }
        if (captured != null) capture = captured else givenUp = true
    }

    LaunchedEffect(capture, boxRadius) {
        val pristine = capture ?: return@LaunchedEffect
        try {
            blurred = withContext(Dispatchers.Default) { pristine.blurred(boxRadius).asImageBitmap() }
        } catch (noMemoryForBlur: OutOfMemoryError) {
            givenUp = true
        }
    }

    val image = blurred
    return when {
        image != null -> UnderlaySnapshot.Ready(image)
        host == null || givenUp -> UnderlaySnapshot.Unavailable
        else -> UnderlaySnapshot.Pending
    }
}

@Composable
private fun rememberHostWindowSize(window: Window?): IntSize {
    var size by remember(window) { mutableStateOf(window.decorSize()) }

    DisposableEffect(window) {
        val decorView = window?.decorView ?: return@DisposableEffect onDispose { }
        val listener = View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            size = IntSize(right - left, bottom - top)
        }
        size = window.decorSize()
        decorView.addOnLayoutChangeListener(listener)

        onDispose { decorView.removeOnLayoutChangeListener(listener) }
    }

    return size
}

private fun Window?.decorSize(): IntSize {
    val view = this?.decorView ?: return IntSize.Zero
    return IntSize(view.width, view.height)
}

/** Blurred copy, leaving the receiver intact for the next radius. A zero radius returns it as is. */
private fun Bitmap.blurred(radius: Int): Bitmap {
    if (radius <= 0) return this

    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    blurPixels(pixels, width, height, radius)

    return createBitmap(width, height).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

/**
 * Recycles the buffer only after every attempt has returned. A cancelled [captureInto] leaves a
 * `PixelCopy` still writing into it, and recycling under that request crashes the framework.
 */
private suspend fun Window.captureOnFrameBoundary(): Bitmap? {
    var buffer: Bitmap? = null

    repeat(CAPTURE_ATTEMPTS) {
        withFrameNanos { }
        val destination = buffer ?: createCaptureBitmap() ?: return@repeat
        buffer = destination
        if (captureInto(destination)) return destination
    }

    buffer?.recycle()
    return null
}
