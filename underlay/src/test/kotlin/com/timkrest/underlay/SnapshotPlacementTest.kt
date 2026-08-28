package com.timkrest.underlay

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SnapshotPlacementTest {

    @Test
    fun `a composable filling the host window maps onto the whole snapshot`() {
        val placement = placementIn(hostWidth = 400, hostHeight = 800, origin = IntOffset.Zero, size = IntSize(400, 800))

        assertEquals(IntRect(IntOffset.Zero, IntSize(width = 100, height = 200)), placement.source)
        assertEquals(IntRect(IntOffset.Zero, IntSize(width = 400, height = 800)), placement.destination)
    }

    @Test
    fun `an origin inside the host window scales down by the capture factor`() {
        val placement = placementIn(
            hostWidth = 400,
            hostHeight = 800,
            origin = IntOffset(x = 80, y = 40),
            size = IntSize(width = 200, height = 100),
        )

        assertEquals(IntRect(IntOffset(x = 20, y = 10), IntSize(width = 50, height = 25)), placement.source)
        assertEquals(IntRect(IntOffset.Zero, IntSize(width = 200, height = 100)), placement.destination)
    }

    @Test
    fun `an origin off the capture grid keeps every host pixel it covers`() {
        val placement = placementIn(
            hostWidth = 400,
            hostHeight = 400,
            origin = IntOffset(x = 3, y = 0),
            size = IntSize(width = 4, height = 4),
        )

        assertEquals(2, placement.source.width, "host pixels 3 to 7 span two snapshot pixels")
        assertEquals(IntOffset(x = -3, y = 0), placement.destination.topLeft, "the extra pixel is drawn where it was captured")
        assertEquals(placement.source.width * WINDOW_CAPTURE_DOWN_SCALE, placement.destination.width)
    }

    @Test
    fun `a composable hanging off the host window keeps the snapshot at its own scale`() {
        val placement = placementIn(
            hostWidth = 400,
            hostHeight = 800,
            origin = IntOffset(x = 360, y = 760),
            size = IntSize(width = 400, height = 800),
        )

        assertEquals(IntRect(IntOffset(x = 90, y = 190), IntSize(width = 10, height = 10)), placement.source)
        assertEquals(
            IntRect(IntOffset.Zero, IntSize(width = 40, height = 40)),
            placement.destination,
            "only the overlapping corner is covered, and at the capture scale",
        )
    }

    @Test
    fun `a negative origin leaves the part outside the host window uncovered`() {
        val placement = placementIn(
            hostWidth = 400,
            hostHeight = 800,
            origin = IntOffset(x = -120, y = -40),
            size = IntSize(width = 200, height = 200),
        )

        assertEquals(IntOffset.Zero, placement.source.topLeft)
        assertEquals(IntOffset(x = 120, y = 40), placement.destination.topLeft)
        assertEquals(IntSize(width = 80, height = 160), placement.destination.size)
    }

    @Test
    fun `a composable entirely off the snapshot draws nothing`() {
        val placement = snapshotPlacement(
            originInHost = IntOffset(x = 10_000, y = 10_000),
            size = IntSize(width = 100, height = 100),
            snapshot = snapshotOf(hostWidth = 400, hostHeight = 800),
        )

        assertNull(placement)
    }

    private fun placementIn(hostWidth: Int, hostHeight: Int, origin: IntOffset, size: IntSize): SnapshotPlacement =
        assertNotNull(
            snapshotPlacement(
                originInHost = origin,
                size = size,
                snapshot = snapshotOf(hostWidth, hostHeight),
            ),
            "the composable overlaps the host window",
        )

    private fun snapshotOf(hostWidth: Int, hostHeight: Int): IntSize = IntSize(
        width = hostWidth / WINDOW_CAPTURE_DOWN_SCALE,
        height = hostHeight / WINDOW_CAPTURE_DOWN_SCALE,
    )
}
