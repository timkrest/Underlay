// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.graphics.Bitmap
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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SnapshotRefreshTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var hostColor by mutableStateOf(Color.Red)
    private var isOverlayOpen by mutableStateOf(false)
    private val underlay = UnderlayState()
    private val reported = CopyOnWriteArrayList<UnderlaySource>()

    @Volatile
    private var backdropOnScreen: Offset? = null

    @Volatile
    private var hostOnScreen: Offset? = null

    private val popup: Overlay = { content -> Popup { content() } }

    private val undimmedDialog: Overlay = { content ->
        Dialog(onDismissRequest = {}) {
            DropTheWindowDim()
            content()
        }
    }

    @After
    fun letTheSystemBlurAgain() = compose.restoreTheSystemBlur()

    @Test
    fun aRefreshUnderAPopupBringsTheHostWindowBackFresh() = assertARefreshBringsTheHostWindowBack(popup)

    @Test
    fun aRefreshUnderADialogBringsTheHostWindowBackFresh() = assertARefreshBringsTheHostWindowBack(undimmedDialog)

    private fun assertARefreshBringsTheHostWindowBack(overlay: Overlay) {
        compose.takeTheSystemBlurOutOfTheLadder()
        showHostAndOverlay(overlay)
        awaitHostOf(Color.Red)
        openTheOverlay()
        awaitTheFirstSnapshot()
        awaitBackdropOf(Color.Red.tinted(TINT))

        hostColor = Color.Blue
        assertTheHostMovedOnWithout(itsSnapshot = Color.Red)

        compose.runOnUiThread { underlay.refresh() }

        awaitBackdropOf(Color.Blue.tinted(TINT))

        hostColor = Color.Yellow
        assertTheHostMovedOnWithout(itsSnapshot = Color.Blue)
    }

    private fun assertTheHostMovedOnWithout(itsSnapshot: Color) {
        lateinit var screen: Bitmap
        compose.awaitOrFail({ "the host at ${hostProbe()} never turned $hostColor on screen" }) {
            screen = screenshot()
            screen.colorAt(hostProbe()).matches(hostColor)
        }

        assertTrue(
            screen.colorAt(backdropProbe()).matches(itsSnapshot.tinted(TINT)),
            "the snapshot followed the host without a refresh: backdrop at ${backdropProbe()} is " +
                "${screen.colorAt(backdropProbe())}, host at ${hostProbe()} is ${screen.colorAt(hostProbe())}, " +
                "reported $reported",
        )
    }

    private fun hostProbe(): Offset = hostOnScreen ?: error("the host probe was never positioned")

    private fun backdropProbe(): Offset = backdropOnScreen ?: error("the backdrop was never positioned")

    private fun showHostAndOverlay(overlay: Overlay) {
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

            if (isOverlayOpen) overlay { Backdrop() }
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

    private fun awaitBackdropOf(color: Color) {
        compose.awaitOrFail({ "no $color backdrop at $backdropOnScreen, was ${backdropColor()}" }) {
            backdropColor().matches(color)
        }
    }

    private fun backdropColor(): Color = screenshot().colorAt(backdropProbe())

    @Composable
    private fun Backdrop() {
        Box(
            Modifier
                .size(BACKDROP_SIZE)
                .reportCenterOnScreen { backdropOnScreen = it }
                .blurredUnderlay(
                    blurRadius = 0.dp,
                    tint = TINT,
                    fallback = Color.Green,
                    state = underlay,
                    onSourceChange = { reported += it },
                ),
        )
    }

    private companion object {
        val TINT = Color.Green.copy(alpha = 0.5f)
        val BACKDROP_SIZE = 120.dp
        val HOST_PROBE_INSET = 16.dp
        val HOST_PROBE_SIZE = 24.dp
    }
}
