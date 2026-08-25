package com.timkrest.underlay

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Test
import kotlin.test.assertEquals

class SnapshotPlacementTest {

    @Test
    fun `a composable filling the host window maps onto the whole snapshot`() {
        val source = snapshotSourceRect(
            originInHost = IntOffset.Zero,
            destination = IntSize(width = 400, height = 800),
            snapshot = snapshotOf(hostWidth = 400, hostHeight = 800),
        )

        assertEquals(IntOffset.Zero, source.topLeft)
        assertEquals(IntSize(width = 100, height = 200), source.size)
    }

    @Test
    fun `an origin inside the host window scales down by the capture factor`() {
        val source = snapshotSourceRect(
            originInHost = IntOffset(x = 80, y = 40),
            destination = IntSize(width = 200, height = 100),
            snapshot = snapshotOf(hostWidth = 400, hostHeight = 800),
        )

        assertEquals(IntOffset(x = 20, y = 10), source.topLeft)
        assertEquals(IntSize(width = 50, height = 25), source.size)
    }

    @Test
    fun `a destination that is not a multiple of the capture factor rounds up`() {
        val source = snapshotSourceRect(
            originInHost = IntOffset.Zero,
            destination = IntSize(width = 401, height = 3),
            snapshot = snapshotOf(hostWidth = 4000, hostHeight = 4000),
        )

        assertEquals(IntSize(width = 101, height = 1), source.size)
    }

    @Test
    fun `an origin past the snapshot falls back to its last pixel`() {
        val snapshot = snapshotOf(hostWidth = 400, hostHeight = 800)

        val source = snapshotSourceRect(
            originInHost = IntOffset(x = 10_000, y = 10_000),
            destination = IntSize(width = 100, height = 100),
            snapshot = snapshot,
        )

        assertEquals(IntOffset(x = snapshot.width - 1, y = snapshot.height - 1), source.topLeft)
        assertEquals(IntSize(width = 1, height = 1), source.size)
    }

    @Test
    fun `a negative origin is pulled back to the snapshot corner`() {
        val source = snapshotSourceRect(
            originInHost = IntOffset(x = -120, y = -40),
            destination = IntSize(width = 100, height = 100),
            snapshot = snapshotOf(hostWidth = 400, hostHeight = 800),
        )

        assertEquals(IntOffset.Zero, source.topLeft)
    }

    @Test
    fun `a composable larger than what is left of the snapshot is cut short`() {
        val snapshot = snapshotOf(hostWidth = 400, hostHeight = 800)

        val source = snapshotSourceRect(
            originInHost = IntOffset(x = 360, y = 760),
            destination = IntSize(width = 400, height = 800),
            snapshot = snapshot,
        )

        assertEquals(IntOffset(x = 90, y = 190), source.topLeft)
        assertEquals(IntSize(width = 10, height = 10), source.size)
        assertEquals(snapshot.width, source.right)
        assertEquals(snapshot.height, source.bottom)
    }

    @Test
    fun `a stale one pixel snapshot still yields a drawable rect`() {
        val source = snapshotSourceRect(
            originInHost = IntOffset(x = 500, y = 500),
            destination = IntSize(width = 400, height = 400),
            snapshot = IntSize(width = 1, height = 1),
        )

        assertEquals(IntOffset.Zero, source.topLeft)
        assertEquals(IntSize(width = 1, height = 1), source.size)
    }

    private fun snapshotOf(hostWidth: Int, hostHeight: Int): IntSize = IntSize(
        width = hostWidth / WINDOW_CAPTURE_DOWN_SCALE,
        height = hostHeight / WINDOW_CAPTURE_DOWN_SCALE,
    )
}
