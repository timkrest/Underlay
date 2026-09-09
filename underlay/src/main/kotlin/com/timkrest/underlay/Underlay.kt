// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp

/**
 * Draws the content of the window underneath this one, blurred, behind the composable.
 *
 * For overlays that live in their own window - a [androidx.compose.ui.window.Dialog], a
 * [androidx.compose.ui.window.Popup], a translucent activity - where the content to blur belongs to
 * another window and in-window blur libraries cannot reach it.
 *
 * The blur comes from the best source the device offers: the system blurs behind the overlay window
 * on API 31+, a blurred snapshot of the host window otherwise, and [fallback] when neither works.
 * See [UnderlaySource]. The snapshot is taken again when the host window changes size or
 * configuration, and whenever [UnderlayState.refresh] is called on [state].
 *
 * In a composable that is not in a window of its own it draws [fallback] and nothing else: the host
 * window would then be this composable's own window, and blurring it would fold the overlay into
 * its own backdrop. Use an in-window blur library for that case.
 *
 * The system blurs everything behind the overlay window, a snapshot only what is behind this
 * composable. Apply the modifier to a composable that fills the overlay window to get the same
 * picture from both, and apply it once per window: two of them fight over the flag and the radius.
 *
 * @param blurRadius blur strength, read as the standard deviation of a Gaussian - the same meaning
 *   design tools and `RenderEffect.createBlurEffect` give it.
 * @param tint drawn over the blurred content, usually a translucent black or white.
 * @param fallback solid color drawn when no blur is available at all.
 * @param state handle on this underlay: exposes the active [UnderlaySource] as Compose state and
 *   takes a fresh snapshot of the host window on [UnderlayState.refresh], for content that moves
 *   underneath a long-lived overlay. The system blur is live and ignores the refresh.
 * @param onSourceChange called whenever the active [UnderlaySource] changes, for diagnostics.
 */
public fun Modifier.blurredUnderlay(
    blurRadius: Dp,
    tint: Color,
    fallback: Color,
    state: UnderlayState? = null,
    onSourceChange: ((UnderlaySource) -> Unit)? = null,
): Modifier = this then UnderlayElement(blurRadius, tint, fallback, state, onSourceChange)

private data class UnderlayElement(
    private val blurRadius: Dp,
    private val tint: Color,
    private val fallback: Color,
    private val state: UnderlayState?,
    private val onSourceChange: ((UnderlaySource) -> Unit)?,
) : ModifierNodeElement<UnderlayNode>() {

    override fun create(): UnderlayNode = UnderlayNode(blurRadius, tint, fallback, state, onSourceChange)

    override fun update(node: UnderlayNode) {
        node.update(blurRadius, tint, fallback, state, onSourceChange)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "blurredUnderlay"
        properties["blurRadius"] = blurRadius
        properties["tint"] = tint
        properties["fallback"] = fallback
        properties["state"] = state
    }
}
