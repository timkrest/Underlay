// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp

/**
 * Draws the window under this overlay's window, blurred, behind this composable.
 *
 * For a [androidx.compose.ui.window.Dialog], a [androidx.compose.ui.window.Popup] or a translucent
 * activity. Apply it once per window, to a composable that fills it: the system blur covers the whole
 * window, a snapshot only this composable. The snapshot is always of the activity window.
 *
 * @param blurRadius the same value `Modifier.blur` and `blurBehindRadius` take.
 * @param tint drawn over the blur.
 * @param fallback drawn when there is nothing to blur.
 * @param liveSnapshot capture the host again on every host frame. Costs a capture and a blur each time.
 * @param state the current [UnderlaySource] as Compose state, and [UnderlayState.refresh].
 * @param onSourceChange called when the [UnderlaySource] changes.
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
