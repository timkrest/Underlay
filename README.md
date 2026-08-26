# Underlay

[![Maven Central](https://img.shields.io/maven-central/v/com.timkrest/underlay?label=Maven%20Central)](https://central.sonatype.com/artifact/com.timkrest/underlay)
[![CI](https://github.com/timkrest/Underlay/actions/workflows/ci.yml/badge.svg)](https://github.com/timkrest/Underlay/actions/workflows/ci.yml)

[Русская версия](README.ru.md)

Blurred backdrop for dialogs and overlays that live in **their own window**, down to API 24.

| `Snapshot` behind a `Dialog` | `Fallback` in a translucent activity |
|---|---|
| <img src="docs/snapshot.png" width="300" alt="A dialog over a grid of tiles, its backdrop blurred from a snapshot of the activity window"> | <img src="docs/fallback.png" width="300" alt="A translucent activity drawing the solid fallback color, with no host window to capture"> |

Both from `:sample` on one device. The badge names the step the modifier reached.

```kotlin
Dialog(onDismissRequest = ::dismiss) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blurredUnderlay(
                blurRadius = 16.dp,
                tint = Color.Black.copy(alpha = 0.3f),
                fallback = MaterialTheme.colorScheme.surface,
            ),
    ) {
        // dialog content
    }
}
```

## Why this exists

Compose blur libraries — [haze](https://github.com/chrisbanes/haze),
[Cloudy](https://github.com/skydoves/Cloudy), [imla](https://github.com/desugar-64/imla) — blur a
composable subtree they can see: you mark a source with a modifier, they capture it into a
`GraphicsLayer` and blur that layer.

A `Dialog` is not in that subtree. It renders in a **separate window**, and the content behind it
belongs to the activity's window — a different render node tree entirely. No amount of marking the
screen as a blur source reaches across that boundary. The platform gap is tracked upstream as
[Support window blur in compose dialogs](https://issuetracker.google.com/issues/296272625).

Underlay fills that gap, and only that gap. For live blur of content inside your own window, use
haze or Cloudy — the two compose well: Underlay supplies the backdrop, haze the live effect on top.

## How it degrades

Three steps, best available one wins:

| Step | Requires | What happens |
|------|----------|--------------|
| `SystemBlur` | API 31+, own window | `FLAG_BLUR_BEHIND` + `blurBehindRadius`. Live, GPU, free. Only the tint is drawn on top. |
| `Snapshot` | API 24+, reachable host window | One `PixelCopy` of the host window (`decorView.draw()` below API 26), downscaled ×4, box-blurred on `Dispatchers.Default`. |
| `Fallback` | — | Solid `fallback` color. |

A capture crosses a frame boundary, a `PixelCopy` and a background blur before it lands. Until it
does, the modifier reports `Pending` and draws only the tint, so the overlay settles from unblurred
host content into the blurred snapshot instead of flashing an opaque `fallback` in between.

Step 1 is not a one-time check. The system silently disables cross-window blur in battery saver
mode, so Underlay subscribes to `WindowManager.addCrossWindowBlurEnabledListener`, drops the window
flag and falls through to the snapshot when that happens.

The snapshot path avoids hardware bitmaps. `decorView.draw()` into a software canvas throws
`Software rendering doesn't support hardware bitmaps` as soon as anything on screen was decoded by
an image loader into one, which on API 26+ is the default for Coil and Glide. Hence `PixelCopy`
wherever it exists.

## Windows

Underlay resolves two windows separately: the one the caller lives in, and the host window
underneath it. Both have to line up before anything is blurred.

| Called from | Own window | Host window | Best step |
|-------------|------------|-------------|-----------|
| `Dialog` | dialog window | activity window | `SystemBlur` |
| `Popup`, `PopupWindow` | root view added through `WindowManager` | activity window | `SystemBlur` |
| Translucent or floating activity | activity window | none — it belongs to another task | `SystemBlur` |
| Plain overlay inside the activity | none | none | `Fallback` |

The last row has nothing to draw: without an own window the host window *is* the caller's own
window, so capturing it would fold the overlay into its own backdrop. Underlay draws `fallback`.

## Reporting the active step

```kotlin
Modifier.blurredUnderlay(
    blurRadius = 16.dp,
    tint = Color.Black.copy(alpha = 0.3f),
    fallback = MaterialTheme.colorScheme.surface,
    onSourceChange = { source -> Log.d("underlay", "step: $source") },
)
```

`UnderlaySource` is `SystemBlur`, `Snapshot`, `Pending` or `Fallback`. The callback fires on every
change, including the fall-through when the system turns cross-window blur off.

## Install

```kotlin
dependencies {
    implementation("com.timkrest:underlay:0.1.0")
}
```

`minSdk 24`, Java 11 bytecode. The dependencies are Compose runtime and UI (exposed as `api`),
`activity-compose`, `core-ktx`, `annotation` and `kotlinx-coroutines-core`.

## Trade-offs

- **The snapshot is frozen.** It captures the host window once, and again when that window changes
  size or configuration. Content moving underneath is not reflected. For a modal overlay that is the
  point; for a transient popup over a scrolling list you want an in-window library instead. A
  re-capture that fails keeps the snapshot already on screen rather than dropping to `fallback` —
  unless the host resized, where the old frame no longer lines up and `fallback` takes over until a
  capture succeeds.
- **`FLAG_BLUR_BEHIND` is a window flag.** Step 1 blurs everything behind the overlay's whole
  window; step 2 blurs only what is behind the composable. Apply the modifier to a composable that
  fills the overlay window to get the same picture from both.
- **`blurRadius` is a Gaussian sigma**, the same meaning Figma and `RenderEffect.createBlurEffect`
  give it. The snapshot converts it to a box-blur radius with the three-box approximation from the
  SVG filter specification, `box size = sigma * 3 * sqrt(2*pi) / 4`, taken on the ×4 downscale.
- **The host is always the activity window.** Under stacked overlays — a dialog opened over another
  dialog — step 2 snapshots the activity, not the dialog in between; step 1 blurs everything behind
  the window and does not have this seam.

## Sample

`:sample` opens one overlay per window kind — Compose `Dialog`, Compose `Popup`, translucent
activity, plain in-window overlay — over a grid of hardware bitmaps, shows which step is active, and
lets you drag the radius. With `SystemBlur` active it points at Battery Saver, which is the way to
watch the fall-through to `Snapshot` on a real device.

```bash
./gradlew :sample:installDebug
```

## Scope

Kotlin Multiplatform is not planned. The gap this library exists for is Android's: on iOS, Compose
draws dialogs into the same view hierarchy, so there is no window boundary to cross and
`UIVisualEffectView` already covers blur. Desktop does have the boundary, but nothing in common with
`FLAG_BLUR_BEHIND` or `PixelCopy` — it would be a second implementation, not shared code. The only
portable code here is the box blur.

## License

Apache 2.0 — see [LICENSE](LICENSE).
