// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.backdrop

import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.snapshot.HostSnapshot
import com.timkrest.underlay.testing.FakeHostCapture
import com.timkrest.underlay.testing.FakeHostWindowEvents
import com.timkrest.underlay.testing.FakeSnapshotBlur
import com.timkrest.underlay.testing.FakeSystemBlur
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UnderlayBackdropTest {

    private val host = FakeHostCapture()
    private val blur = FakeSnapshotBlur()
    private val events = FakeHostWindowEvents()
    private val systemBlur = FakeSystemBlur(isEnabled = false)
    private var changes = 0

    @Test
    fun `the system blur wins whenever it is on`() = withBackdrop(isSystemBlurOn = true) {
        start()

        assertEquals(UnderlaySource.SystemBlur, source)
        assertEquals(RADIUS_PX.toInt(), systemBlur.appliedRadiusPx)
        assertTrue(host.requests.isEmpty(), "nothing is captured under the system blur")
    }

    @Test
    fun `without the system blur the host is captured at a quarter of the sigma`() = withBackdrop {
        start()
        assertEquals(UnderlaySource.Pending, source)

        host.landNext()

        assertEquals(UnderlaySource.Snapshot, source)
        assertEquals(listOf(BlurRadius.ofPx(RADIUS_PX).snapshotSigma), blur.sigmas)
    }

    @Test
    fun `the snapshot takes over when the system stops blurring`() = withBackdrop(isSystemBlurOn = true) {
        start()

        systemBlur.switch(isEnabled = false)
        host.landNext()

        assertEquals(UnderlaySource.Snapshot, source)
        assertEquals(1, systemBlur.withdrawals, "the window flag was left behind")
    }

    @Test
    fun `the system blur takes over again and drops the snapshot`() = withBackdrop {
        start()
        host.landNext()

        systemBlur.switch(isEnabled = true)

        assertEquals(UnderlaySource.SystemBlur, source)
        assertNull(image)
    }

    @Test
    fun `the same answer from the system changes nothing`() = withBackdrop {
        start()
        val reported = changes

        systemBlur.switch(isEnabled = false)

        assertEquals(1, host.requests.size, "the host was captured twice for the same answer")
        assertEquals(reported, changes)
    }

    @Test
    fun `a new radius reaches the window flag`() = withBackdrop(isSystemBlurOn = true) {
        start()

        setRadius(BlurRadius.ofPx(40f))

        assertEquals(40, systemBlur.appliedRadiusPx)
    }

    @Test
    fun `a new radius reblurs the snapshot`() = withBackdrop {
        start()
        host.landNext()

        setRadius(BlurRadius.ofPx(40f))

        assertEquals(BlurRadius.ofPx(40f).snapshotSigma, blur.sigmas.last())
    }

    @Test
    fun `a live snapshot follows host draws only while it is what is drawn`() = withBackdrop {
        start(isLive = true)
        assertTrue(events.isFollowingDraws)

        systemBlur.switch(isEnabled = true)
        assertFalse(events.isFollowingDraws, "the system blur is live on its own")

        systemBlur.switch(isEnabled = false)
        setLive(false)
        assertFalse(events.isFollowingDraws)
    }

    @Test
    fun `a host draw asks for a capture once the one in flight lands`() = withBackdrop {
        start(isLive = true)
        events.draw()
        events.draw()

        host.landNext()

        assertEquals(2, host.requests.size)
    }

    @Test
    fun `a resized host drops the snapshot that no longer lines up`() = withBackdrop {
        start()
        host.landNext()

        events.resize()

        assertNull(image)
        assertEquals(UnderlaySource.Pending, source)
        assertEquals(2, host.requests.size)
    }

    @Test
    fun `release lets go of the window and drops a capture in flight`() = withBackdrop {
        start()
        val reported = changes

        release()
        host.landNext()

        assertTrue(systemBlur.isReleased)
        assertTrue(events.isReleased)
        assertNull(image)
        assertEquals(reported, changes)
    }

    @Test
    fun `without a host and a system blur there is only the fallback`() = runTest(UnconfinedTestDispatcher()) {
        val backdrop = UnderlayBackdrop(systemBlur = null, snapshot = null, hostEvents = null, onChange = {})

        backdrop.start(BlurRadius.ofPx(RADIUS_PX), isLive = true)

        assertEquals(UnderlaySource.Fallback, backdrop.source)
    }

    @Test
    fun `without a host the system blur falls back when it stops`() = runTest(UnconfinedTestDispatcher()) {
        systemBlur.isEnabled = true
        val backdrop = UnderlayBackdrop(systemBlur.factory, snapshot = null, hostEvents = null, onChange = {})
        backdrop.start(BlurRadius.ofPx(RADIUS_PX), isLive = false)

        systemBlur.switch(isEnabled = false)

        assertEquals(UnderlaySource.Fallback, backdrop.source)
    }

    @Test
    fun `before API 31 the host is captured straight away`() = runTest(UnconfinedTestDispatcher()) {
        val backdrop = UnderlayBackdrop(systemBlur = null, snapshot(), events.factory, onChange = {})

        backdrop.start(BlurRadius.ofPx(RADIUS_PX), isLive = false)
        host.landNext()

        assertEquals(UnderlaySource.Snapshot, backdrop.source)
    }

    private fun UnderlayBackdrop.start(isLive: Boolean = false) = start(BlurRadius.ofPx(RADIUS_PX), isLive)

    private fun withBackdrop(isSystemBlurOn: Boolean = false, test: UnderlayBackdrop.() -> Unit) =
        runTest(UnconfinedTestDispatcher()) {
            systemBlur.isEnabled = isSystemBlurOn
            UnderlayBackdrop(systemBlur.factory, snapshot(), events.factory, onChange = { changes++ }).test()
        }

    private fun TestScope.snapshot() = HostSnapshot(host, blur, backgroundScope, onChange = { changes++ })

    private companion object {
        const val RADIUS_PX = 16f
    }
}
