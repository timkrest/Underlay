// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.backdrop

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SnapshotFadeInTest {

    @Test
    fun `a fade runs from clear to whole over its duration`() {
        val fadeIn = SnapshotFadeIn(startedAtMillis = 1_000L, durationMillis = 200L)

        assertEquals(0f, fadeIn.alphaAt(1_000L))
        assertEquals(0.5f, fadeIn.alphaAt(1_100L))
        assertEquals(1f, fadeIn.alphaAt(1_200L))
        assertEquals(1f, fadeIn.alphaAt(5_000L))
    }

    @Test
    fun `a fade of no length is whole at once`() {
        assertEquals(1f, SnapshotFadeIn.Done.alphaAt(0L))
        assertFalse(SnapshotFadeIn.Done.isRunningAt(0L))
    }
}
