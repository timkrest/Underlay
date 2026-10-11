// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.content.Context
import android.graphics.Outline
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

private const val TITLE_SP = 24f
private const val BODY_SP = 14f

internal fun viewDialogCard(
    context: Context,
    blurRadius: Dp,
    onBlurRadiusChange: (Dp) -> Unit,
    onClose: () -> Unit,
): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    val padding = context.toPx(CARD_PADDING)
    setPadding(padding, padding, padding, padding)
    roundCorners(context.toPx(CARD_CORNER).toFloat())

    addView(label(OverlayKind.ViewDialog.label, TITLE_SP, Color.White))
    addView(label(OverlayKind.ViewDialog.explanation, BODY_SP, CARD_SECONDARY_TEXT).spacedBelow())
    addView(radiusSeekBar(blurRadius, onBlurRadiusChange).spacedBelow())
    addView(closeButton(onClose).spacedBelow())
}

private fun View.label(text: String, sizeSp: Float, color: Color) = TextView(context).apply {
    this.text = text
    setTextColor(color.toArgb())
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
}

private fun View.radiusSeekBar(blurRadius: Dp, onBlurRadiusChange: (Dp) -> Unit) = SeekBar(context).apply {
    max = MAX_BLUR_RADIUS.value.roundToInt()
    progress = blurRadius.value.roundToInt()
    setOnSeekBarChangeListener(
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) onBlurRadiusChange(progress.dp)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        },
    )
}

private fun View.closeButton(onClose: () -> Unit) =
    Button(context, null, android.R.attr.borderlessButtonStyle).apply {
        text = context.getString(android.R.string.cancel)
        setTextColor(Color.White.toArgb())
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
        setOnClickListener { onClose() }
    }

private fun View.spacedBelow(): View = apply {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = context.toPx(CARD_SPACING) }
}

private fun View.roundCorners(radius: Float) {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
    clipToOutline = true
}

internal fun Context.toPx(dp: Dp): Int = (dp.value * resources.displayMetrics.density).roundToInt()

@Preview
@Composable
private fun ViewDialogCardPreview() {
    SampleTheme {
        AndroidView(
            factory = { context ->
                viewDialogCard(context, DEFAULT_BLUR_RADIUS, onBlurRadiusChange = {}, onClose = {})
            },
        )
    }
}
