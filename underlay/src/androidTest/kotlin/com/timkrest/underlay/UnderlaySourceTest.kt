// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Which source wins is decided by the device, not the API level - hence the assumptions. */
@RunWith(AndroidJUnit4::class)
class UnderlaySourceTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val inTheActivityWindow: Overlay = { content -> content() }
    private val dialog: Overlay = { content -> Dialog(onDismissRequest = {}) { content() } }
    private val popup: Overlay = { content -> Popup { content() } }

    @After
    fun letTheSystemBlurAgain() = compose.restoreTheSystemBlur()

    @Test
    fun anOverlayInsideTheActivityWindowReportsTheFallback() {
        val reported = awaitSource(inTheActivityWindow) { it == UnderlaySource.Fallback }

        assertEquals(UnderlaySource.Fallback, reported.last())
    }

    @Test
    fun aDialogNeverReportsTheFallback() = assertABackdropFromTheFirstReport(dialog)

    @Test
    fun aPopupNeverReportsTheFallback() = assertABackdropFromTheFirstReport(popup)

    @Test
    fun withoutSystemBlurADialogFallsThroughToTheSnapshot() = assertTheSnapshotWins(dialog)

    @Test
    fun withoutSystemBlurAPopupFallsThroughToTheSnapshot() = assertTheSnapshotWins(popup)

    @Test
    fun aDialogFallsThroughToTheSnapshotWhenTheSystemStopsBlurring() =
        assertTheSnapshotTakesOverFrom(dialog)

    @Test
    fun aPopupFallsThroughToTheSnapshotWhenTheSystemStopsBlurring() =
        assertTheSnapshotTakesOverFrom(popup)

    @Test
    fun withSystemBlurADialogUsesTheWindowFlag() = assertTheWindowFlagWins(dialog)

    @Test
    fun withSystemBlurAPopupUsesTheWindowFlag() = assertTheWindowFlagWins(popup)

    @Test
    fun theStateFollowsTheOverlayItIsPassedTo() {
        val state = UnderlayState()
        val isOpen = mutableStateOf(true)
        val reported = showUnderlay({ content -> if (isOpen.value) dialog(content) }, state)

        compose.awaitOrFail({ "no backdrop, reported $reported" }) { state.source?.isBackdrop() == true }
        assertEquals(reported.last(), state.source)

        compose.runOnIdle { isOpen.value = false }

        compose.awaitOrFail({ "the state kept ${state.source} after the overlay closed" }) { state.source == null }
    }

    private fun assertABackdropFromTheFirstReport(overlay: Overlay) {
        val reported = awaitSource(overlay) { it.isBackdrop() }

        assertFalse(UnderlaySource.Fallback in reported, "an overlay in its own window never falls back")
    }

    private fun assertTheSnapshotWins(overlay: Overlay) {
        compose.takeTheSystemBlurOutOfTheLadder()

        val reported = awaitSource(overlay) { it.isBackdrop() }

        assertEquals(UnderlaySource.Snapshot, reported.last())
    }

    private fun assertTheWindowFlagWins(overlay: Overlay) {
        assumeTrue("cross-window blur is off on this device", isCrossWindowBlurEnabled())

        val reported = awaitSource(overlay) { it.isBackdrop() }

        assertEquals(UnderlaySource.SystemBlur, reported.last())
    }

    private fun assertTheSnapshotTakesOverFrom(overlay: Overlay) {
        assumeTrue("cross-window blur is off on this device", isCrossWindowBlurEnabled())

        val reported = showUnderlay(overlay)
        compose.awaitOrFail({ "no window flag, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.SystemBlur
        }

        compose.takeTheSystemBlurOutOfTheLadder()

        compose.awaitOrFail({ "the ladder never reached the snapshot, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.Snapshot
        }
    }

    private fun UnderlaySource.isBackdrop(): Boolean =
        this == UnderlaySource.SystemBlur || this == UnderlaySource.Snapshot

    private fun awaitSource(overlay: Overlay, matches: (UnderlaySource) -> Boolean): List<UnderlaySource> {
        val reported = showUnderlay(overlay)

        compose.awaitOrFail({ "no matching source, reported $reported" }) {
            reported.lastOrNull()?.let(matches) == true
        }

        return reported.toList()
    }

    private fun showUnderlay(overlay: Overlay, state: UnderlayState? = null): CopyOnWriteArrayList<UnderlaySource> {
        val reported = CopyOnWriteArrayList<UnderlaySource>()

        compose.setContent {
            overlay {
                Box(
                    Modifier
                        .fillMaxSize()
                        .blurredUnderlay(
                            blurRadius = 16.dp,
                            tint = Color.Black.copy(alpha = 0.3f),
                            fallback = Color.DarkGray,
                            state = state,
                            onSourceChange = { reported += it },
                        ),
                )
            }
        }

        return reported
    }
}
