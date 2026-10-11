// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.content.Context
import android.graphics.Outline
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

private const val TITLE_SP = 24f
private const val BODY_SP = 14f

internal fun viewDialogCard(context: Context, onClose: () -> Unit): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val padding = context.dp(CARD_PADDING.value)
        setPadding(padding, padding, padding, padding)
        roundCorners(context.dp(CARD_CORNER.value).toFloat())

        addView(text(context, OverlayKind.ViewDialog.label, TITLE_SP, Color.White.toArgb()))
        addView(text(context, OverlayKind.ViewDialog.explanation, BODY_SP, CARD_SECONDARY_TEXT.toArgb()).spacedBelow(context))
        addView(
            text(context, "Close", BODY_SP, Color.White.toArgb()).apply {
                gravity = Gravity.END
                setOnClickListener { onClose() }
            }.spacedBelow(context),
        )
    }

private fun text(context: Context, text: String, sizeSp: Float, color: Int) = TextView(context).apply {
    this.text = text
    setTextColor(color)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
}

private fun View.spacedBelow(context: Context): View = apply {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = context.dp(CARD_SPACING.value) }
}

private fun View.roundCorners(radius: Float) {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
    clipToOutline = true
}

internal fun Context.dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()
