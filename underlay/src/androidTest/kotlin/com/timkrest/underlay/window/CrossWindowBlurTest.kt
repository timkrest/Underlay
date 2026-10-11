// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.timkrest.underlay.backdrop.SystemBlurBehind
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
class CrossWindowBlurTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun theFlagHasToExist() {
        assumeTrue("FLAG_BLUR_BEHIND arrives in API 31", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }

    @Test
    fun withSystemBlurOffTheWindowKeepsTheBlurItSetItself() {
        val window = showDialog()
        val overlay = OverlayWindow.OfWindow(window)
        val systemBlur = crossWindowBlur(overlay, isEnabled = false)

        compose.runOnUiThread {
            overlay.setBlurBehind(OWN_BLUR)
            assertFalse(systemBlur.request(RADIUS_PX), "system blur is off")
            systemBlur.withdraw()
            systemBlur.release()
        }

        assertEquals(OWN_BLUR, compose.runOnUiThread { overlay.blurBehind })
    }

    @Test
    fun withdrawingGivesBackTheBlurTheWindowHadWhenTheFlagWentOn() {
        val window = showDialog()
        val overlay = OverlayWindow.OfWindow(window)
        val systemBlur = crossWindowBlur(overlay, isEnabled = true)

        compose.runOnUiThread {
            overlay.setBlurBehind(OWN_BLUR)
            assertTrue(systemBlur.request(RADIUS_PX), "the window refused the flag")
        }
        assertEquals(BlurBehind.on(RADIUS_PX), compose.runOnUiThread { overlay.blurBehind })

        compose.runOnUiThread { systemBlur.release() }

        assertEquals(OWN_BLUR, compose.runOnUiThread { overlay.blurBehind })
    }

    private fun showDialog(): Window {
        var dialogWindow: Window? = null
        compose.setContent {
            Dialog(onDismissRequest = {}) {
                dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
                Box(Modifier.size(OVERLAY_SIZE))
            }
        }
        compose.waitForIdle()
        return assertNotNull(dialogWindow, "a dialog owns a window")
    }

    private fun crossWindowBlur(overlay: OverlayWindow, isEnabled: Boolean): SystemBlurBehind {
        val factory = assertNotNull(crossWindowBlurOrNull(overlay, windowManager(isEnabled)), "API 31 has cross-window blur")
        return compose.runOnUiThread { factory {} }
    }

    private fun windowManager(isEnabled: Boolean): WindowManager {
        val real = assertNotNull(compose.activity.getSystemService<WindowManager>(), "no window manager")
        return object : WindowManager by real {
            override fun isCrossWindowBlurEnabled(): Boolean = isEnabled
        }
    }

    private companion object {
        const val RADIUS_PX = 24
        val OWN_BLUR = BlurBehind.on(29)
        val OVERLAY_SIZE = 64.dp
    }
}
