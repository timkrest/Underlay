// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
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
import com.timkrest.underlay.UnderlaySource
import com.timkrest.underlay.ViewUnderlay
import com.timkrest.underlay.blurredUnderlay
import kotlin.math.roundToInt

private const val EYEBROW_SP = 11f
private const val TITLE_SP = 24f
private const val BODY_SP = 14f
private const val BADGE_SP = 14f
private val BADGE_DOT = 8.dp
private val SLIDER_TINT = Color(0xFF9FD0FF)

/** The Views counterpart of [OverlayCard], centered on a [root] that fills the dialog window. */
internal class ViewDialogCard(
    context: Context,
    blurRadius: Dp,
    fallback: Int,
    onSourceChange: ViewUnderlay.OnSourceChangeListener,
    onBlurRadiusChange: (Dp) -> Unit,
    onShuffle: () -> Unit,
    onClose: () -> Unit,
) {

    private val badge = context.badge()
    private val card = context.card(blurRadius, onBlurRadiusChange, onShuffle = {
        onShuffle()
        underlay.refresh()
    }, onClose)

    val root: FrameLayout = FrameLayout(context).apply {
        setOnClickListener { onClose() }
        val cardWidth = minOf(context.toPx(CARD_MAX_WIDTH), resources.displayMetrics.widthPixels - context.toPx(CARD_MARGIN) * 2)
        addView(card, FrameLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    val underlay: ViewUnderlay = root.blurredUnderlay(
        blurRadius = root.context.toPx(blurRadius).toFloat(),
        tint = UNDERLAY_TINT.toArgb(),
        fallback = fallback,
        onSourceChange = onSourceChange,
    )

    var source: UnderlaySource? = null
        set(value) {
            if (field == value) return
            field = value
            paintBadge()
        }

    init {
        paintBadge()
    }

    private fun paintBadge() {
        val color = source.indicatorColor()
        badge.text = source?.description() ?: "Resolving"
        badge.background = pill(color.copy(alpha = BADGE_FILL_ALPHA), color.copy(alpha = BADGE_BORDER_ALPHA), badge.context)
        (badge.compoundDrawablesRelative[0] as? GradientDrawable)?.setColor(color.toArgb())
    }

    private fun Context.card(
        blurRadius: Dp,
        onBlurRadiusChange: (Dp) -> Unit,
        onShuffle: () -> Unit,
        onClose: () -> Unit,
    ) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        isClickable = true
        val padding = toPx(CARD_PADDING)
        setPadding(padding, padding, padding, padding)
        background = pill(CARD_FILL, CARD_BORDER, this@card, corner = CARD_CORNER)
        clipToOutline = true

        addView(label(HostedKind.ViewDialog.windows.uppercase(), EYEBROW_SP, CARD_SECONDARY_TEXT))
        addView(titleRow().spacedBelow())
        addView(label(HostedKind.ViewDialog.explanation, BODY_SP, CARD_SECONDARY_TEXT).spacedBelow())
        addView(radiusRow(blurRadius, onBlurRadiusChange).spacedBelow())
        addView(buttonRow(onShuffle, onClose).spacedBelow())
    }

    private fun Context.titleRow() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(
            label(HostedKind.ViewDialog.label, TITLE_SP, Color.White).apply { setTypeface(typeface, Typeface.BOLD) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        addView(badge)
    }

    @SuppressLint("SetTextI18n")
    private fun Context.radiusRow(blurRadius: Dp, onBlurRadiusChange: (Dp) -> Unit) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        val value = label("${blurRadius.value.roundToInt()} dp", BADGE_SP, Color.White)
        addView(
            radiusSeekBar(blurRadius) { radius ->
                value.text = "${radius.value.roundToInt()} dp"
                onBlurRadiusChange(radius)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        val valueParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        addView(value, valueParams.apply { marginStart = toPx(12.dp) })
    }

    private fun Context.badge() = TextView(this).apply {
        setTextColor(Color.White.toArgb())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, BADGE_SP)
        maxLines = 1
        compoundDrawablePadding = toPx(8.dp)
        setPaddingRelative(toPx(10.dp), toPx(6.dp), toPx(12.dp), toPx(6.dp))
        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setSize(toPx(BADGE_DOT), toPx(BADGE_DOT))
        }
        setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
    }
}

private fun pill(fill: Color, border: Color, context: Context, corner: Dp = 999.dp) = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadius = context.toPx(corner).toFloat()
    setColor(fill.toArgb())
    setStroke(context.toPx(1.dp), border.toArgb())
}

private fun Context.label(text: String, sizeSp: Float, color: Color) = TextView(this).apply {
    this.text = text
    setTextColor(color.toArgb())
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
}

private fun Context.radiusSeekBar(blurRadius: Dp, onBlurRadiusChange: (Dp) -> Unit) = SeekBar(this).apply {
    max = MAX_BLUR_RADIUS.value.roundToInt()
    progress = blurRadius.value.roundToInt()
    progressTintList = ColorStateList.valueOf(SLIDER_TINT.toArgb())
    thumbTintList = ColorStateList.valueOf(SLIDER_TINT.toArgb())
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

private fun Context.buttonRow(onShuffle: () -> Unit, onClose: () -> Unit) = LinearLayout(this).apply {
    gravity = Gravity.END
    addView(textButton("Shuffle feed", onShuffle))
    addView(textButton("Close", onClose))
}

private fun Context.textButton(text: String, onClick: () -> Unit) =
    Button(this, null, android.R.attr.borderlessButtonStyle).apply {
        this.text = text
        isAllCaps = false
        setTextColor(Color.White.toArgb())
        setOnClickListener { onClick() }
    }

private fun View.spacedBelow(): View = apply {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = context.toPx(CARD_SPACING) }
}

internal fun Context.toPx(dp: Dp): Int = (dp.value * resources.displayMetrics.density).roundToInt()

@Preview
@Composable
private fun ViewDialogCardPreview() {
    SampleTheme {
        AndroidView(
            factory = { context ->
                ViewDialogCard(context, DEFAULT_BLUR_RADIUS, fallback = 0xFF101318.toInt(), {}, {}, {}, {})
                    .apply { source = UnderlaySource.Snapshot }
                    .root
            },
        )
    }
}
