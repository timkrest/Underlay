package com.timkrest.underlay

/**
 * Which step of the degradation ladder currently produces the underlay.
 *
 * Reported through the `onSourceChange` callback of
 * [blurredUnderlay][androidx.compose.ui.Modifier.blurredUnderlay].
 */
public enum class UnderlaySource {

    /**
     * The system blurs whatever is behind the overlay window itself, via `FLAG_BLUR_BEHIND`.
     * Requires API 31+, cross-window blur enabled by the system, and an overlay that really does
     * own a separate window. Only the tint is drawn on top.
     */
    SystemBlur,

    /**
     * A one-shot snapshot of the host window, blurred on a background thread, is drawn under the
     * tint. Requires a resolvable host window that is not the overlay's own window.
     */
    Snapshot,

    /**
     * A snapshot is being captured and blurred. Only the tint is drawn, so the host window shows
     * through unblurred until [Snapshot] takes over. Transient: it is always followed by [Snapshot]
     * or, if the capture keeps failing, by [Fallback].
     */
    Pending,

    /**
     * Nothing better is available and the solid fallback color is drawn. Reported when the caller
     * is not in a window of its own - blurring the host window would then mean blurring the overlay
     * into its own backdrop - when there is no host window to capture, and when capturing it failed.
     */
    Fallback,
}
