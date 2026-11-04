# Awake

**Awake** keeps an Android laptop (a Googlebook, or Android in desktop mode) from going to sleep while you want it
awake. Click the coffee cup in the taskbar and the screen stays on; click it again and the laptop sleeps as usual.
Made by **Meridian11 LLC**. Package `com.meridian11.books.awake`.

## What it does

- **One switch, four places.** The taskbar icon, Awake's own window, a Quick Settings tile and a desktop widget all
  turn it on and off and always show the same state. The launcher icon itself changes: a full cup while Awake is on,
  a half-empty one when it is off.
- **For as long as you choose:** until you turn it off, or 30 minutes, 1 hour or 2 hours. A notification shows that
  Awake is on, with Turn off and +30 minutes.
- **Guards:** off when the battery drops below 20% unplugged and when the lid closes (both on by default); on when the
  charger is connected (off by default); an option to let the screen dim while the device stays awake.
- **Quick to reach:** right-click the taskbar icon for its shortcuts, or set the icon to switch Awake at once without
  opening the window. The widget starts as one square and shows the timer and settings when made bigger.

How it keeps the device awake: a foreground service (`specialUse`) holding a wake lock, renewed while it runs. No
accessibility service, no internet permission.

## Privacy

Awake has no internet permission, no ads, no accounts, and collects nothing; its settings stay on the device.
Privacy policy: https://meridian11.com/privacy/awake. Terms: [TERMS_OF_SERVICE.md](TERMS_OF_SERVICE.md).

## Build and test

```bash
./gradlew testDebugUnitTest assembleDebug
```

Releases come from a `v*` tag (`.github/workflows/release.yml`): signed in GitHub Actions, published as a GitHub
release, the bundle kept for Google Play. The store kit and the Play Console answers are in
[store-submission/](store-submission/README.md).

Copyright © 2026 Meridian11 LLC (meridian11labs@gmail.com). All rights reserved.
