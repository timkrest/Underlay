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

/**
 * Which step the modifier reaches, end to end through the public API. Every branch here is decided
 * by the device: the API level, and whether the system has cross-window blur switched on at all.
 */
@RunWith(AndroidJUnit4::class)
class UnderlaySourceTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun anOverlayInsideTheActivityWindowReportsTheFallback() {
        val source = awaitSource(overlay = { content -> content() }) { it == UnderlaySource.Fallback }

        assertEquals(UnderlaySource.Fallback, source)
    }

    @Test
    fun aDialogNeverSettlesOnTheFallback() {
        awaitSource(overlay = { content -> Dialog(onDismissRequest = {}) { content() } }) { it.isBackdrop() }
    }

    @Test
    fun withoutSystemBlurADialogFallsThroughToTheSnapshot() {
        assumeTrue("the system blurs behind the window here", !isCrossWindowBlurEnabled())

        val source = awaitSource(overlay = { content -> Dialog(onDismissRequest = {}) { content() } }) { it.isBackdrop() }

        assertEquals(UnderlaySource.Snapshot, source)
    }

    @Test
    fun withSystemBlurADialogUsesTheWindowFlag() {
        assumeTrue("cross-window blur is off on this device", isCrossWindowBlurEnabled())

        val source = awaitSource(overlay = { content -> Dialog(onDismissRequest = {}) { content() } }) { it.isBackdrop() }

        assertEquals(UnderlaySource.SystemBlur, source)
    }

    /** A step that actually draws something behind the overlay, as opposed to Pending or Fallback. */
    private fun UnderlaySource.isBackdrop(): Boolean =
        this == UnderlaySource.SystemBlur || this == UnderlaySource.Snapshot

    private fun isCrossWindowBlurEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val windowManager = compose.activity.getSystemService<WindowManager>() ?: return false
        return windowManager.isCrossWindowBlurEnabled
    }

    /**
     * Waits for a reported step that [matches], in real time. A snapshot crosses a frame boundary,
     * a `PixelCopy` callback and a background blur before it lands, so the first report is never the
     * answer and the test clock has nothing to do with when it arrives.
     */
    private fun awaitSource(
        overlay: @Composable (content: @Composable () -> Unit) -> Unit,
        matches: (UnderlaySource) -> Boolean,
    ): UnderlaySource {
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
            throw AssertionError("no matching step within ${TIMEOUT_MILLIS}ms, reported $reported", neverMatched)
        }

        return reported.last()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
