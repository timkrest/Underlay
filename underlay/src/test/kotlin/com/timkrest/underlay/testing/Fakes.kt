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

internal open class FakeImage(val name: String) : ImageBitmap {
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

    val requests = mutableListOf<Request>()
    private var captures = 0

    val inFlight: List<Request> get() = requests.filter { it.isActive }

    override suspend fun capture(): ImageBitmap? {
        val request = Request()
        requests += request
        return request.result.await()
    }

    fun landNext(): ImageBitmap = FakeImage("capture ${++captures}").also { land(it) }

    fun land(image: ImageBitmap?) {
        val request = requests.lastOrNull { it.isActive } ?: error("no capture in flight, requests: $requests")
        request.result.complete(image)
    }

    fun fail() = land(null)

    class Request {
        val result = CompletableDeferred<ImageBitmap?>()
        val isActive: Boolean get() = !result.isCompleted

        override fun toString(): String = "Request(completed=${result.isCompleted})"
    }
}

internal class FakeSnapshotBlur : SnapshotBlur {

    var isSuspending = false
    val calls = mutableListOf<Float>()
    private val pending = mutableListOf<CompletableDeferred<Unit>>()
    var isReleased = false
        private set

    override suspend fun blur(capture: ImageBitmap, sigma: Float): ImageBitmap {
        calls += sigma
        if (isSuspending) CompletableDeferred<Unit>().also { pending += it }.await()
        return BlurredImage(capture, sigma)
    }

    fun finishPending() {
        val waiting = pending.toList()
        pending.clear()
        waiting.forEach { it.complete(Unit) }
    }

    override fun forgetCaptures() = Unit

    override fun release() {
        isReleased = true
    }
}

internal class FakeSystemBlur(isEnabled: Boolean) : SystemBlurBehind {

    var isEnabled: Boolean = isEnabled
        private set
    var appliedRadiusPx: Int? = null
        private set
    var isReleased = false
        private set
    val requests = mutableListOf<Int>()
    private var onEnabledChange: (() -> Unit)? = null

    val factory: SystemBlurFactory = { onEnabledChange ->
        this.onEnabledChange = onEnabledChange
        this
    }

    override fun request(radiusPx: Int): Boolean {
        requests += radiusPx
        if (!isEnabled) return false

        appliedRadiusPx = radiusPx
        return true
    }

    override fun withdraw() {
        appliedRadiusPx = null
    }

    override fun release() {
        isReleased = true
        onEnabledChange = null
        withdraw()
    }

    fun switch(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        onEnabledChange?.invoke()
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
        isFollowingDraws = false
    }

    fun resize() = onResize()

    fun draw() {
        if (isFollowingDraws) onDraw()
    }
}
