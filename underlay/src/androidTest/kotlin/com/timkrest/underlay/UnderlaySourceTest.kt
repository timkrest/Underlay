package com.timkrest.underlay

import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Which source wins is decided by the device, not the API level - hence the assumptions. */
@RunWith(AndroidJUnit4::class)
class UnderlaySourceTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun anOverlayInsideTheActivityWindowReportsTheFallback() {
        val reported = awaitSource(overlay = { content -> content() }) { it == UnderlaySource.Fallback }

        assertEquals(UnderlaySource.Fallback, reported.last())
    }

    @Test
    fun aDialogNeverReportsTheFallback() {
        val reported = awaitSource(overlay = { content -> Dialog(onDismissRequest = {}) { content() } }) {
            it.isBackdrop()
        }

        assertFalse(UnderlaySource.Fallback in reported, "a dialog has a backdrop from the first report on")
    }

    @Test
    fun withoutSystemBlurADialogFallsThroughToTheSnapshot() {
        assumeTrue("the system blurs behind the window here", !isCrossWindowBlurEnabled())

        val reported = awaitSource(overlay = { content -> Dialog(onDismissRequest = {}) { content() } }) {
            it.isBackdrop()
        }

        assertEquals(UnderlaySource.Snapshot, reported.last())
    }

    @Test
    fun withSystemBlurADialogUsesTheWindowFlag() {
        assumeTrue("cross-window blur is off on this device", isCrossWindowBlurEnabled())

        val reported = awaitSource(overlay = { content -> Dialog(onDismissRequest = {}) { content() } }) {
            it.isBackdrop()
        }

        assertEquals(UnderlaySource.SystemBlur, reported.last())
    }

    private fun UnderlaySource.isBackdrop(): Boolean =
        this == UnderlaySource.SystemBlur || this == UnderlaySource.Snapshot

    private fun isCrossWindowBlurEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val windowManager = compose.activity.getSystemService<WindowManager>() ?: return false
        return windowManager.isCrossWindowBlurEnabled
    }

    /**
     * Collects every reported source until one [matches]. Real time, not the test clock: a snapshot
     * waits for a frame boundary, a `PixelCopy` callback and a background blur before it lands, so
     * the first report is never the answer.
     */
    private fun awaitSource(
        overlay: @Composable (content: @Composable () -> Unit) -> Unit,
        matches: (UnderlaySource) -> Boolean,
    ): List<UnderlaySource> {
        val reported = mutableListOf<UnderlaySource>()

        compose.setContent {
            overlay {
                Box(
                    Modifier
                        .fillMaxSize()
                        .blurredUnderlay(
                            blurRadius = 16.dp,
                            tint = Color.Black.copy(alpha = 0.3f),
                            fallback = Color.DarkGray,
                            onSourceChange = { reported += it },
                        ),
                )
            }
        }

        try {
            compose.waitUntil(TIMEOUT_MILLIS) { reported.lastOrNull()?.let(matches) == true }
        } catch (neverMatched: ComposeTimeoutException) {
            throw AssertionError("no matching source within ${TIMEOUT_MILLIS}ms, reported $reported", neverMatched)
        }

        return reported
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
