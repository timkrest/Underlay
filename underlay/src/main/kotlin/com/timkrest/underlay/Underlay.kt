package com.timkrest.underlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp

/**
 * Draws the content of the window underneath this one, blurred, behind the composable.
 *
 * Intended for overlays that live in their own window - a [androidx.compose.ui.window.Dialog],
 * a [androidx.compose.ui.window.Popup], a translucent activity - where the content to blur belongs
 * to another window and is therefore out of reach of in-window blur libraries.
 *
 * The effect degrades in three steps, picking the best one the platform offers:
 * 1. API 31+: the system blurs the window behind via `FLAG_BLUR_BEHIND`, and only [tint] is drawn.
 *    The system switches this off in battery saver, which is observed and falls through to step 2.
 * 2. API 24+: a one-shot snapshot of the host window, blurred on a background thread, plus [tint].
 *    Re-taken when the host window changes size or configuration. A re-capture that fails keeps the
 *    snapshot already on screen, unless the host resized and it no longer maps onto this composable.
 *    While the first capture is in flight only [tint] is drawn, so the overlay settles from
 *    unblurred host content into the blurred snapshot instead of flashing [fallback] in between.
 * 3. Neither is available: [fallback] is drawn as a solid color.
 *
 * Called from a composable that is *not* in a window of its own, it draws [fallback] and nothing
 * else: the host window would then be this composable's own window, and blurring it would fold the
 * overlay into its own backdrop. Use an in-window blur library for that case.
 *
 * `FLAG_BLUR_BEHIND` is a window flag, so step 1 blurs everything behind the overlay's whole
 * window, while step 2 blurs only what is behind this composable. Apply the modifier to a
 * composable that fills the overlay window to get the same picture from both. For the same reason,
 * apply it once per overlay window: two instances in one window fight over the flag and radius.
 *
 * @param blurRadius blur strength, interpreted as the standard deviation of a Gaussian - the same
 *   meaning design tools and `RenderEffect.createBlurEffect` give it.
 * @param tint drawn over the blurred content, typically a translucent black or white.
 * @param fallback solid color drawn when no blur is available at all.
 * @param onSourceChange invoked whenever the active [UnderlaySource] changes, for diagnostics.
 */
@Composable
public fun Modifier.blurredUnderlay(
    blurRadius: Dp,
    tint: Color,
    fallback: Color,
    onSourceChange: ((UnderlaySource) -> Unit)? = null,
): Modifier {
    val windows = rememberUnderlayWindows()
    val backdrop = rememberUnderlayBackdrop(windows = windows, blurRadius = blurRadius)
    val placement = rememberSnapshotPlacement(host = windows.host)

    ReportUnderlaySource(source = backdrop.source, onSourceChange = onSourceChange)

    return this
        .onGloballyPositioned(placement::onPositioned)
        .drawBehind {
            when (backdrop) {
                UnderlayBackdrop.SystemBlur -> drawRect(tint)

                is UnderlayBackdrop.Snapshot -> {
                    drawSnapshot(backdrop.image, placement.originInHost)
                    drawRect(tint)
                }

                UnderlayBackdrop.Pending -> drawRect(tint)

                UnderlayBackdrop.Unavailable -> drawRect(fallback)
            }
        }
}

@Composable
private fun ReportUnderlaySource(source: UnderlaySource, onSourceChange: ((UnderlaySource) -> Unit)?) {
    val report by rememberUpdatedState(onSourceChange)

    LaunchedEffect(source) { report?.invoke(source) }
}
