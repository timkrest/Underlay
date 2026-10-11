# Contributing

For a device bug, include the device, Android version, your `minSdk`, the output of
`adb shell dumpsys window | grep mBlurEnabled`, and the source `onSourceChange` reported.

## Building

Needs JDK 21 and an Android SDK.

```
./gradlew build
```

This runs ktlint, Android Lint, the unit tests and the API check against
[`underlay/api/underlay.api`](underlay/api/underlay.api), and assembles both modules.

Instrumentation tests need a device or an emulator:

```
./gradlew :underlay:connectedDebugAndroidTest
```

CI runs them on API 23, 26, 28, 31, 34 and 36: `decorView.draw()` below 26, `PixelCopy` from 26,
`FLAG_BLUR_BEHIND` from 31, plus the newest level. Tests check the source the device actually
offers, so a device with cross-window blur turned off passes too.

The sample app: `./gradlew :sample:installDebug`.

## Pull requests

- Run `./gradlew build`.
- New files under `src/` start with the SPDX header the others carry
  (`.github/scripts/check-license-headers.sh`).
- After changing a public declaration, run `./gradlew apiDump` on its own and commit the `.api`
  file. A new `Dp` or `Color` parameter changes the mangled name, so the dump shows a removal and an
  addition.
- Removing a released declaration needs a minor version bump before 1.0.0 and a major one after
  (`.github/scripts/check-api-compatibility.sh`).
- Add an entry under `## [Unreleased]` in [CHANGELOG.md](CHANGELOG.md) for anything a caller would
  notice.

Contributions fall under the [CLA](CLA.md). A bot asks you to sign it on your first pull request.

## Releasing

1. In one commit: move the `Unreleased` entries under `## [x.y.z] - yyyy-mm-dd`, add the link at
   the bottom of the changelog, set `VERSION_NAME` in `gradle.properties`, and update the version
   in both READMEs and both articles.
2. `git tag -a vx.y.z -m "Underlay x.y.z" && git push origin vx.y.z`.
3. The release workflow reruns CI and publishes the tag to Maven Central without staging. It fails
   if the tag doesn't match `VERSION_NAME`, the changelog has no entry, or the commit isn't on
   `main`.
