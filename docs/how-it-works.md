# How it works

[Русская версия](how-it-works.ru.md)

## Windows

Underlay finds two windows: the overlay's own, which takes the system blur, and the host under it,
which the snapshot captures.

| Called from | Own window | Host window | Best source |
|-------------|------------|-------------|-------------|
| `Dialog`, Material 3 `AlertDialog` | dialog window | activity window | `SystemBlur` |
| `Popup`, `PopupWindow`, `DropdownMenu` | root view added through `WindowManager` | activity window | `SystemBlur` |
| `ModalBottomSheet` | its own window | activity window | `SystemBlur` |
| View `Dialog`, `DialogFragment`, `BottomSheetDialog` | dialog window | activity window | `SystemBlur` |
| Translucent or floating activity | activity window | none, the window below is another activity's | `SystemBlur` |
| Overlay inside the activity window | none | none | `Fallback` |

Without a window of its own the overlay would capture itself, so it draws `fallback`.

## The system blur

`FLAG_BLUR_BEHIND` and `blurBehindRadius` go on the overlay window, and the system blurs everything
behind it, live and at no cost to the app. Underlay follows
`WindowManager.addCrossWindowBlurEnabledListener` and switches between `SystemBlur` and `Snapshot`
as cross-window blur turns on and off. A window that refuses the flag gets `Snapshot`.

## The snapshot

The host window is copied at a quarter of its size once the requested frame is drawn: with
`PixelCopy` on API 26+, with `decorView.draw()` below. The copy is blurred with `RenderEffect` on
API 31+ and with three box-blur passes on `Dispatchers.Default` below. Until the snapshot is ready
only the tint is drawn; the first snapshot fades in over 200 ms. A failed recapture keeps the old
snapshot unless the host has resized.

`blurRadius` is the radius `Modifier.blur`, `blurBehindRadius` and `RenderEffect.createBlurEffect`
take, in window pixels. The snapshot turns it into the platform's Gaussian sigma,
`0.57735 * radius + 0.5`, scaled to the quarter-size copy, so `SystemBlur` and `Snapshot` blur
equally hard. The box blur picks the radius whose three passes come closest to that sigma.

## Scope

Underlay is Android-only. On iOS Compose dialogs share the view hierarchy and `UIVisualEffectView`
covers blur; desktop has no `FLAG_BLUR_BEHIND` or `PixelCopy`.
