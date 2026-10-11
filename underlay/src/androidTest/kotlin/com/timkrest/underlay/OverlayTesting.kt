// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.os.Build
import android.os.SystemClock
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.core.content.getSystemService
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import java.io.FileInputStream
import kotlin.test.assertNotNull

internal typealias Overlay = @Composable (content: @Composable () -> Unit) -> Unit

internal val inDialog: Overlay = { content -> Dialog(onDismissRequest = {}) { content() } }

internal val inUndimmedDialog: Overlay = { content ->
    Dialog(onDismissRequest = {}) {
        DropTheWindowDim()
        content()
    }
}

internal val inPopup: Overlay = { content -> Popup { content() } }

@Composable
internal fun DropTheWindowDim() {
    val window = dialogWindow()

    SideEffect { window?.setDimAmount(0f) }
}

@Composable
internal fun dialogWindow(): Window? = (LocalView.current.parent as? DialogWindowProvider)?.window

internal fun ComposeContentTestRule.showDialogWindow(): Window {
    var shown: Window? = null
    setContent {
        Dialog(onDismissRequest = {}) {
            shown = dialogWindow()
            Box(Modifier.size(64.dp))
        }
    }
    waitForIdle()
    return assertNotNull(shown, "a dialog owns a window")
}

private const val TIMEOUT_MILLIS = 20_000L

internal fun ComposeTestRule.awaitOrFail(failure: () -> String, condition: () -> Boolean) {
    try {
        waitUntil(TIMEOUT_MILLIS, condition)
    } catch (neverLanded: ComposeTimeoutException) {
        throw AssertionError("${failure()} within ${TIMEOUT_MILLIS}ms", neverLanded)
    }
}

internal val TINT = Color.Green.copy(alpha = 0.5f)
internal val BACKDROP_SIZE = 120.dp
internal val BEHIND_THE_BACKDROP = Color.Magenta

internal fun UnderlaySource.isBackdrop(): Boolean = this == UnderlaySource.SystemBlur || this == UnderlaySource.Snapshot

internal fun isCrossWindowBlurEnabled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

    return InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getSystemService<WindowManager>()
        ?.isCrossWindowBlurEnabled == true
}

class SystemBlurRule : ExternalResource() {

    private var original: String? = null

    override fun after() {
        val original = original ?: return
        this.original = null
        shell(if (original == UNSET) "settings delete global $SETTING" else "settings put global $SETTING $original")
    }

    fun switchOff() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        if (original == null) original = shell("settings get global $SETTING").trim().ifEmpty { UNSET }
        shell("settings put global $SETTING 1")
        awaitSetting { !isCrossWindowBlurEnabled() }
    }

    private fun awaitSetting(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MILLIS
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "the system never applied $SETTING" }
            SystemClock.sleep(POLL_MILLIS)
        }
    }
}

private const val POLL_MILLIS = 50L
private const val SETTING = "disable_window_blurs"
private const val UNSET = "null"

private fun shell(command: String): String {
    val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)

    return output.use { descriptor -> FileInputStream(descriptor.fileDescriptor).use { it.readBytes().decodeToString() } }
}
