# How it works

[Русская версия](how-it-works.ru.md)

## Windows

Underlay resolves two windows: the one the caller lives in, and the host window underneath.

| Called from | Own window | Host window | Best source |
|-------------|------------|-------------|-------------|
| `Dialog`, Material 3 `AlertDialog` | dialog window | activity window | `SystemBlur` |
| `Popup`, `PopupWindow`, `DropdownMenu` | root view added through `WindowManager` | activity window | `SystemBlur` |
| `ModalBottomSheet` | its own window | activity window | `SystemBlur` |
| View `Dialog`, `DialogFragment`, `BottomSheetDialog` | dialog window | activity window | `SystemBlur` |
| Translucent or floating activity | activity window | none, the window underneath belongs to another activity | `SystemBlur` |
| Overlay inside the activity window | none | none | `Fallback` |

Without a window of its own the host window would be the caller's own window, and the snapshot would
fold the overlay into its own backdrop. That case draws `fallback`; an in-window blur library covers
it.

A translucent activity has no host window either, so it drops from `SystemBlur` straight to
`Fallback`.

## The system blur

`FLAG_BLUR_BEHIND` and `blurBehindRadius` go on the overlay window: through `Window.setAttributes`
for a Compose `Dialog`, through `WindowManager.updateViewLayout` for everything else. The system blurs everything behind
the window, live, at no cost to the app.

Cross-window blur can be off: battery saver turns it off, and some vendors never turn it on: Samsung
Galaxy phones leave `ro.surface_flinger.supports_background_blur` unset.
Underlay listens through `WindowManager.addCrossWindowBlurEnabledListener`, drops the flag and falls
through to the snapshot. A window that refuses the flag falls through the same way.

## The snapshot

The host window is copied with `PixelCopy` from API 26, with `decorView.draw()` below it. On API 26+
`draw()` is not a fallback: it throws on hardware bitmaps, which Coil and Glide decode by default.

A capture waits for the frame it was asked from to be drawn, covers the largest exact quarter of the
window, and is blurred with `RenderEffect` on the GPU from API 31, with three box passes on
`Dispatchers.Default` below it. Until it lands only the tint is drawn, and the first snapshot fades
in over 200 ms.

`blurRadius` is the radius `Modifier.blur`, `blurBehindRadius` and `RenderEffect.createBlurEffect`
take, in window pixels. The platform turns it into a Gaussian sigma as `0.57735 * radius + 0.5`, and
the snapshot uses the same sigma at its quarter scale, so all three paths blur equally hard. On the
CPU the sigma becomes the box radius whose three passes come nearest,
`(sqrt(4*sigma^2 + 1) - 1) / 2` rounded to the closer neighbor. The box passes average premultiplied
channels, so a transparent neighbor does not bleed its color.

A host window with `FLAG_SECURE` is captured only into an overlay window that is secure too, which a
Compose `Dialog` or `Popup` is by default. Otherwise the snapshot step is skipped, and a snapshot taken
before the host turned secure is dropped.

## Trade-offs

- **The snapshot is frozen unless asked to follow.** Content moving underneath is picked up on
  `refresh()`, or a couple of frames behind with `liveSnapshot`. A failed recapture keeps the
  snapshot on screen, unless the host resized and the old frame no longer lines up.
- **`FLAG_BLUR_BEHIND` is a window flag.** `SystemBlur` blurs everything behind the overlay window,
  `Snapshot` only what is behind the composable. Apply the modifier once per window, to a composable
  that fills it, to get the same picture from both.
- **The host is always the activity window.** Under a dialog opened over another dialog the snapshot
  captures the activity, not the dialog in between.

## Scope

Kotlin Multiplatform is not planned. On iOS Compose draws dialogs into the same view hierarchy and
`UIVisualEffectView` covers blur. Desktop has the boundary but nothing like `FLAG_BLUR_BEHIND` or
`PixelCopy`; only the box blur would port.
