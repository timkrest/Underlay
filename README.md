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

A `Dialog` is a separate window, so `Modifier.blur()` inside it can't reach the activity behind
([platform issue](https://issuetracker.google.com/issues/296272625)). Underlay blurs that window:
`FLAG_BLUR_BEHIND` on API 31+, a blurred snapshot otherwise. Unlike haze, it needs no marked Compose
subtree: the screen behind can be Views or code you don't own, and the overlay a View `Dialog` or a
`PopupWindow`.

Details: [how it works](docs/how-it-works.md), [the article](docs/article-window-boundary.md).

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

Battery saver turns cross-window blur off, moving open overlays to `Snapshot`. Samsung Galaxy phones
and some other devices lack it and always use `Snapshot`.

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

`Snapshot` is retaken when the host resizes, on `refresh()` and, in Compose, on a configuration
change (with Views, call `refresh()`). `liveSnapshot = true` retakes it on every host frame, a
couple of frames behind, for a capture and a blur per frame.

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

The underlay becomes the view's background, radius in pixels. `ViewUnderlay` exposes `source`,
`refresh()`, setters and `remove()`, which restores the old background. Call it on your own view,
not a `decorView` or a `BottomSheetDialog` sheet.

## Limits

- Apply the modifier once per window, to a composable that fills it: `SystemBlur` covers the whole
  window, `Snapshot` only the composable.
- The snapshot is always of the activity window, even under a dialog opened over another dialog.
- A translucent activity has no host window: without `SystemBlur` it draws `fallback`. An overlay
  inside the activity window always does; use an in-window blur library there.
- A `FLAG_SECURE` host is captured only into a secure overlay window; otherwise `Snapshot` gives way
  to `Fallback`. Compose `Dialog` and `Popup` inherit the flag by default.

## Compatibility

Every build checks the public API against [`underlay/api/underlay.api`](underlay/api/underlay.api).
Patch releases don't break it; before 1.0.0 a minor release may, and the changelog says so. A new
parameter on a function taking `Dp` or `Color` changes its mangled JVM name and needs a rebuild.

## Sample

One overlay per window kind, with the active source and a radius slider:

```bash
./gradlew :sample:installDebug
```

## Contributing

[CONTRIBUTING.md](CONTRIBUTING.md). The most useful report is which source your device reached.
Contributions are covered by the [CLA](CLA.md).

## License

Apache 2.0, see [LICENSE](LICENSE). Vulnerabilities go through [SECURITY.md](SECURITY.md).
