package com.timkrest.underlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp

@Immutable
internal sealed interface UnderlayBackdrop {

    val source: UnderlaySource

    data object SystemBlur : UnderlayBackdrop {
        override val source: UnderlaySource = UnderlaySource.SystemBlur
    }

    class Snapshot(val image: ImageBitmap) : UnderlayBackdrop {
        override val source: UnderlaySource = UnderlaySource.Snapshot
    }

    /**
     * A snapshot is on its way. Only the tint is drawn, so the overlay settles from unblurred host
     * content into the blurred snapshot rather than flashing an opaque fallback in between.
     */
    data object Pending : UnderlayBackdrop {
        override val source: UnderlaySource = UnderlaySource.Pending
    }

    data object Unavailable : UnderlayBackdrop {
        override val source: UnderlaySource = UnderlaySource.Fallback
    }
}

@Composable
internal fun rememberUnderlayBackdrop(windows: UnderlayWindows, blurRadius: Dp): UnderlayBackdrop {
    val isWindowBlurred = rememberWindowBlurBehind(overlay = windows.overlay, radius = blurRadius)
    val snapshot = rememberBlurredSnapshot(
        host = if (isWindowBlurred) null else windows.host,
        radius = blurRadius,
    )

    return remember(isWindowBlurred, snapshot) {
        when {
            isWindowBlurred -> UnderlayBackdrop.SystemBlur
            snapshot is UnderlaySnapshot.Ready -> UnderlayBackdrop.Snapshot(snapshot.image)
            snapshot is UnderlaySnapshot.Pending -> UnderlayBackdrop.Pending
            else -> UnderlayBackdrop.Unavailable
        }
    }
}
