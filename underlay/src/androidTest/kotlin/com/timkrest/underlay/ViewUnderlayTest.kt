// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.doOnAttach
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ViewUnderlayTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val systemBlur = SystemBlurRule()

    private val reported = CopyOnWriteArrayList<UnderlaySource>()
    private var dismiss: () -> Unit = {}
    private lateinit var underlay: ViewUnderlay

    @After
    fun closeTheOverlay() {
        compose.runOnUiThread { dismiss() }
    }

    @Test
    fun aViewDialogNeverReportsTheFallback() {
        showInDialog()

        awaitBackdrop()
        assertFalse(UnderlaySource.Fallback in reported)
    }

    @Test
    fun aPopupWindowNeverReportsTheFallback() {
        showInPopupWindow()

        awaitBackdrop()
        assertFalse(UnderlaySource.Fallback in reported)
    }

    @Test
    fun withoutSystemBlurAViewDialogDrawsTheHostBehindIt() {
        systemBlur.switchOff()

        val content = showInDialog()

        compose.awaitOrFail({ "no host behind the dialog, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.Snapshot &&
                screenshot().colorAt(content.centerOnScreen()).matches(HOST.tinted(TINT))
        }
    }

    @Test
    fun aSecureHostIsNotShownInAnOverlayThatIsNot() {
        systemBlur.switchOff()
        compose.runOnUiThread { compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }

        showInDialog()

        compose.awaitOrFail({ "the secure host was not refused, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.Fallback
        }
        assertFalse(UnderlaySource.Snapshot in reported)
    }

    @Test
    fun aHostThatTurnsSecureTakesTheSnapshotAway() {
        systemBlur.switchOff()
        showInDialog()
        compose.awaitOrFail({ "no snapshot to take away, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.Snapshot
        }

        compose.runOnUiThread { compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }

        compose.awaitOrFail({ "the snapshot outlived FLAG_SECURE, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.Fallback
        }
    }

    @Test
    fun aNewTintShowsOverTheSameSnapshot() {
        systemBlur.switchOff()
        val content = showInDialog()
        compose.awaitOrFail({ "no snapshot to tint, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.Snapshot
        }

        compose.runOnUiThread { underlay.tint = OTHER_TINT.toArgb() }

        compose.awaitOrFail({ "the new tint never showed" }) {
            screenshot().colorAt(content.centerOnScreen()).matches(HOST.tinted(OTHER_TINT))
        }
    }

    @Test
    fun theUnderlayKeepsTheMinimumSizeOfThePreviousBackground() {
        val view = compose.runOnUiThread {
            View(compose.activity).apply {
                background = GradientDrawable().apply { setSize(MINIMUM_WIDTH_PX, MINIMUM_HEIGHT_PX) }
                installUnderlay(this)
                measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            }
        }

        assertEquals(MINIMUM_WIDTH_PX, view.measuredWidth)
        assertEquals(MINIMUM_HEIGHT_PX, view.measuredHeight)
    }

    @Test
    fun removingTheUnderlayGivesTheWindowItsOwnBlurBack() {
        assumeTrue("the emulator has no cross-window blur", isCrossWindowBlurEnabled())
        var window: Window? = null
        showInDialog(configureWindow = { dialogWindow ->
            window = dialogWindow
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                dialogWindow.attributes = dialogWindow.attributes.apply { blurBehindRadius = OWN_BLUR_RADIUS_PX }
            }
        })
        compose.awaitOrFail({ "no system blur to remove, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.SystemBlur
        }

        compose.runOnUiThread { underlay.remove() }

        val attributes = compose.runOnUiThread { checkNotNull(window).attributes }
        assertTrue(attributes.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND != 0, "the window lost its own blur")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) assertEquals(OWN_BLUR_RADIUS_PX, attributes.blurBehindRadius)
    }

    @Test
    fun aViewThatLeavesTheWindowAndComesBackGetsItsBackdropBack() {
        val content = showInDialog()
        awaitBackdrop()
        val parent = content.parent as ViewGroup

        compose.runOnUiThread { parent.removeView(content) }
        compose.runOnUiThread { assertNull(underlay.source) }
        reported.clear()
        compose.runOnUiThread { parent.addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }

        awaitBackdrop()
    }

    @Test
    fun installingAgainReplacesTheUnderlayAndKeepsTheOriginalBackground() {
        val previous = ColorDrawable(Color.Blue.toArgb())
        val content = showInDialog(background = previous)
        awaitBackdrop()
        val first = underlay
        reported.clear()

        compose.runOnUiThread { installUnderlay(content) }
        awaitBackdrop()
        compose.runOnUiThread { underlay.remove() }

        assertNull(first.source)
        assertNull(underlay.source)
        assertSame(previous, content.background)
    }

    @Test
    fun removingWhileTheFirstCaptureRunsStopsTheReports() {
        systemBlur.switchOff()
        val previous = ColorDrawable(Color.Blue.toArgb())
        val content = showInDialog(background = previous, configureContent = { view -> view.doOnAttach { underlay.remove() } })

        compose.awaitOrFail({ "the underlay was never removed" }) { compose.runOnUiThread { content.background === previous } }
        SystemClock.sleep(SETTLE_MILLIS)

        assertEquals(listOf(UnderlaySource.Pending), reported, "the capture outlived remove()")
        assertNull(underlay.source)
    }

    @Test
    fun anotherBackgroundTakesTheUnderlayOff() {
        val content = showInDialog()
        awaitBackdrop()

        compose.runOnUiThread { content.background = ColorDrawable(Color.Blue.toArgb()) }

        compose.awaitOrFail({ "the underlay kept running under another background" }) {
            compose.runOnUiThread { underlay.source } == null
        }
    }

    private fun awaitBackdrop() {
        compose.awaitOrFail({ "no backdrop under the view, reported $reported" }) {
            reported.lastOrNull()?.isBackdrop() == true
        }
        compose.runOnUiThread { assertSame(reported.last(), underlay.source) }
    }

    private fun showInDialog(
        background: ColorDrawable? = null,
        configureWindow: (Window) -> Unit = {},
        configureContent: (View) -> Unit = {},
    ): View {
        showHost()
        return compose.runOnUiThread {
            val content = View(compose.activity).apply { this.background = background }
            installUnderlay(content)
            configureContent(content)
            val frame = FrameLayout(compose.activity).apply {
                setBackgroundColor(BEHIND_THE_BACKDROP.toArgb())
                addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            val dialog = Dialog(compose.activity).apply {
                setContentView(frame, ViewGroup.LayoutParams(CONTENT_PX, CONTENT_PX))
                window?.setDimAmount(0f)
                window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                window?.let(configureWindow)
                show()
            }
            dismiss = dialog::dismiss
            content
        }
    }

    private fun showInPopupWindow(): View {
        showHost()
        return compose.runOnUiThread {
            val content = FrameLayout(compose.activity)
            installUnderlay(content)
            val popup = PopupWindow(content, CONTENT_PX, CONTENT_PX)
            popup.showAtLocation(compose.activity.window.decorView, Gravity.CENTER, 0, 0)
            dismiss = popup::dismiss
            content
        }
    }

    private fun installUnderlay(view: View) {
        underlay = view.blurredUnderlay(
            blurRadius = 0f,
            tint = TINT.toArgb(),
            fallback = FALLBACK.toArgb(),
            onSourceChange = { reported += it },
        )
    }

    private fun showHost() {
        compose.setContent { Box(Modifier.fillMaxSize().background(HOST)) }
        compose.waitForIdle()
    }

    private fun View.centerOnScreen(): Offset {
        val location = IntArray(2)
        compose.runOnUiThread { getLocationOnScreen(location) }

        return Offset(location[0] + CONTENT_PX / 2f, location[1] + CONTENT_PX / 2f)
    }

    private companion object {
        val HOST = Color.Red
        val FALLBACK = Color.Yellow
        val OTHER_TINT = Color.Blue.copy(alpha = 0.5f)
        const val CONTENT_PX = 360
        const val MINIMUM_WIDTH_PX = 120
        const val MINIMUM_HEIGHT_PX = 80
        const val OWN_BLUR_RADIUS_PX = 19
        const val SETTLE_MILLIS = 500L
    }
}
