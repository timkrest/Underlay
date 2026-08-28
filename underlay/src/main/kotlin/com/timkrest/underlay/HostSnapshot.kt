package com.timkrest.underlay

import android.graphics.Bitmap
import android.view.Window
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/** One blurred capture of [host]. The image on screen stays until a new one is ready. */
internal class HostSnapshot(
    private val host: Window,
    private val blur: SnapshotBlur,
    nodeScope: CoroutineScope,
    private val onChange: () -> Unit,
) {

    /**
     * The node's scope does not always pin the main thread, and a blur that ran on a worker resumes
     * there. The capture draws views and [onChange] touches Compose, so both must be back on main.
     */
    private val scope = nodeScope + Dispatchers.Main.immediate

    var image: ImageBitmap? = null
        private set

    var hasFailed: Boolean = false
        private set

    private var pristine: Bitmap? = null
    private var sigma: Float = 0f
    private var captureJob: Job? = null
    private var blurJob: Job? = null

    fun capture(sigma: Float) {
        this.sigma = sigma
        hasFailed = false
        captureJob?.cancel()
        blurJob?.cancel()
        onChange()
        captureJob = scope.launch {
            val captured = try {
                host.captureOnFrameBoundary()
            } catch (noMemoryForCapture: OutOfMemoryError) {
                null
            }
            if (captured == null) {
                hasFailed = true
                onChange()
                return@launch
            }
            pristine = captured
            blurPristine()
        }
    }

    fun reblur(sigma: Float) {
        if (sigma == this.sigma) return

        this.sigma = sigma
        blurPristine()
    }

    fun discard() {
        captureJob?.cancel()
        blurJob?.cancel()
        pristine = null
        image = null
        hasFailed = false
    }

    private fun blurPristine() {
        val captured = pristine ?: return
        val sigma = sigma

        hasFailed = false
        blurJob?.cancel()
        onChange()
        blurJob = scope.launch {
            try {
                image = if (sigma > 0f) blur.blur(captured, sigma) else captured.asImageBitmap()
            } catch (noMemoryForBlur: OutOfMemoryError) {
                hasFailed = true
            }
            onChange()
        }
    }
}
