# Security

Report vulnerabilities through
[GitHub's private advisory form](https://github.com/timkrest/Underlay/security/advisories/new),
not a public issue. Only the latest release gets fixes.

## What the library touches

On the `Snapshot` source, Underlay copies the activity window into a bitmap at a quarter of its
width and height, blurs it and draws it behind the overlay. The bitmap holds whatever the screen
shows and stays in memory while the overlay is open. It is never written to disk or sent anywhere,
and is dropped when the overlay leaves the composition or, for `View.blurredUnderlay`, detaches
from its window.

A heap dump taken while an overlay is open contains that picture, including card numbers or
one-time codes on the screen behind it. The `SystemBlur` source doesn't have this problem: the blur
runs in SurfaceFlinger and the app never sees the pixels.

A host window with `FLAG_SECURE` is drawn only into an overlay window that is also secure, so its
content never shows up in screenshots or screen recordings. A snapshot taken before the host turned
secure is dropped.

The library needs no permissions, makes no network calls and reads no storage. It captures only
the window of its own activity, through `PixelCopy` or `decorView.draw()`, and cannot reach other
apps' windows. On a translucent activity, where the window underneath belongs to another activity,
it captures nothing and draws the fallback color.
