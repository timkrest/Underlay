# Security

Report a vulnerability through
[GitHub's private advisory form](https://github.com/timkrest/Underlay/security/advisories/new)
rather than a public issue. Fixes land in the latest release; older ones are not patched.

## What the library touches

Underlay reads pixels. On the `Snapshot` step it copies the activity window into a bitmap a quarter
of its size, blurs it and draws it behind your overlay. That bitmap is the screen as the user sees
it, including whatever the host window has on it at that moment, and it lives in your process while
the overlay is open. Nothing writes it to disk and nothing sends it anywhere. It is dropped when the
overlay leaves the composition.

The consequence worth knowing: a heap dump taken while an overlay is open contains a picture of the
screen behind it. If the host window shows card numbers or a one-time code, that picture is in the
dump too. `FLAG_BLUR_BEHIND` has no such property — on the `SystemBlur` step the blur happens in
SurfaceFlinger and the app never sees the pixels.

The library asks for no permissions, opens no connections and reads no storage. It captures only the
window of the activity it was called from, through `PixelCopy` or `decorView.draw()`, both of which
are scoped to that window by the platform. It cannot reach another app's window, and on a translucent
activity, where the window underneath belongs to another task, it captures nothing and draws the
fallback colour instead.
