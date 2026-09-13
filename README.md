# Underlay

[![Maven Central](https://img.shields.io/maven-central/v/com.timkrest/underlay?label=Maven%20Central)](https://central.sonatype.com/artifact/com.timkrest/underlay)
[![CI](https://github.com/timkrest/Underlay/actions/workflows/ci.yml/badge.svg)](https://github.com/timkrest/Underlay/actions/workflows/ci.yml)

[Русская версия](README.ru.md)

Blurred backdrop for dialogs and overlays that live in **their own window**, down to API 23.

| `Snapshot` behind a `Dialog` | `Fallback` in a translucent activity |
|---|---|
| <img src="docs/snapshot.png" width="300" alt="A dialog over a grid of tiles, its backdrop blurred from a snapshot of the activity window"> | <img src="docs/fallback.png" width="300" alt="A translucent activity drawing the solid fallback color, with no host window to capture"> |

Both from `:sample` on one device.

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

A `Dialog` renders in a **separate window**: the content behind it belongs to the activity's window,
a different render node tree. `Modifier.blur()` inside the dialog blurs the dialog, not that content.

Backdrop libraries do cross that boundary — haze records a subtree you mark with `hazeSource` and
replays it under the effect,
[dialogs included](https://chrisbanes.github.io/haze/latest/core-concepts/#dialogs). That needs the
background to be Compose you own and can mark.

Underlay is for when it isn't: the screen behind is Views or code you don't own, or your overlay is a
`PopupWindow` rather than a subcomposition of it. Underlay asks nothing of the composition and works
one level down, on windows — `FLAG_BLUR_BEHIND` on API 31+, a `PixelCopy` snapshot below that. The
platform itself still has no answer here:
[Support window blur in compose dialogs](https://issuetracker.google.com/issues/296272625).

The two combine: Underlay draws the backdrop from the other window, an in-window library the live
effect on top.

The boundary, the two ways around it and what each one costs, at length:
[the article](docs/article-window-boundary.md).

## How it degrades

The backdrop comes from the best source the device offers:

| Source | Requires | What happens |
|--------|----------|--------------|
| `SystemBlur` | API 31+, own window | `FLAG_BLUR_BEHIND` + `blurBehindRadius`. Live, on the GPU, free. Only the tint is drawn on top. |
| `Snapshot` | API 23+, reachable host window | One `PixelCopy` of the host window (`decorView.draw()` below API 26), downscaled ×4, then blurred: `RenderEffect` on the GPU from API 31, box passes on `Dispatchers.Default` below it. |
| `Fallback` | — | Solid `fallback` color. |

A capture takes a frame boundary, a `PixelCopy` and a blur. Until it lands the modifier reports a
fourth value, `Pending`, and draws only the tint, so the overlay settles from clear host content
into the blurred snapshot instead of flashing an opaque `fallback`.

The system switches cross-window blur off in battery saver. Underlay listens for that through
`WindowManager.addCrossWindowBlurEnabledListener`, drops the window flag and falls through to the
snapshot.

The snapshot uses `PixelCopy` wherever it exists. `decorView.draw()` into a software canvas throws
`Software rendering doesn't support hardware bitmaps` as soon as an image loader has decoded
anything on screen into one, which is the Coil and Glide default from API 26.

## Windows

Underlay resolves two windows: the one the caller lives in, and the host window underneath.

| Called from | Own window | Host window | Best source |
|-------------|------------|-------------|-------------|
| `Dialog` | dialog window | activity window | `SystemBlur` |
| `Popup`, `PopupWindow` | root view added through `WindowManager` | activity window | `SystemBlur` |
| Translucent or floating activity | activity window | none — it belongs to another task | `SystemBlur` |
| Plain overlay inside the activity | none | none | `Fallback` |

Without an own window the host window *is* the caller's own window, so capturing it would fold the
overlay into its own backdrop. That row draws `fallback`.

A translucent activity has no host window either, so it drops from `SystemBlur` straight to
`Fallback` where a `Dialog` or a `Popup` would drop to `Snapshot`.

## Reporting the active source

```kotlin
Modifier.blurredUnderlay(
    blurRadius = 16.dp,
    tint = Color.Black.copy(alpha = 0.3f),
    fallback = MaterialTheme.colorScheme.surface,
    onSourceChange = { source -> Log.d("underlay", "source: $source") },
)
```

`UnderlaySource` is `SystemBlur`, `Snapshot`, `Pending` or `Fallback`. The callback fires on every
change, including the fall-through when the system turns cross-window blur off.

An `UnderlayState`, introduced in the next section, carries the same value as Compose state:
`underlay.source` is the active `UnderlaySource`, or null while no overlay carries the state.

## Refreshing the snapshot

`Snapshot` freezes the host window. To capture it again while the overlay is open, hand the modifier
an `UnderlayState`:

```kotlin
val underlay = rememberUnderlayState()

Box(
    modifier = Modifier.blurredUnderlay(
        blurRadius = 16.dp,
        tint = Color.Black.copy(alpha = 0.3f),
        fallback = MaterialTheme.colorScheme.surface,
        state = underlay,
    ),
) {
    Button(onClick = { underlay.refresh() }) { Text("Refresh") }
}
```

The snapshot on screen stays until the new one is ready, so a refresh does not flash. `SystemBlur`
blurs live and ignores the state.

For content that keeps moving, `liveSnapshot = true` captures the host window again every time it
draws, one capture in flight at a time, so a list scrolling under a popup shows through a couple of
frames behind. It costs a `PixelCopy` and a blur per host frame for as long as the host keeps
drawing, which is why it is off by default.

## Install

```kotlin
dependencies {
    implementation("com.timkrest:underlay:0.4.0")
}
```

`minSdk 23`, the floor Compose itself declares, and Java 11 bytecode. Dependencies: Compose runtime
and UI, exposed as `api`, plus `activity-compose`, `core-ktx`, `annotation` and
`kotlinx-coroutines-core`.

## Compatibility

The public surface is recorded in [`underlay/api/underlay.api`](underlay/api/underlay.api) and
checked on every build.

- **A patch release never breaks anything**, in source or in binary.
- **Until 1.0.0 a minor release may**, and says so in the changelog. CI refuses a release that drops
  a declaration without leaving the series it was published in.
- **Kotlin mangles every signature taking a `Dp` or a `Color`** — `blurredUnderlay-6Ivg_Sk`. Adding
  a parameter renames it, so a release can keep your call sites compiling and still need a rebuild.
  A release that needs the call site itself edited says so in the changelog.

## Trade-offs

- **The snapshot is frozen unless asked to follow.** It captures the host window once, again when
  that window changes size or configuration, and on `UnderlayState.refresh()`. Content moving
  underneath is not picked up on its own; `liveSnapshot` picks it up a couple of frames behind, at
  the price of a capture per host frame. A failed re-capture keeps the snapshot on screen, unless
  the host resized and the old frame no longer lines up — then the snapshot is dropped and only the
  tint is drawn until a capture succeeds, and `fallback` only if none does.
- **`FLAG_BLUR_BEHIND` is a window flag.** `SystemBlur` blurs everything behind the overlay window;
  `Snapshot` blurs only what is behind the composable. Apply the modifier to a composable that fills
  the overlay window to get the same picture from both.
- **`blurRadius` is a Gaussian sigma**, the meaning Figma and `RenderEffect.createBlurEffect` give
  it, taken on the ×4 downscale in `Snapshot`. On the CPU it becomes the box radius whose three
  passes land nearest, `radius = (sqrt(4*sigma^2 + 1) - 1) / 2` rounded to the closer neighbour.
- **The host is always the activity window.** Under a dialog opened over another dialog, `Snapshot`
  captures the activity, not the dialog in between. `SystemBlur` has no such seam.

## Sample

`:sample` opens one overlay per window kind — `Dialog`, `Popup`, translucent activity, plain
in-window overlay — over a grid of hardware bitmaps, reports the active source and lets you drag the
radius. It also reports whether the device has cross-window blur at all, and points at Battery Saver,
which triggers the fall-through to `Snapshot` on a device that does.

```bash
./gradlew :sample:installDebug
```

## Scope

Kotlin Multiplatform is not planned. The gap this library covers is Android's: on iOS Compose draws
dialogs into the same view hierarchy, so there is no window boundary and `UIVisualEffectView`
already covers blur. Desktop has the boundary but nothing in common with `FLAG_BLUR_BEHIND` or
`PixelCopy` — a second implementation, not shared code. Only the box blur is portable.

## Contributing

[CONTRIBUTING.md](CONTRIBUTING.md) covers building, the instrumentation tests and what a pull
request needs. The most useful report is which step the ladder reached on your device. Contributions
are covered by the [CLA](CLA.md), signed by a comment on your first pull request.

## License

Apache 2.0 — see [LICENSE](LICENSE). Vulnerabilities go through [SECURITY.md](SECURITY.md), not an
issue.
