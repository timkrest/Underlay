package com.timkrest.underlay

import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class SnapshotRefreshTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var hostColor by mutableStateOf(Color.Red)
    private val underlay = UnderlayState()

    @Test
    fun aRefreshBringsTheHostWindowBackFresh() {
        assumeTrue("reading a dialog window back needs PixelCopy", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        assumeTrue("the system blurs behind the window here, no snapshot is taken", !isCrossWindowBlurEnabled())
        showDialogOverHost()
        awaitBackdropOf(Color.Red)

        hostColor = Color.Blue
        compose.waitForIdle()
        compose.runOnUiThread { underlay.refresh() }

        awaitBackdropOf(Color.Blue)
    }

    private fun showDialogOverHost() {
        compose.setContent {
            Box(Modifier.fillMaxSize().background(hostColor))

            Dialog(onDismissRequest = {}) {
                Box(
                    Modifier
                        .size(BACKDROP_SIZE)
                        .testTag(BACKDROP_TAG)
                        .blurredUnderlay(
                            blurRadius = 0.dp,
                            tint = Color.Transparent,
                            fallback = Color.Green,
                            state = underlay,
                        ),
                )
            }
        }
        compose.waitForIdle()
    }

    /** Real time, not the test clock: a capture waits for a frame boundary and a `PixelCopy`. */
    private fun awaitBackdropOf(color: Color) {
        try {
            compose.waitUntil(TIMEOUT_MILLIS) { backdropPixel().matches(color) }
        } catch (neverLanded: ComposeTimeoutException) {
            throw AssertionError("no $color backdrop within ${TIMEOUT_MILLIS}ms, was ${backdropPixel()}", neverLanded)
        }
    }

    private fun backdropPixel(): Color {
        val backdrop = compose.onNodeWithTag(BACKDROP_TAG).captureToImage()
        return backdrop.pixelAt(backdrop.width / 2, backdrop.height / 2)
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
        const val BACKDROP_TAG = "backdrop"
        val BACKDROP_SIZE = 120.dp
    }
}
