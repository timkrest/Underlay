// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.testing

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.timkrest.underlay.backdrop.HostWindowEvents
import com.timkrest.underlay.backdrop.HostWindowEventsFactory
import com.timkrest.underlay.backdrop.SystemBlurBehind
import com.timkrest.underlay.backdrop.SystemBlurFactory
import com.timkrest.underlay.blur.SnapshotBlur
import com.timkrest.underlay.snapshot.HostCapture
import kotlinx.coroutines.CompletableDeferred

internal open class FakeImage(private val name: String) : ImageBitmap {
    override val width: Int = 4
    override val height: Int = 4
    override val colorSpace: ColorSpace = ColorSpaces.Srgb
    override val hasAlpha: Boolean = true
    override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int,
    ) = Unit

    override fun prepareToDraw() = Unit

    override fun toString(): String = name
}

internal class BlurredImage(val capture: ImageBitmap, val sigma: Float) : FakeImage("$capture blurred at $sigma")

internal class FakeHostCapture : HostCapture {

    val requests = mutableListOf<CompletableDeferred<ImageBitmap?>>()
    private var captures = 0

    override suspend fun capture(): ImageBitmap? = CompletableDeferred<ImageBitmap?>().also { requests += it }.await()

    fun landNext(): ImageBitmap = FakeImage("capture ${++captures}").also(::answer)

    fun fail() = answer(null)

    private fun answer(image: ImageBitmap?) {
        val request = requests.lastOrNull { !it.isCompleted } ?: error("no capture is waiting for an answer")
        request.complete(image)
    }
}

internal class FakeSnapshotBlur : SnapshotBlur {

    val sigmas = mutableListOf<Float>()
    var cacheClears = 0
        private set
    var isSuspending = false
    var runsOutOfMemory = false
    private val pending = mutableListOf<CompletableDeferred<Unit>>()

    override suspend fun blur(capture: ImageBitmap, sigma: Float): ImageBitmap {
        sigmas += sigma
        if (runsOutOfMemory) throw OutOfMemoryError()
        if (isSuspending) CompletableDeferred<Unit>().also { pending += it }.await()
        return BlurredImage(capture, sigma)
    }

    fun finishPending() {
        val waiting = pending.toList()
        pending.clear()
        waiting.forEach { it.complete(Unit) }
    }

    override fun clearCache() {
        cacheClears++
    }

    override fun release() = Unit
}

internal class FakeSystemBlur(var isEnabled: Boolean) : SystemBlurBehind {

    var appliedRadiusPx: Int? = null
        private set
    var withdrawals = 0
        private set
    var isReleased = false
        private set
    private var onEnabledChange: () -> Unit = {}

    val factory: SystemBlurFactory = { onEnabledChange ->
        this.onEnabledChange = onEnabledChange
        this
    }

    override fun request(radiusPx: Int): Boolean {
        if (isEnabled) appliedRadiusPx = radiusPx
        return isEnabled
    }

    override fun withdraw() {
        withdrawals++
        appliedRadiusPx = null
    }

    override fun release() {
        isReleased = true
    }

    fun switch(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        onEnabledChange()
    }
}

internal class FakeHostWindowEvents : HostWindowEvents {

    var isFollowingDraws = false
        private set
    var isReleased = false
        private set
    private var onResize: () -> Unit = {}
    private var onDraw: () -> Unit = {}

    val factory: HostWindowEventsFactory = { onResize, onDraw ->
        this.onResize = onResize
        this.onDraw = onDraw
        this
    }

    override fun followDraws(isFollowing: Boolean) {
        isFollowingDraws = isFollowing
    }

    override fun release() {
        isReleased = true
    }

    fun resize() = onResize()

    fun draw() {
        if (isFollowingDraws) onDraw()
    }
}
