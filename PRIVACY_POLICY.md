# Awake privacy policy

The published policy is at **https://meridian11.com/privacy/awake**; this copy says the same.

Last updated November 11, 2026. For the Awake app for Googlebooks (`com.meridian11.books.awake`), made and published on
Google Play by Meridian11 LLC.

## The short version

Awake keeps your Googlebook from going to sleep while you want it awake. It has no internet permission, no accounts, no
ads, analytics, or trackers, and it collects and shares nothing. Its settings stay strictly on your device, and
Meridian11 LLC receives nothing from it.

## Permissions and what they are for

- **Keep the device awake (`WAKE_LOCK`):** Holding the screen and processor awake while you have Awake turned on, and
  releasing the lock immediately when you turn it off, when its timer ends, or when a guardrail triggers.
- **Foreground service (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`):** Staying active while it keeps your
  Googlebook awake across windows, with an ongoing status notification that shows it is on and lets you turn it off in
  one click.
- **Notifications (`POST_NOTIFICATIONS`, optional):** Showing that ongoing status notification, with its **Turn Off**
  and **+30m** buttons. Awake still works if you decline notifications.
- **Run at startup (`RECEIVE_BOOT_COMPLETED`):** Resetting the app icon and switch to off after your Googlebook restarts
  so the icon always matches normal sleep on a fresh boot (and preserving an active session across app updates).

Awake can turn itself off when the battery runs low unplugged or when the laptop lid closes; you choose which of these
guardrails are active in its settings. It learns of them from Android's standard local hardware broadcasts, which need
no special permission and are never stored or sent anywhere.

## What Awake keeps

Your settings (whether Awake is on, the timer preset you chose, and your guardrail options) stay strictly in Awake's
private app storage on your Googlebook. Cloud backup and device-to-device transfer are disabled (`allowBackup="false"`),
so your preferences and session state are never uploaded to any cloud backup. Uninstalling Awake permanently deletes all
of its data from your Googlebook.

## Google Play

If you install Awake from Google Play, Google Play gives Meridian11 LLC aggregate statistics such as install counts, and
reports of crashes and freezes from people who chose in their device settings to share diagnostics with app developers.
These come from Google Play, not from Awake; they do not identify you, and Google's privacy policy covers them.

## Children

Awake is not directed at children under 13, and Meridian11 LLC receives nothing from anyone who uses it.

## Changes and contact

Changes to this policy are published at https://meridian11.com/privacy/awake, with a new date at the top. Questions:
email [meridian11labs@gmail.com](mailto:meridian11labs@gmail.com).

© 2026 Meridian11 LLC
