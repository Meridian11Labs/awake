# Awake

**Awake** keeps your **Googlebook** from going to sleep while you want it awake. Click the coffee cup in the taskbar and
the screen stays on; click it again and your Googlebook sleeps as usual. Made by **Meridian11 LLC**. Package
`com.meridian11.books.awake`.

## What it does

- **One switch, four places.** The taskbar icon, Awake's own window, a Quick Settings tile, and a desktop widget all
  turn it on and off and always show the same state. The launcher icon itself changes: a full cup while Awake is on,
  a half-empty one when it is off.
- **For as long as you choose:** Infinite (until you turn it off), or 30 minutes, 1 hour, or 2 hours, with a live
  countdown across the window, widget, and ongoing notification (plus Turn Off and +30m actions on timed sessions).
- **Guards:** automatically turns off when the battery drops to or below 20% unplugged and when the laptop lid closes
  without an external monitor attached (both on by default); includes an option to let the screen dim while keeping the
  Googlebook awake.
- **Quick to reach:** right-click the taskbar icon for its shortcuts, or set the icon to switch Awake at once without
  opening the window. The widget starts as a 1×1 squircle and reveals live countdown, timer presets, and Settings when
  resized larger.

How it keeps the Googlebook awake: a foreground service (`specialUse`) holding CPU and display wake locks, renewed
in-place with bounded safety timeouts and `AlarmManager` timer expiration. No accessibility service, no internet
permission.

## Privacy

Awake has no internet permission, no ads, no accounts, and collects nothing; cloud backup is disabled so settings stay
strictly on your Googlebook and are deleted on uninstall. Privacy policy: https://meridian11.com/privacy/awake. Terms:
[TERMS_OF_SERVICE.md](TERMS_OF_SERVICE.md).

## Build and test

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

Releases come from a `v*` tag (`.github/workflows/release.yml`): signed in GitHub Actions, published as a GitHub
release, the bundle kept for Google Play. The store listing, its pictures, and the Play Console answers are kept
privately, outside this repo.

Copyright © 2026 Meridian11 LLC (meridian11labs@gmail.com). All rights reserved.
