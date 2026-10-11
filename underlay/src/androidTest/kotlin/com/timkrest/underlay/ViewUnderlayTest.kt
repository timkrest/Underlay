// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class ViewUnderlayTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val reported = CopyOnWriteArrayList<UnderlaySource>()
    private var dismiss: () -> Unit = {}
    private lateinit var underlay: ViewUnderlay

    @After
    fun closeTheOverlay() {
        compose.runOnUiThread { dismiss() }
        compose.restoreTheSystemBlur()
    }

    @Test
    fun aViewDialogNeverReportsTheFallback() {
        showInDialog()

        awaitBackdrop()
        assertFalse(UnderlaySource.Fallback in reported, "an overlay in its own window never falls back")
    }

    @Test
    fun aPopupWindowNeverReportsTheFallback() {
        showInPopupWindow()

        awaitBackdrop()
        assertFalse(UnderlaySource.Fallback in reported, "an overlay in its own window never falls back")
    }

    @Test
    fun withoutSystemBlurAViewDialogDrawsTheHostBehindIt() {
        compose.takeTheSystemBlurOutOfTheLadder()

        val content = showInDialog()

        compose.awaitOrFail({ "no host behind the dialog, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.Snapshot &&
                screenshot().colorAt(content.centerOnScreen()).matches(HOST.tinted(TINT))
        }
    }

    @Test
    fun removingTheUnderlayPutsTheBackgroundBack() {
        val previous = ColorDrawable(Color.Blue.toArgb())
        val content = showInDialog(background = previous)
        awaitBackdrop()

        compose.runOnUiThread { underlay.remove() }

        assertSame(previous, content.background)
        assertNull(underlay.source)
    }

    private fun awaitBackdrop() {
        compose.awaitOrFail({ "no backdrop under the view, reported $reported" }) {
            reported.lastOrNull() == UnderlaySource.SystemBlur || reported.lastOrNull() == UnderlaySource.Snapshot
        }
        compose.runOnUiThread { assertSame(reported.last(), underlay.source) }
    }

    private fun showInDialog(background: ColorDrawable? = null): View {
        showHost()
        return compose.runOnUiThread {
            val content = FrameLayout(compose.activity).apply { this.background = background }
            installUnderlay(content)
            val dialog = Dialog(compose.activity).apply {
                setContentView(content, ViewGroup.LayoutParams(CONTENT_PX, CONTENT_PX))
                window?.setDimAmount(0f)
                window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
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
        val TINT = Color.Green.copy(alpha = 0.5f)
        val FALLBACK = Color.Magenta
        const val CONTENT_PX = 360
    }
}
