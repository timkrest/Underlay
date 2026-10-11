// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.backdrop

import org.junit.Test
import kotlin.test.assertEquals

class BlurRadiusTest {

    @Test
    fun `a radius that is not a blur means no blur`() {
        for (px in listOf(-8f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(BlurRadius.None, BlurRadius.ofPx(px), "$px")
        }
    }
}
