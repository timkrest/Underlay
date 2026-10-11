// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class UnderlaySourceTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val systemBlur = SystemBlurRule()

    private val inTheActivityWindow: Overlay = { content -> content() }
    private val alertDialog: Overlay = { content -> BasicAlertDialog(onDismissRequest = {}) { content() } }
    private val bottomSheet: Overlay = { content -> ModalBottomSheet(onDismissRequest = {}) { content() } }
    private val dropdownMenu: Overlay = { content -> DropdownMenu(expanded = true, onDismissRequest = {}) { content() } }

    @Test
    fun anOverlayInsideTheActivityWindowReportsOnlyTheFallback() {
        val reported = showUnderlay(inTheActivityWindow)
        compose.waitForIdle()

        assertEquals(listOf(UnderlaySource.Fallback), reported)
    }

    @Test
    fun aDialogNeverReportsTheFallback() = assertABackdropFromTheFirstReport(inDialog)

    @Test
    fun aPopupNeverReportsTheFallback() = assertABackdropFromTheFirstReport(inPopup)

    @Test
    fun aMaterialAlertDialogNeverReportsTheFallback() = assertABackdropFromTheFirstReport(alertDialog)

    @Test
    fun aModalBottomSheetNeverReportsTheFallback() = assertABackdropFromTheFirstReport(bottomSheet)

    @Test
    fun aDropdownMenuNeverReportsTheFallback() = assertABackdropFromTheFirstReport(dropdownMenu)

    @Test
    fun withoutSystemBlurAModalBottomSheetFallsThroughToTheSnapshot() = assertTheSnapshotWins(bottomSheet)

    @Test
    fun withoutSystemBlurADialogFallsThroughToTheSnapshot() = assertTheSnapshotWins(inDialog)

    @Test
    fun withoutSystemBlurAPopupFallsThroughToTheSnapshot() = assertTheSnapshotWins(inPopup)

    @Test
    fun aDialogFallsThroughToTheSnapshotWhenTheSystemStopsBlurring() =
        assertTheSnapshotTakesOverFrom(inDialog)

    @Test
    fun aPopupFallsThroughToTheSnapshotWhenTheSystemStopsBlurring() =
        assertTheSnapshotTakesOverFrom(inPopup)

    @Test
    fun withSystemBlurADialogUsesTheWindowFlag() = assertTheWindowFlagWins(inDialog)

    @Test
    fun withSystemBlurAPopupUsesTheWindowFlag() = assertTheWindowFlagWins(inPopup)

    @Test
    fun theStateFollowsTheOverlayItIsPassedTo() {
        val state = UnderlayState()
        val isOpen = mutableStateOf(true)
        val reported = showUnderlay({ content -> if (isOpen.value) inDialog(content) }, state)

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
        systemBlur.switchOff()

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

        systemBlur.switchOff()

        compose.awaitOrFail({ "the ladder never reached the snapshot, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.Snapshot
        }
    }

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
