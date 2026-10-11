// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("ViewUnderlays")

package com.timkrest.underlay

import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.annotation.Px
import androidx.compose.ui.platform.AndroidUiDispatcher
import com.timkrest.underlay.backdrop.BlurRadius
import com.timkrest.underlay.blur.SnapshotBlur
import com.timkrest.underlay.window.findActivity
import com.timkrest.underlay.window.sharedGraphicsContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Makes the window under this view's window, blurred, the background of this view.
 *
 * The View version of [blurredUnderlay][androidx.compose.ui.Modifier.blurredUnderlay], for an
 * `android.app.Dialog`, a `DialogFragment`, a `BottomSheetDialog` or a `PopupWindow`. Call it on the
 * root of your own layout, not on a `decorView` or a bottom sheet. It keeps the old background's
 * outline and padding, so set `clipToOutline` for rounded corners. [ViewUnderlay.remove] or a new
 * background takes it off.
 *
 * @param blurRadius in pixels, the same value `Modifier.blur` and `blurBehindRadius` take.
 * @param tint drawn over the blur.
 * @param fallback drawn when there is nothing to blur.
 * @param liveSnapshot see [ViewUnderlay.liveSnapshot].
 * @param onSourceChange called when the [UnderlaySource] changes, never before this function returns.
 */
@MainThread
@JvmOverloads
public fun View.blurredUnderlay(
    @Px blurRadius: Float,
    @ColorInt tint: Int,
    @ColorInt fallback: Int,
    liveSnapshot: Boolean = false,
    onSourceChange: ViewUnderlay.OnSourceChangeListener? = null,
): ViewUnderlay {
    (getTag(R.id.underlay_view_underlay) as? ViewUnderlay)?.remove()

    return ViewUnderlay(this, blurRadius, tint, fallback, liveSnapshot, onSourceChange)
}

/** The underlay [blurredUnderlay][android.view.View.blurredUnderlay] put on a view. Main thread only. */
@MainThread
public class ViewUnderlay internal constructor(
    private val view: View,
    blurRadius: Float,
    tint: Int,
    fallback: Int,
    liveSnapshot: Boolean,
    onSourceChange: OnSourceChangeListener?,
) {

    /** Called when the [UnderlaySource] changes. */
    public fun interface OnSourceChangeListener {
        public fun onSourceChange(source: UnderlaySource)
    }

    private class Binding(val scope: CoroutineScope, val controller: UnderlayController)

    private val previousBackground = view.background
    private val drawable = UnderlayDrawable(previousBackground, tint, fallback)
    private var binding: Binding? = null
    private var isRemoved = false

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(attached: View) = bind()

        override fun onViewDetachedFromWindow(detached: View) = unbind()
    }
    private val beforeDraw = ViewTreeObserver.OnPreDrawListener {
        if (view.background !== drawable) remove()
        true
    }

    /** In pixels, the same value `Modifier.blur` and `blurBehindRadius` take. */
    @get:Px
    public var blurRadius: Float = blurRadius
        set(@Px value) {
            field = value
            binding?.controller?.setRadius(BlurRadius.ofPx(value))
        }

    /** Drawn over the blur. */
    @get:ColorInt
    public var tint: Int
        get() = drawable.tintColor
        set(@ColorInt value) {
            drawable.tintColor = value
        }

    /** Drawn when there is nothing to blur. */
    @get:ColorInt
    public var fallback: Int
        get() = drawable.fallbackColor
        set(@ColorInt value) {
            drawable.fallbackColor = value
        }

    /** Capture the host again on every host frame. Costs a capture and a blur each time. */
    public var liveSnapshot: Boolean = liveSnapshot
        set(value) {
            field = value
            binding?.controller?.setLive(value)
        }

    /** Called when the [UnderlaySource] changes. */
    public var onSourceChange: OnSourceChangeListener? = onSourceChange

    /** Null while the view is detached or the underlay removed. */
    public val source: UnderlaySource? get() = binding?.controller?.source

    init {
        view.setTag(R.id.underlay_view_underlay, this)
        view.background = drawable
        view.addOnAttachStateChangeListener(attachListener)
        if (view.isAttachedToWindow) view.post(::bind)
    }

    /**
     * Retakes the snapshot after the host changed without resizing, a handled configuration change
     * included. The old one stays until the new one is ready.
     */
    public fun refresh() {
        binding?.controller?.recapture()
    }

    /** Takes the underlay off and puts the old background back. */
    public fun remove() {
        if (isRemoved) return
        isRemoved = true

        view.removeOnAttachStateChangeListener(attachListener)
        unbind()
        if (view.getTag(R.id.underlay_view_underlay) === this) view.setTag(R.id.underlay_view_underlay, null)
        if (view.background === drawable) view.background = previousBackground
    }

    private fun bind() {
        if (isRemoved || binding != null || !view.isAttachedToWindow) return

        val scope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)
        val controller = UnderlayController(
            blur = SnapshotBlur.bestAvailable { view.sharedGraphicsContext() },
            scope = scope,
            onSourceChange = ::onSourceReported,
            onRedraw = drawable::invalidateSelf,
        )
        binding = Binding(scope, controller)
        drawable.controller = controller
        view.viewTreeObserver.addOnPreDrawListener(beforeDraw)
        controller.bind(view, view.context.findActivity(), BlurRadius.ofPx(blurRadius), liveSnapshot)
    }

    private fun unbind() {
        val binding = binding ?: return
        this.binding = null

        view.viewTreeObserver.removeOnPreDrawListener(beforeDraw)
        drawable.controller = null
        binding.controller.release()
        binding.scope.cancel()
    }

    private fun onSourceReported(source: UnderlaySource) {
        onSourceChange?.onSourceChange(source)
    }
}
