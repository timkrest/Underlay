# Changelog

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Until 1.0.0 the public API — the surface
recorded in [`underlay/api/underlay.api`](underlay/api/underlay.api) — may change in any minor
version.

## [0.4.0] - 2026-09-12

### Added

- `liveSnapshot` on `blurredUnderlay`. The host window is captured again every time it draws, one
  capture in flight at a time, so a list scrolling under a popup shows through blurred a couple of
  frames behind instead of frozen. Off by default: it costs a `PixelCopy` and a blur per host frame
  for as long as the host keeps drawing. `SystemBlur` is live on its own and ignores it.

### Changed

- `:sample` opens its `Popup` without taking the touches outside it, so the grid scrolls under an
  open popup, and the panel carries a switch for the live snapshot.

## [0.3.1] - 2026-09-10

### Fixed

- `SystemBlur` was reported from the intent to set the window flag, not from the window taking it.
  A `Popup` whose root view had already left the window manager refused the update, and the overlay
  reported a system blur while the host stayed sharp behind it. A refusal now falls through to a
  snapshot, the same way a device with cross-window blur switched off does.
- On a host window whose width or height is not a multiple of four, `PixelCopy` captured the whole
  window into a bitmap that could not hold it at a flat quarter scale, while `decorView.draw()`
  scaled by exactly a quarter. Placement assumes the quarter, so on the `PixelCopy` path the
  backdrop was shifted, and the last one to three columns or rows of the window had none at all.
  Both paths now cover the largest exact quarter that fits.

### Changed

- `:sample` carries the radius slider inside the overlay card as well as in the panel: a `Dialog`
  and a `Popup` swallow the touches outside them, which left re-blurring an open overlay
  unreachable. It also reports whether the device has cross-window blur at all, rather than only
  pointing at Battery Saver once an overlay had already reached `SystemBlur`.

## [0.3.0] - 2026-09-05

### Added

- `UnderlayState.source`: the `UnderlaySource` the overlay carrying the state draws from, as Compose
  state. Null while no overlay carries it, so a badge outside the overlay clears on dismiss without
  hosting the value from `onSourceChange` by hand. The callback stays for callers without a state.

### Fixed

- The snapshot could spill up to three host pixels past the composable it backs, where its
  placement rounds out to whole snapshot pixels. It is now clipped to the composable.
- On API 31 and above with cross-window blur switched off, opening an overlay captured the host
  window twice: the initial callback of the blur listener repeated the choice `start()` had already
  made. The source is now chosen only when the system's answer changes.
- A detached node kept its `Activity`, `Configuration` and `WindowManager` until it was collected.

## [0.2.0] - 2026-08-29

### Added

- `UnderlayState` and `rememberUnderlayState()`, passed to `Modifier.blurredUnderlay` as `state`.
  `refresh()` captures the host window again, for content that moves underneath an open overlay.
  The snapshot on screen stays until the new one is ready, so a refresh does not flash.

### Changed

- `minSdk` is 23, down from 24 - the floor Compose itself declares. CI runs the instrumentation
  suite on API 23, 26, 28, 31 and 34.
- A capture waits for the frame it was asked from to reach the screen. `UnderlayState.refresh()`
  called right after changing the content underneath now captures that change rather than the
  frame before it.
- The CPU blur radius is the one whose three box passes land closest to the requested Gaussian.
  The previous SVG approximation rounded some radii down to no blur and undershot the rest by up
  to 29%.
- A snapshot that covers only part of the composable is drawn at its capture scale over that part
  instead of being stretched across the whole of it.
- The snapshot is blurred with `RenderEffect` on the GPU from API 31, where the CPU box passes used
  to run. `Snapshot` is reached when the system switches cross-window blur off, which is usually
  battery saver - the moment CPU is least available. Below API 31 the platform ignores
  `RenderEffect`, so the box blur stays.
- `blurredUnderlay` is a `Modifier.Node` element rather than a `@Composable` function, so the
  modifier can be hoisted into a `val` outside composition. Trailing-lambda call sites keep
  compiling; a call that passed `onSourceChange` as a fourth positional argument does not, because
  that position takes `state`. Code built against 0.1.0 needs a rebuild either way.

## [0.1.0] - 2026-08-26

### Added

- `Modifier.blurredUnderlay(blurRadius, tint, fallback, onSourceChange)` — a blurred backdrop for
  overlays rendered in their own window, on API 24 and above.
- Three-step degradation reported through `UnderlaySource`: `SystemBlur` (`FLAG_BLUR_BEHIND`, API
  31+), `Snapshot` (`PixelCopy` of the host window, `decorView.draw()` below API 26) and `Fallback`
  (a solid color). A fourth value, `Pending`, covers the frames where a capture is in flight: only
  the tint is drawn there, so an overlay never flashes an opaque fallback on its way to a snapshot.
- Live fall-through from `SystemBlur` to `Snapshot` when the system disables cross-window blur, via
  `WindowManager.addCrossWindowBlurEnabledListener`.
- Snapshot re-capture when the host window changes size or configuration. Changing only
  `blurRadius` re-blurs the pixels already captured rather than reading the window back again.
- `:sample` application covering all four window kinds: `Dialog`, `Popup`, a translucent activity
  and a plain in-window overlay.
- Instrumentation coverage of every rung of the ladder, run on API 24, 28, 31 and 34: the software
  `decorView` draw, `PixelCopy`, and `FLAG_BLUR_BEHIND`. The step each test asserts follows what the
  device offers, not the API level, so a device with cross-window blur switched off is covered too.

[Unreleased]: https://github.com/timkrest/Underlay/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/timkrest/Underlay/releases/tag/v0.4.0
[0.3.1]: https://github.com/timkrest/Underlay/releases/tag/v0.3.1
[0.3.0]: https://github.com/timkrest/Underlay/releases/tag/v0.3.0
[0.2.0]: https://github.com/timkrest/Underlay/releases/tag/v0.2.0
[0.1.0]: https://github.com/timkrest/Underlay/releases/tag/v0.1.0
