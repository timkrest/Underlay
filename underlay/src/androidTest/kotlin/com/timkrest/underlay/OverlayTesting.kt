// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.content.getSystemService
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

internal typealias Overlay = @Composable (content: @Composable () -> Unit) -> Unit

private const val TIMEOUT_MILLIS = 20_000L

internal fun ComposeTestRule.awaitOrFail(failure: () -> String, condition: () -> Boolean) {
    try {
        waitUntil(TIMEOUT_MILLIS, condition)
    } catch (neverLanded: ComposeTimeoutException) {
        throw AssertionError("${failure()} within ${TIMEOUT_MILLIS}ms", neverLanded)
    }
}

internal fun isCrossWindowBlurEnabled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

    return InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getSystemService<WindowManager>()
        ?.isCrossWindowBlurEnabled == true
}

private var isTheSystemBlurSwitchedOffByATest = false

internal fun ComposeTestRule.takeTheSystemBlurOutOfTheLadder() {
    if (isCrossWindowBlurEnabled()) {
        shell("settings put global disable_window_blurs 1")
        isTheSystemBlurSwitchedOffByATest = true
    }

    awaitOrFail({ "the system kept blurring behind the window, no snapshot is ever taken" }) {
        !isCrossWindowBlurEnabled()
    }
}

internal fun ComposeTestRule.restoreTheSystemBlur() {
    if (!isTheSystemBlurSwitchedOffByATest) return
    isTheSystemBlurSwitchedOffByATest = false

    shell("settings delete global disable_window_blurs")

    awaitOrFail({ "the system never resumed blurring behind windows" }) { isCrossWindowBlurEnabled() }
}

private fun shell(command: String) {
    val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)

    output.use { descriptor -> FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
}
