// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

@RunWith(AndroidJUnit4::class)
class LiveSnapshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var hostColor by mutableStateOf(Color.Red)
    private var isOverlayOpen by mutableStateOf(false)
    private val reported = CopyOnWriteArrayList<UnderlaySource>()

    @Volatile
    private var backdropOnScreen: Offset? = null

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
    fun aLiveSnapshotUnderAPopupFollowsTheHostWithoutARefresh() = assertTheSnapshotFollowsTheHost(popup)

    @Test
    fun aLiveSnapshotUnderADialogFollowsTheHostWithoutARefresh() = assertTheSnapshotFollowsTheHost(undimmedDialog)

    private fun assertTheSnapshotFollowsTheHost(overlay: Overlay) {
        compose.takeTheSystemBlurOutOfTheLadder()
        compose.setContent {
            Box(Modifier.fillMaxSize().background(hostColor))
            if (isOverlayOpen) overlay { Backdrop() }
        }
        isOverlayOpen = true
        awaitBackdropOf(Color.Red)

        hostColor = Color.Blue
        awaitBackdropOf(Color.Blue)

        hostColor = Color.Yellow
        awaitBackdropOf(Color.Yellow)
    }

    private fun awaitBackdropOf(host: Color) {
        val expected = host.tinted(TINT)
        compose.awaitOrFail({ "the backdrop at $backdropOnScreen never followed the $host host, reported $reported" }) {
            val probe = backdropOnScreen ?: return@awaitOrFail false
            screenshot().colorAt(probe).matches(expected)
        }
    }

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
                    liveSnapshot = true,
                    onSourceChange = { reported += it },
                ),
        )
    }

    private companion object {
        val TINT = Color.Green.copy(alpha = 0.5f)
        val BACKDROP_SIZE = 120.dp
    }
}
