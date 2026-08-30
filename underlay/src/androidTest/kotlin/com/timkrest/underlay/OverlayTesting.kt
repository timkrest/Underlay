package com.timkrest.underlay

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.content.getSystemService

internal typealias Overlay = @Composable (content: @Composable () -> Unit) -> Unit

private const val TIMEOUT_MILLIS = 10_000L

internal fun ComposeTestRule.awaitOrFail(failure: () -> String, condition: () -> Boolean) {
    try {
        waitUntil(TIMEOUT_MILLIS, condition)
    } catch (neverLanded: ComposeTimeoutException) {
        throw AssertionError("${failure()} within ${TIMEOUT_MILLIS}ms", neverLanded)
    }
}

internal fun Activity.isCrossWindowBlurEnabled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

    return getSystemService<WindowManager>()?.isCrossWindowBlurEnabled == true
}
