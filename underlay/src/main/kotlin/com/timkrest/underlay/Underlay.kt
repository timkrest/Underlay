// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp

/**
 * Draws the window underneath this composable's window, blurred, behind the composable.
 *
 * For overlays in their own window - a [androidx.compose.ui.window.Dialog], a
 * [androidx.compose.ui.window.Popup], a translucent activity - whose backdrop belongs to another
 * window that in-window blur libraries cannot reach. The blur comes from the best source the device
 * offers, see [UnderlaySource]; outside a window of its own the composable draws [fallback] only.
 *
 * The system blurs everything behind the overlay window, a snapshot only what is behind this
 * composable: apply the modifier once per window, to a composable that fills it, for the same
 * picture from both. The snapshot is always taken of the activity window, so under a dialog over a
 * dialog it shows the activity, not the dialog in between.
 *
 * @param blurRadius blur radius, the same value `Modifier.blur` and `blurBehindRadius` take.
 * @param tint drawn over the blurred content, usually a translucent black or white.
 * @param fallback solid color drawn when no blur is available at all.
 * @param liveSnapshot capture the host window again every time it draws, so the snapshot follows
 *   content moving underneath a couple of frames behind, at a capture and a blur per host frame; below
 *   API 26 the capture redraws the host in software. The system blur is live on its own and ignores
 *   it.
 * @param state exposes the active [UnderlaySource] as Compose state and captures the host window
 *   again on [UnderlayState.refresh].
 * @param onSourceChange called whenever the active [UnderlaySource] changes.
 */
public fun Modifier.blurredUnderlay(
    blurRadius: Dp,
    tint: Color,
    fallback: Color,
    liveSnapshot: Boolean = false,
    state: UnderlayState? = null,
    onSourceChange: ((UnderlaySource) -> Unit)? = null,
): Modifier = this then UnderlayElement(blurRadius, tint, fallback, liveSnapshot, state, onSourceChange)

private data class UnderlayElement(
    private val blurRadius: Dp,
    private val tint: Color,
    private val fallback: Color,
    private val liveSnapshot: Boolean,
    private val state: UnderlayState?,
    private val onSourceChange: ((UnderlaySource) -> Unit)?,
) : ModifierNodeElement<UnderlayNode>() {

    override fun create(): UnderlayNode =
        UnderlayNode(blurRadius, tint, fallback, liveSnapshot, state, onSourceChange)

    override fun update(node: UnderlayNode) {
        node.update(blurRadius, tint, fallback, liveSnapshot, state, onSourceChange)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "blurredUnderlay"
        properties["blurRadius"] = blurRadius
        properties["tint"] = tint
        properties["fallback"] = fallback
        properties["liveSnapshot"] = liveSnapshot
        properties["state"] = state
    }
}
