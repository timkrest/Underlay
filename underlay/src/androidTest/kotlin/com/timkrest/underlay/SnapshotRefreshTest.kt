// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SnapshotRefreshTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val systemBlur = SystemBlurRule()

    private var hostColor by mutableStateOf(Color.Red)
    private var isOverlayOpen by mutableStateOf(false)
    private val underlay = UnderlayState()
    private val reported = CopyOnWriteArrayList<UnderlaySource>()

    @Volatile
    private var backdropOnScreen: Offset? = null

    @Volatile
    private var hostOnScreen: Offset? = null

    @Test
    fun aRefreshUnderAPopupBringsTheHostWindowBackFresh() = assertARefreshBringsTheHostWindowBack(inPopup)

    @Test
    fun aRefreshUnderADialogBringsTheHostWindowBackFresh() = assertARefreshBringsTheHostWindowBack(inUndimmedDialog)

    @Test
    fun aLiveSnapshotUnderAPopupFollowsTheHostWithoutARefresh() = assertALiveSnapshotFollowsTheHost(inPopup)

    @Test
    fun aLiveSnapshotUnderADialogFollowsTheHostWithoutARefresh() = assertALiveSnapshotFollowsTheHost(inUndimmedDialog)

    private fun assertARefreshBringsTheHostWindowBack(overlay: Overlay) {
        systemBlur.switchOff()
        showHostAndOverlay(overlay, liveSnapshot = false)
        awaitHostOf(Color.Red)
        openTheOverlay()
        awaitTheFirstSnapshot()
        awaitBackdropOf(Color.Red)

        hostColor = Color.Blue
        assertTheHostMovedOnWithout(itsSnapshot = Color.Red)

        compose.runOnUiThread { underlay.refresh() }

        awaitBackdropOf(Color.Blue)

        hostColor = Color.Yellow
        assertTheHostMovedOnWithout(itsSnapshot = Color.Blue)
    }

    private fun assertALiveSnapshotFollowsTheHost(overlay: Overlay) {
        systemBlur.switchOff()
        showHostAndOverlay(overlay, liveSnapshot = true)
        openTheOverlay()
        awaitBackdropOf(Color.Red)

        hostColor = Color.Blue
        awaitBackdropOf(Color.Blue)

        hostColor = Color.Yellow
        awaitBackdropOf(Color.Yellow)
    }

    private fun assertTheHostMovedOnWithout(itsSnapshot: Color) {
        val frozen = itsSnapshot.tinted(TINT)
        compose.awaitOrFail({ "no $frozen backdrop at ${backdropProbe()} with a $hostColor host, ${state()}" }) {
            val screen = screenshot()
            screen.colorAt(hostProbe()).matches(hostColor) && screen.colorAt(backdropProbe()).matches(frozen)
        }

        repeat(FROZEN_FRAMES) {
            SystemClock.sleep(FRAME_GAP_MILLIS)
            assertTrue(
                screenshot().colorAt(backdropProbe()).matches(frozen),
                "the snapshot followed the host without a refresh: ${state()}",
            )
        }
    }

    private fun state(): String {
        val screen = screenshot()

        return "backdrop at ${backdropProbe()} is ${screen.colorAt(backdropProbe())}, " +
            "host at ${hostProbe()} is ${screen.colorAt(hostProbe())}, reported $reported"
    }

    private fun hostProbe(): Offset = hostOnScreen ?: error("the host probe was never positioned")

    private fun backdropProbe(): Offset = backdropOnScreen ?: error("the backdrop was never positioned")

    private fun showHostAndOverlay(overlay: Overlay, liveSnapshot: Boolean) {
        compose.setContent {
            Box(Modifier.fillMaxSize().background(hostColor)) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = HOST_PROBE_INSET)
                        .size(HOST_PROBE_SIZE)
                        .reportCenterOnScreen { hostOnScreen = it },
                )
            }

            if (isOverlayOpen) overlay { Backdrop(liveSnapshot) }
        }
    }

    private fun openTheOverlay() {
        isOverlayOpen = true
        compose.waitForIdle()
    }

    private fun awaitTheFirstSnapshot() {
        compose.awaitOrFail({ "no snapshot, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.Snapshot
        }
    }

    private fun awaitHostOf(color: Color) {
        compose.awaitOrFail({ "no $color host at $hostOnScreen" }) {
            screenshot().colorAt(hostProbe()).matches(color)
        }
    }

    private fun awaitBackdropOf(host: Color) {
        val expected = host.tinted(TINT)
        compose.awaitOrFail({ "no $expected backdrop over the $host host, ${state()}" }) {
            val probe = backdropOnScreen ?: return@awaitOrFail false
            screenshot().colorAt(probe).matches(expected)
        }
    }

    @Composable
    private fun Backdrop(liveSnapshot: Boolean) {
        Box(
            Modifier
                .size(BACKDROP_SIZE)
                .reportCenterOnScreen { backdropOnScreen = it }
                .background(BEHIND_THE_BACKDROP)
                .blurredUnderlay(
                    blurRadius = 0.dp,
                    tint = TINT,
                    fallback = Color.Green,
                    liveSnapshot = liveSnapshot,
                    state = underlay,
                    onSourceChange = { reported += it },
                ),
        )
    }

    private companion object {
        const val FROZEN_FRAMES = 3
        const val FRAME_GAP_MILLIS = 100L
        val HOST_PROBE_INSET = 16.dp
        val HOST_PROBE_SIZE = 24.dp
    }
}
