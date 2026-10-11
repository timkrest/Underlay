// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.core.content.getSystemService
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import java.io.FileInputStream

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
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window

    SideEffect { window?.setDimAmount(0f) }
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

    override fun before() = restore()

    override fun after() = restore()

    fun switchOff() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        shell("settings put global disable_window_blurs 1")
        awaitSetting { !isCrossWindowBlurEnabled() }
    }

    private fun restore() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) shell("settings delete global disable_window_blurs")
    }

    private fun awaitSetting(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MILLIS
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "the system never applied disable_window_blurs" }
            SystemClock.sleep(POLL_MILLIS)
        }
    }
}

private const val POLL_MILLIS = 50L

private fun shell(command: String) {
    val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)

    output.use { descriptor -> FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
}
