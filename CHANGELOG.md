# Changelog

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Until 1.0.0 the public API — the surface
recorded in [`underlay/api/underlay.api`](underlay/api/underlay.api) — may change in any minor
version.

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

[0.1.0]: https://github.com/timkrest/Underlay/releases/tag/v0.1.0
