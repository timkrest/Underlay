# Contributing

Bug reports, ideas and pull requests are all welcome.

One report is worth more than the rest: which step the ladder reached on your device. The choice is
made on the user's phone, not from the API level, so an issue with your `minSdk`, the manufacturer,
what `adb shell dumpsys window | grep mBlurEnabled` answered and what `onSourceChange` reported says
something no test here can.

## Building

```
./gradlew build
```

That compiles both modules, runs the unit tests, applies ktlint, checks the public API against
[`underlay/api/underlay.api`](underlay/api/underlay.api) and assembles the library and the sample.
It needs JDK 21 and an Android SDK.

Everything the library actually does — reading another window, the system flag, the blur itself —
needs a device or an emulator:

```
./gradlew :underlay:connectedDebugAndroidTest
```

CI runs those on API 23, 26, 28, 31 and 34: the levels where the ladder changes shape.
`decorView.draw()` is all there is below 26, `PixelCopy` arrives at 26, `FLAG_BLUR_BEHIND` at 31.
The tests assert the step the device offers rather than the one its API level suggests, so an
emulator or a phone with cross-window blur switched off is covered too.

To try a change by hand:

```
./gradlew :sample:installDebug
```

## Before opening a pull request

- Run `./gradlew build`.
- Start any new file under a source set with the two-line SPDX header the others carry.
  `.github/scripts/check-license-headers.sh` is the same check CI runs.
- If you changed a public declaration, run `./gradlew apiDump` and commit the updated `.api` file.
  It has to be a separate invocation, because Gradle refuses to write and verify the same dump in
  one run. Kotlin mangles the name of anything taking a `Dp` or a `Color`, so adding a parameter to
  `blurredUnderlay` shows up in the dump as one declaration leaving and another arriving.
- Add a line under `## [Unreleased]` in [CHANGELOG.md](CHANGELOG.md) for anything a caller would
  notice.

Contributions are covered by the [CLA](CLA.md); a bot asks you to sign it on your first pull
request.

## Releasing

For maintainers:

1. Move the `Unreleased` entries under a `## [x.y.z] - yyyy-mm-dd` heading, add the matching link at
   the bottom of the changelog, set `VERSION_NAME` in `gradle.properties`, and point the dependency
   snippets in both READMEs at the new version. Same commit.
2. Tag it: `git tag -a vx.y.z -m "Underlay x.y.z" && git push origin vx.y.z`. The tag is what the
   release workflow publishes; `VERSION_NAME` only feeds the compatibility check on `main`.
3. The workflow re-runs the checks and publishes to Maven Central, released rather than staged. A
   removed declaration fails it unless the version left its release series — until 1.0.0 that means
   a minor bump.
