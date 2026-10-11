# Underlay

[![Maven Central](https://img.shields.io/maven-central/v/com.timkrest/underlay?label=Maven%20Central)](https://central.sonatype.com/artifact/com.timkrest/underlay)
[![CI](https://github.com/timkrest/Underlay/actions/workflows/ci.yml/badge.svg)](https://github.com/timkrest/Underlay/actions/workflows/ci.yml)

[Русская версия](README.ru.md)

Blurred backdrop for dialogs and overlays that live in **their own window**, down to API 23.

| `Snapshot` behind a `Dialog` | `Fallback` in a translucent activity |
|---|---|
| <img src="docs/snapshot.png" width="300" alt="A dialog over a grid of tiles, its backdrop blurred from a snapshot of the activity window"> | <img src="docs/fallback.png" width="300" alt="A translucent activity drawing the solid fallback color, with no host window to capture"> |

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

## Why

A `Dialog` is a separate window. `Modifier.blur()` inside it blurs the dialog, not the activity
behind it. Libraries like haze can carry a Compose subtree you mark across that boundary; Underlay is
for when the screen behind is Views or code you don't own, or the overlay is a View `Dialog` or a
`PopupWindow`. It works on windows:
`FLAG_BLUR_BEHIND` on API 31+, a `PixelCopy` snapshot below that. Platform issue:
[Support window blur in compose dialogs](https://issuetracker.google.com/issues/296272625).

Background and trade-offs: [the article](docs/article-window-boundary.md) and
[how it works](docs/how-it-works.md).

## Install

```kotlin
dependencies {
    implementation("com.timkrest:underlay:0.4.0")
}
```

`minSdk 23`, Java 11 bytecode. Compose runtime and UI come in as `api`.

## Sources

| Source | When | What is drawn |
|--------|------|---------------|
| `SystemBlur` | API 31+, own window, cross-window blur on | The system blurs behind the window, the tint on top |
| `Snapshot` | A host window to capture | The host window, captured, downscaled ×4 and blurred, under the tint |
| `Pending` | A capture is in flight | The tint only |
| `Fallback` | No own window, no host window, or the capture failed | The `fallback` color |

Battery saver switches cross-window blur off; an open overlay then falls through to `Snapshot`.
Some devices never turn it on, Samsung Galaxy phones among them; there every overlay uses `Snapshot`.

## State and refresh

```kotlin
val underlay = rememberUnderlayState()

Box(
    modifier = Modifier.blurredUnderlay(
        blurRadius = 16.dp,
        tint = Color.Black.copy(alpha = 0.3f),
        fallback = MaterialTheme.colorScheme.surface,
        state = underlay,
        onSourceChange = { source -> Log.d("underlay", "$source") },
    ),
) {
    Text("${underlay.source}")
    Button(onClick = { underlay.refresh() }) { Text("Refresh") }
}
```

`Snapshot` is taken when the overlay opens, when the host resizes, on a configuration change (Compose
only; with Views call `refresh()`) and on `refresh()`. `liveSnapshot = true` captures the host again every time it draws, a couple of frames
behind, at a `PixelCopy` and a blur per host frame.

## Views

```kotlin
class FrostedDialogFragment : DialogFragment(R.layout.frosted_dialog) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.setDimAmount(0f)
        view.blurredUnderlay(
            blurRadius = 16f * resources.displayMetrics.density,
            tint = 0x4D000000,
            fallback = requireContext().getColor(R.color.surface),
        )
    }
}
```

The underlay becomes the view's background, with the radius in pixels. It returns a `ViewUnderlay`
with `source`, `refresh()`, setters and `remove()`, which puts the previous background back. Call it
on a view of your own, not on a `decorView` or the sheet of a `BottomSheetDialog`.

## Compatibility

The public API is recorded in [`underlay/api/underlay.api`](underlay/api/underlay.api) and checked on
every build. A patch release never breaks anything; until 1.0.0 a minor release may, and the
changelog says so. Kotlin mangles signatures that take a `Dp` or a `Color`, so a new parameter can
need a rebuild even when your code still compiles.

## Sample

One overlay per window kind over a grid of hardware bitmaps, with the active source and a radius
slider:

```bash
./gradlew :sample:installDebug
```

## Contributing

[CONTRIBUTING.md](CONTRIBUTING.md). The most useful report is which source your device reached.
Contributions are covered by the [CLA](CLA.md).

## License

Apache 2.0, see [LICENSE](LICENSE). Vulnerabilities go through [SECURITY.md](SECURITY.md).
