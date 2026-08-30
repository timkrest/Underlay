package com.timkrest.underlay

import android.os.Build
import android.view.WindowManager
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SnapshotRefreshTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var hostColor by mutableStateOf(Color.Red)
    private val underlay = UnderlayState()
    private val reported = mutableListOf<UnderlaySource>()

    /** A popup is read off the screen, so this covers every level where a snapshot is read back. */
    @Test
    fun aRefreshUnderAPopupBringsTheHostWindowBackFresh() {
        assumeASnapshotIsRead()

        assertARefreshBringsTheHostWindowBack { content -> Popup { content() } }
    }

    /** A dialog window is read through `PixelCopy` instead, which the test API does only from 28. */
    @Test
    fun aRefreshUnderADialogBringsTheHostWindowBackFresh() {
        assumeASnapshotIsRead()
        assumeTrue("reading a dialog window back needs API 28", Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)

        assertARefreshBringsTheHostWindowBack { content -> Dialog(onDismissRequest = {}) { content() } }
    }

    private fun assertARefreshBringsTheHostWindowBack(
        overlay: @Composable (content: @Composable () -> Unit) -> Unit,
    ) {
        showOverHost(overlay)
        awaitTheFirstSnapshot()
        awaitPixelOf(BACKDROP_TAG, Color.Red)

        hostColor = Color.Blue
        compose.waitForIdle()
        awaitPixelOf(HOST_TAG, Color.Blue)
        // A popup is read off the screen, where an overlay that drew nothing would show the host
        // itself. Only a backdrop still holding the old color proves the snapshot is what is drawn.
        assertTrue(pixelOf(BACKDROP_TAG).matches(Color.Red), "the snapshot changed without a refresh")

        compose.runOnUiThread { underlay.refresh() }

        awaitPixelOf(BACKDROP_TAG, Color.Blue)
    }

    private fun assumeASnapshotIsRead() {
        assumeTrue("reading an overlay back needs API 26", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        assumeTrue("the system blurs behind the window here, no snapshot is taken", !isCrossWindowBlurEnabled())
    }

    private fun showOverHost(overlay: @Composable (content: @Composable () -> Unit) -> Unit) {
        compose.setContent {
            Box(Modifier.fillMaxSize().testTag(HOST_TAG).background(hostColor))

            overlay {
                Box(
                    Modifier
                        .size(BACKDROP_SIZE)
                        .testTag(BACKDROP_TAG)
                        .blurredUnderlay(
                            blurRadius = 0.dp,
                            tint = Color.Transparent,
                            fallback = Color.Green,
                            state = underlay,
                            onSourceChange = { reported += it },
                        ),
                )
            }
        }
        compose.waitForIdle()
    }

    /**
     * A pixel cannot tell a snapshot of the red host from the red host itself showing through, so
     * without this the host would go blue while the first capture is still on its way and land blue.
     */
    private fun awaitTheFirstSnapshot() {
        try {
            compose.waitUntil(TIMEOUT_MILLIS) { reported.lastOrNull() == UnderlaySource.Snapshot }
        } catch (neverLanded: ComposeTimeoutException) {
            throw AssertionError("no snapshot within ${TIMEOUT_MILLIS}ms, reported $reported", neverLanded)
        }
    }

    /** Real time, not the test clock: a capture waits for a frame boundary and a `PixelCopy`. */
    private fun awaitPixelOf(tag: String, color: Color) {
        try {
            compose.waitUntil(TIMEOUT_MILLIS) { pixelOf(tag).matches(color) }
        } catch (neverLanded: ComposeTimeoutException) {
            throw AssertionError("no $color at $tag within ${TIMEOUT_MILLIS}ms, was ${pixelOf(tag)}", neverLanded)
        }
    }

    private fun pixelOf(tag: String): Color {
        val image = compose.onNodeWithTag(tag).captureToImage()
        return image.pixelAt(image.width / 2, image.height / 2)
    }

    private fun isCrossWindowBlurEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return compose.activity.getSystemService<WindowManager>()?.isCrossWindowBlurEnabled == true
    }

    /** A round trip through the GPU is not always bit exact. */
    private fun Color.matches(other: Color): Boolean =
        abs(red - other.red) <= CHANNEL_TOLERANCE &&
            abs(green - other.green) <= CHANNEL_TOLERANCE &&
            abs(blue - other.blue) <= CHANNEL_TOLERANCE

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val CHANNEL_TOLERANCE = 8f / 255f
        const val HOST_TAG = "host"
        const val BACKDROP_TAG = "backdrop"
        val BACKDROP_SIZE = 120.dp
    }
}
