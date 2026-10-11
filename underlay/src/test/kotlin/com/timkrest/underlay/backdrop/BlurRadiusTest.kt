// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.backdrop

import org.junit.Test
import kotlin.test.assertEquals

class BlurRadiusTest {

    @Test
    fun `the snapshot gets the platform sigma of the radius, at the capture scale`() {
        assertEquals((0.57735f * 16f + 0.5f) / 4f, BlurRadius.ofPx(16f).snapshotSigma, absoluteTolerance = 1e-5f)
        assertEquals(16, BlurRadius.ofPx(15.6f).windowPx)
    }

    @Test
    fun `a radius that is not a blur means no blur`() {
        for (px in listOf(0f, -8f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(0f, BlurRadius.ofPx(px).snapshotSigma, "$px")
        }
    }
}
