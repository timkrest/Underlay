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
    private val events = FakeHostWindowEvents()
    private var changes = 0

    @Test
    fun `the system blur wins wherever it is on`() = withBackdrop(FakeSystemBlur(isEnabled = true)) {
        start()

        assertEquals(UnderlaySource.SystemBlur, source)
        assertTrue(host.requests.isEmpty(), "nothing is captured under the system blur")
    }

    @Test
    fun `without the system blur the host is captured`() = withBackdrop(FakeSystemBlur(isEnabled = false)) {
        start()
        assertEquals(UnderlaySource.Pending, source)

        host.landNext()

        assertEquals(UnderlaySource.Snapshot, source)
    }

    @Test
    fun `the snapshot takes over when the system stops blurring`() {
        val systemBlur = FakeSystemBlur(isEnabled = true)
        withBackdrop(systemBlur) {
            start()

            systemBlur.switch(isEnabled = false)
            host.landNext()

            assertEquals(UnderlaySource.Snapshot, source)
            assertNull(systemBlur.appliedRadiusPx, "the window flag was left behind")
        }
    }

    @Test
    fun `the system blur takes over again and drops the snapshot`() {
        val systemBlur = FakeSystemBlur(isEnabled = false)
        withBackdrop(systemBlur) {
            start()
            host.landNext()

            systemBlur.switch(isEnabled = true)

            assertEquals(UnderlaySource.SystemBlur, source)
            assertNull(image)
        }
    }

    @Test
    fun `an answer the system repeats changes nothing`() {
        val systemBlur = FakeSystemBlur(isEnabled = false)
        withBackdrop(systemBlur) {
            start()
            val reported = changes

            systemBlur.switch(isEnabled = false)

            assertEquals(1, host.requests.size, "the host was captured twice for the same answer")
            assertEquals(reported, changes)
        }
    }

    @Test
    fun `a new radius reaches the window flag`() {
        val systemBlur = FakeSystemBlur(isEnabled = true)
        withBackdrop(systemBlur) {
            start()

            setRadius(BlurRadius.ofPx(40f))

            assertEquals(40, systemBlur.appliedRadiusPx)
        }
    }

    @Test
    fun `a live snapshot follows host draws only while it is what is drawn`() {
        val systemBlur = FakeSystemBlur(isEnabled = false)
        withBackdrop(systemBlur) {
            start(isLive = true)
            assertTrue(events.isFollowingDraws)

            systemBlur.switch(isEnabled = true)
            assertFalse(events.isFollowingDraws, "the system blur is live on its own")

            systemBlur.switch(isEnabled = false)
            setLive(false)
            assertFalse(events.isFollowingDraws)
        }
    }

    @Test
    fun `a host draw is captured once the capture in flight lands`() = withBackdrop(FakeSystemBlur(isEnabled = false)) {
        start(isLive = true)
        events.draw()
        events.draw()

        host.landNext()

        assertEquals(2, host.requests.size)
    }

    @Test
    fun `a resized host drops the snapshot that no longer lines up`() = withBackdrop(FakeSystemBlur(isEnabled = false)) {
        start()
        host.landNext()

        events.resize()

        assertNull(image)
        assertEquals(UnderlaySource.Pending, source)
        assertEquals(2, host.requests.size)
    }

    @Test
    fun `release lets go of the window and the host`() {
        val systemBlur = FakeSystemBlur(isEnabled = true)
        withBackdrop(systemBlur) {
            start()

            release()

            assertTrue(systemBlur.isReleased)
            assertNull(systemBlur.appliedRadiusPx)
            assertTrue(events.isReleased)
        }
    }

    private fun UnderlayBackdrop.start(isLive: Boolean = false) = start(BlurRadius.ofPx(RADIUS_PX), isLive)

    private fun withBackdrop(
        systemBlur: FakeSystemBlur,
        test: UnderlayBackdrop.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        backdrop(systemBlur).test()
    }

    private fun TestScope.backdrop(systemBlur: FakeSystemBlur) = UnderlayBackdrop(
        systemBlur = systemBlur.factory,
        snapshot = HostSnapshot(host, FakeSnapshotBlur(), backgroundScope, onChange = { changes++ }),
        hostEvents = events.factory,
        onChange = { changes++ },
    )

    private companion object {
        const val RADIUS_PX = 16f
    }
}
