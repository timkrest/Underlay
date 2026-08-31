package com.timkrest.underlay

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
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
    private var isOverlayOpen by mutableStateOf(false)
    private val underlay = UnderlayState()
    private val reported = mutableListOf<UnderlaySource>()

    private var backdropOnScreen = Offset.Unspecified
    private var hostOnScreen = Offset.Unspecified

    private val popup: Overlay = { content -> Popup { content() } }

    private val undimmedDialog: Overlay = { content ->
        Dialog(onDismissRequest = {}) {
            DropTheWindowDim()
            content()
        }
    }

    @Test
    fun aRefreshUnderAPopupBringsTheHostWindowBackFresh() = assertARefreshBringsTheHostWindowBack(popup)

    @Test
    fun aRefreshUnderADialogBringsTheHostWindowBackFresh() = assertARefreshBringsTheHostWindowBack(undimmedDialog)

    private fun assertARefreshBringsTheHostWindowBack(overlay: Overlay) {
        assumeTrue(
            "the system blurs behind the window here, no snapshot is taken",
            !compose.activity.isCrossWindowBlurEnabled(),
        )
        showHostAndOverlay(overlay)
        awaitHostOf(Color.Red)
        openTheOverlay()
        awaitTheFirstSnapshot()
        awaitBackdropOf(Color.Red.tinted())

        hostColor = Color.Blue
        assertTheHostTurnedBlueButItsSnapshotDidNot()

        compose.runOnUiThread { underlay.refresh() }

        awaitBackdropOf(Color.Blue.tinted())
    }

    private fun assertTheHostTurnedBlueButItsSnapshotDidNot() {
        lateinit var screen: Bitmap
        compose.awaitOrFail({ "the host at $hostOnScreen never turned blue on screen" }) {
            screen = screenshot()
            screen.colorAt(hostOnScreen).matches(Color.Blue)
        }

        assertTrue(
            screen.colorAt(backdropOnScreen).matches(Color.Red.tinted()),
            "the snapshot changed without a refresh: backdrop at $backdropOnScreen is " +
                "${screen.colorAt(backdropOnScreen)}, host at $hostOnScreen is ${screen.colorAt(hostOnScreen)}, " +
                "reported $reported",
        )
    }

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
            screenshot().colorAt(hostOnScreen).matches(color)
        }
    }

    private fun awaitBackdropOf(color: Color) {
        compose.awaitOrFail({ "no $color backdrop at $backdropOnScreen, was ${backdropColor()}" }) {
            backdropColor().matches(color)
        }
    }

    private fun backdropColor(): Color = screenshot().colorAt(backdropOnScreen)

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

    @Composable
    private fun DropTheWindowDim() {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window

        SideEffect { window?.setDimAmount(0f) }
    }

    private fun Color.tinted(): Color = Color(
        red = TINT.red * TINT.alpha + red * (1f - TINT.alpha),
        green = TINT.green * TINT.alpha + green * (1f - TINT.alpha),
        blue = TINT.blue * TINT.alpha + blue * (1f - TINT.alpha),
    )

    /** A round trip through the GPU is not always bit exact. */
    private fun Color.matches(other: Color): Boolean =
        abs(red - other.red) <= CHANNEL_TOLERANCE &&
            abs(green - other.green) <= CHANNEL_TOLERANCE &&
            abs(blue - other.blue) <= CHANNEL_TOLERANCE

    private companion object {
        const val CHANNEL_TOLERANCE = 8f / 255f
        val TINT = Color.Green.copy(alpha = 0.5f)
        val BACKDROP_SIZE = 120.dp
        val HOST_PROBE_INSET = 16.dp
        val HOST_PROBE_SIZE = 24.dp
    }
}
