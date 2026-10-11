// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("ViewUnderlays")

package com.timkrest.underlay

import android.graphics.drawable.Drawable
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
 * Makes the window underneath this view's window, blurred, the background of this view.
 *
 * The View counterpart of [blurredUnderlay][androidx.compose.ui.Modifier.blurredUnderlay], for
 * overlays built from Views: an `android.app.Dialog`, a `DialogFragment`, a `BottomSheetDialog`, a
 * `PopupWindow`. It picks its source the same way, see [UnderlaySource].
 *
 * The underlay takes the background slot of this view until [ViewUnderlay.remove] puts the previous
 * background back. It reports the previous background's outline and padding, so set
 * `clipToOutline` for rounded corners. Call it on a view of your own, such as the root of the
 * overlay's layout, not on one whose background something else manages, like a `decorView` or the
 * sheet of a `BottomSheetDialog`. Calling it again on the same view replaces the underlay, and setting
 * another background on the view removes it.
 *
 * @param blurRadius blur radius in pixels, the same value `Modifier.blur` and `blurBehindRadius` take.
 * @param tint drawn over the blurred content, usually a translucent black or white.
 * @param fallback solid color drawn when no blur is available at all.
 * @param liveSnapshot capture the host window again every time it draws. See
 *   [ViewUnderlay.liveSnapshot].
 * @param onSourceChange called whenever the active [UnderlaySource] changes, never before this
 *   function has returned.
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

/**
 * Handle on the underlay [blurredUnderlay][android.view.View.blurredUnderlay] installed on a view.
 * Every member is to be used from the main thread.
 */
@MainThread
public class ViewUnderlay internal constructor(
    private val view: View,
    blurRadius: Float,
    tint: Int,
    fallback: Int,
    liveSnapshot: Boolean,
    onSourceChange: OnSourceChangeListener?,
) {

    /** Called whenever the active [UnderlaySource] changes. */
    public fun interface OnSourceChangeListener {
        public fun onSourceChange(source: UnderlaySource)
    }

    private class Binding(val scope: CoroutineScope, val controller: UnderlayController)

    private val previousBackground = view.background
    private val drawable = UnderlayDrawable(view, previousBackground, tint, fallback)
    private val installedBackground: Drawable?
    private var binding: Binding? = null
    private var isRemoved = false

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(attached: View) = bind()

        override fun onViewDetachedFromWindow(detached: View) = unbind()
    }
    private val beforeDraw = ViewTreeObserver.OnPreDrawListener {
        when {
            view.background !== installedBackground -> remove()
            binding?.controller?.needsRedrawBeforeDraw(view) == true -> drawable.invalidateSelf()
        }
        true
    }

    /** Blur radius in pixels, the same value `Modifier.blur` and `blurBehindRadius` take. */
    @get:Px
    public var blurRadius: Float = blurRadius
        set(@Px value) {
            field = value
            binding?.controller?.setRadius(BlurRadius.ofPx(value))
        }

    /** Drawn over the blurred content. */
    @get:ColorInt
    public var tint: Int
        get() = drawable.tintColor
        set(@ColorInt value) {
            drawable.tintColor = value
        }

    /** Drawn when no blur is available at all. */
    @get:ColorInt
    public var fallback: Int
        get() = drawable.fallbackColor
        set(@ColorInt value) {
            drawable.fallbackColor = value
        }

    /**
     * Capture the host window again every time it draws, so the snapshot follows content that moves
     * underneath a couple of frames behind. It costs a capture and a blur per host frame; below API 26
     * the capture redraws the host in software. The system blur ignores it.
     */
    public var liveSnapshot: Boolean = liveSnapshot
        set(value) {
            field = value
            binding?.controller?.setLive(value)
        }

    /** Called whenever the active [UnderlaySource] changes. */
    public var onSourceChange: OnSourceChangeListener? = onSourceChange

    /** Where the underlay comes from. Null while the view is not attached to a window, or removed. */
    public var source: UnderlaySource? = null
        private set

    init {
        view.setTag(R.id.underlay_view_underlay, this)
        view.background = drawable
        installedBackground = view.background
        view.addOnAttachStateChangeListener(attachListener)
        if (view.isAttachedToWindow) view.post(::bind)
    }

    /**
     * Captures the host window again. The snapshot on screen stays until the new one is ready. Call it
     * when the host changed without resizing, a configuration change it handles itself included.
     */
    public fun refresh() {
        binding?.controller?.recapture()
    }

    /** Takes the underlay off the view and puts back the background it had before. */
    public fun remove() {
        if (isRemoved) return
        isRemoved = true

        view.removeOnAttachStateChangeListener(attachListener)
        unbind()
        if (view.getTag(R.id.underlay_view_underlay) === this) view.setTag(R.id.underlay_view_underlay, null)
        if (view.background === installedBackground) view.background = previousBackground
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
        source = null
    }

    private fun onSourceReported(source: UnderlaySource) {
        this.source = source
        onSourceChange?.onSourceChange(source)
    }
}
