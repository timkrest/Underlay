package com.timkrest.underlay

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Which windows the modifier resolves from each place it can be called. No window of its own means
 * nothing to blur; no reachable host means nothing to snapshot.
 */
@RunWith(AndroidJUnit4::class)
class UnderlayWindowsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun anOverlayInsideTheActivityWindowOwnsNoWindowAndHasNoHost() {
        lateinit var windows: UnderlayWindows
        compose.setContent { windows = rememberUnderlayWindows() }
        compose.waitForIdle()

        assertNull(windows.overlay, "an in-window overlay must not claim a window of its own")
        assertNull(windows.host, "capturing the caller's own window would fold the overlay into its backdrop")
    }

    @Test
    fun aDialogOwnsItsWindowAndSeesTheActivityUnderneath() {
        lateinit var windows: UnderlayWindows
        compose.setContent {
            Dialog(onDismissRequest = {}) { windows = rememberUnderlayWindows() }
        }
        compose.waitForIdle()

        assertTrue(windows.overlay is OverlayWindow.OfWindow, "a dialog owns a window, got ${windows.overlay}")
        assertSame(compose.activity.window, windows.host)
    }

    @Test
    fun aPopupOwnsItsRootViewAndSeesTheActivityUnderneath() {
        lateinit var windows: UnderlayWindows
        compose.setContent {
            Popup { windows = rememberUnderlayWindows() }
        }
        compose.waitForIdle()

        assertTrue(
            windows.overlay is OverlayWindow.OfRootView,
            "a popup is added through WindowManager, got ${windows.overlay}",
        )
        assertSame(compose.activity.window, windows.host)
    }
}
