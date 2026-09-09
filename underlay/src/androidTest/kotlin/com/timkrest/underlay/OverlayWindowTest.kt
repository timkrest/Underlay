// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class OverlayWindowTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun theFlagHasToExist() {
        assumeTrue("FLAG_BLUR_BEHIND arrives in API 31", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }

    @Test
    fun aDialogTakesTheFlagOnItsOwnWindow() {
        var dialogWindow: Window? = null
        compose.setContent {
            Dialog(onDismissRequest = {}) {
                dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
                Box(Modifier.size(OVERLAY_SIZE))
            }
        }
        compose.waitForIdle()
        val window = assertNotNull(dialogWindow, "a dialog owns a window")

        assertBlurBehindTravels(OverlayWindow.OfWindow(window)) { window.attributes }
    }

    @Test
    fun aPopupTakesTheFlagThroughTheWindowManager() {
        var popupRootView: View? = null
        compose.setContent {
            Popup {
                popupRootView = LocalView.current.rootView
                Box(Modifier.size(OVERLAY_SIZE))
            }
        }
        compose.waitForIdle()
        val rootView = assertNotNull(popupRootView, "a popup owns a root view")
        val windowManager = assertNotNull(compose.activity.getSystemService<WindowManager>(), "no window manager")

        assertBlurBehindTravels(OverlayWindow.OfRootView(rootView, windowManager)) {
            rootView.layoutParams as WindowManager.LayoutParams
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun assertBlurBehindTravels(overlay: OverlayWindow, attributes: () -> WindowManager.LayoutParams) {
        assertTrue(compose.runOnUiThread { overlay.takesBlurBehind(RADIUS_PX) }, "the window refused the flag")
        compose.waitForIdle()

        assertTrue(attributes().hasBlurBehind(), "the flag never reached the window")
        assertEquals(RADIUS_PX, attributes().blurBehindRadius)

        assertTrue(compose.runOnUiThread { overlay.dropsBlurBehind() }, "the window refused to drop the flag")
        compose.waitForIdle()

        assertFalse(attributes().hasBlurBehind(), "the flag stayed on the window")
        assertEquals(0, attributes().blurBehindRadius)
    }

    private fun WindowManager.LayoutParams.hasBlurBehind(): Boolean =
        flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND != 0

    private companion object {
        const val RADIUS_PX = 24
        val OVERLAY_SIZE = 64.dp
    }
}
