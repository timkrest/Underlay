// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.snapshot

import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.testing.BlurredImage
import com.timkrest.underlay.testing.FakeHostCapture
import com.timkrest.underlay.testing.FakeImage
import com.timkrest.underlay.testing.FakeSnapshotBlur
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HostSnapshotTest {

    private val host = FakeHostCapture()
    private val blur = FakeSnapshotBlur()
    private val reports = mutableListOf<UnderlaySource>()

    @Test
    fun `nothing is drawn but the tint until the first capture is blurred`() = withSnapshot { snapshot ->
        snapshot.capture(SIGMA)
        assertEquals(UnderlaySource.Pending, snapshot.source)

        val captured = host.landNext()

        val image = assertIs<BlurredImage>(snapshot.image)
        assertSame(captured, image.capture)
        assertEquals(SIGMA, image.sigma)
        assertEquals(listOf(UnderlaySource.Snapshot), reports)
    }

    @Test
    fun `a first capture that fails falls back, and a later one recovers`() = withSnapshot { snapshot ->
        snapshot.capture(SIGMA)
        host.fail()

        snapshot.capture(SIGMA)
        assertEquals(UnderlaySource.Fallback, snapshot.source, "what is on screen stays until a capture lands")
        host.landNext()

        assertEquals(listOf(UnderlaySource.Fallback, UnderlaySource.Pending, UnderlaySource.Snapshot), reports)
    }

    @Test
    fun `a failed recapture keeps the snapshot on screen`() = withSnapshot { snapshot ->
        snapshot.capture(SIGMA)
        host.landNext()
        val shown = snapshot.image

        snapshot.capture(SIGMA)
        host.fail()

        assertSame(shown, snapshot.image)
        assertEquals(UnderlaySource.Snapshot, snapshot.source)
    }

    @Test
    fun `a capture asked for while one is in flight waits for it and runs once`() = withSnapshot { snapshot ->
        snapshot.capture(SIGMA)
        repeat(3) { snapshot.captureWhenIdle(SIGMA) }
        assertEquals(1, host.requests.size, "one capture in flight at a time")

        host.landNext()
        assertEquals(2, host.requests.size, "the waiting draws are captured together, once")

        host.landNext()
        assertEquals(2, host.requests.size)
    }

    @Test
    fun `a capture asked for while a failing one is in flight still runs`() = withSnapshot { snapshot ->
        snapshot.capture(SIGMA)
        snapshot.captureWhenIdle(SIGMA)

        host.fail()

        assertEquals(2, host.requests.size)
    }

    @Test
    fun `an explicit capture drops the one in flight`() = withSnapshot { snapshot ->
        snapshot.capture(SIGMA)
        val stale = host.requests.single()

        snapshot.capture(SIGMA)
        stale.complete(FakeImage("stale"))
        val fresh = host.landNext()

        assertSame(fresh, assertIs<BlurredImage>(snapshot.image).capture)
    }

    @Test
    fun `a new radius reblurs the pixels already captured`() = withSnapshot { snapshot ->
        snapshot.capture(SIGMA)
        val captured = host.landNext()

        snapshot.reblur(SIGMA)
        snapshot.reblur(OTHER_SIGMA)

        assertSame(captured, assertIs<BlurredImage>(snapshot.image).capture)
        assertEquals(listOf(SIGMA, OTHER_SIGMA), blur.sigmas, "the same radius was blurred twice")
        assertEquals(1, host.requests.size, "the host was read back again")
    }

    @Test
    fun `a radius changed during a blur is caught up with once that blur lands`() = withSnapshot { snapshot ->
        blur.isSuspending = true
        snapshot.capture(SIGMA)
        host.landNext()

        snapshot.reblur(OTHER_SIGMA)
        snapshot.reblur(LAST_SIGMA)
        blur.finishPending()
        blur.finishPending()

        assertEquals(listOf(SIGMA, LAST_SIGMA), blur.sigmas, "the radius in between was never wanted")
        assertEquals(LAST_SIGMA, assertIs<BlurredImage>(snapshot.image).sigma)
    }

    @Test
    fun `a capture asked for during a reblur runs after it`() = withSnapshot { snapshot ->
        snapshot.capture(SIGMA)
        host.landNext()
        blur.isSuspending = true

        snapshot.reblur(OTHER_SIGMA)
        snapshot.captureWhenIdle(OTHER_SIGMA)
        blur.finishPending()

        assertEquals(2, host.requests.size)
    }

    @Test
    fun `without a radius the capture itself is drawn`() = withSnapshot { snapshot ->
        snapshot.capture(sigma = 0f)
        val captured = host.landNext()

        assertSame(captured, snapshot.image)
        assertTrue(blur.sigmas.isEmpty())
    }

    @Test
    fun `a blur that runs out of memory falls back, then keeps what is on screen`() = withSnapshot { snapshot ->
        blur.runsOutOfMemory = true
        snapshot.capture(SIGMA)
        host.landNext()
        assertEquals(UnderlaySource.Fallback, snapshot.source)

        blur.runsOutOfMemory = false
        snapshot.capture(SIGMA)
        host.landNext()
        val shown = snapshot.image

        blur.runsOutOfMemory = true
        snapshot.capture(SIGMA)
        host.landNext()
        assertSame(shown, snapshot.image)
    }

    @Test
    fun `a discarded snapshot forgets its failure and starts over`() = withSnapshot { snapshot ->
        snapshot.capture(SIGMA)
        host.fail()
        snapshot.captureWhenIdle(SIGMA)

        snapshot.discard()
        assertEquals(UnderlaySource.Pending, snapshot.source)
        assertEquals(1, blur.cacheClears)

        snapshot.capture(SIGMA)
        host.landNext()
        assertEquals(UnderlaySource.Snapshot, snapshot.source)
        assertEquals(3, host.requests.size)
    }

    private fun withSnapshot(test: TestScope.(HostSnapshot) -> Unit) = runTest(UnconfinedTestDispatcher()) {
        lateinit var snapshot: HostSnapshot
        snapshot = HostSnapshot(host, blur, backgroundScope, onChange = { reports += snapshot.source })
        test(snapshot)
    }

    private companion object {
        const val SIGMA = 4f
        const val OTHER_SIGMA = 6f
        const val LAST_SIGMA = 8f
    }
}
