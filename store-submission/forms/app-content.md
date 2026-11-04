# App content declarations (Play Console › Policy › App content)

| Declaration | Answer |
|---|---|
| Privacy policy | https://meridian11.com/privacy/awake |
| Ads | No, the app contains no ads |
| App access | All functionality is available without special access (no login) |
| Content rating | See `content-rating.md` |
| Target audience | 13 and over (13–15, 16–17, 18+). Not designed for children, so the Families policy doesn't apply |
| Data safety | See `data-safety.md` |
| News app | No |
| Government app | No |
| Financial features | My app doesn't provide any financial features |
| Health apps | No health features |
| Advertising ID | Not used (no ad or analytics SDKs; `AD_ID` isn't declared) |
| Permissions | `WAKE_LOCK`, `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE` (below), `POST_NOTIFICATIONS` (the notification of the running service), `RECEIVE_BOOT_COMPLETED` (resets the icon and the switch to off after a restart). No internet, no location, no accessibility service. |

## Foreground service declaration

Policy › App content › Foreground service permissions (appears after the first bundle is uploaded).

- **Type:** Special use (`FOREGROUND_SERVICE_SPECIAL_USE`), subtype declared in the manifest
  (`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`).
- **Feature:** Keep the device awake. The user turns Awake on (taskbar icon, its window, the Quick Settings tile or the
  widget); `KeepAwakeService` runs as a foreground service with a notification ("Turn off", "+30 min") and holds a wake
  lock for as long as the user chose (until turned off, or 30 minutes, 1 or 2 hours). It stops when the user turns it
  off, when the timer ends, when the lid closes or when the battery runs low (both guards on by default).
- **Why no other type fits:** keeping the device awake at the user's request is not media playback, location, data
  sync, a connected device, health or any other predefined type, and it has to keep running while the user works in
  other apps, which an activity can't.
- **User impact if deferred or interrupted:** the device goes to sleep in the middle of what the user wanted kept
  running (a download, a presentation, a build), the thing the user explicitly asked Awake to prevent.
- **Video:** an unlisted YouTube video recorded on a Googlebook: opening Awake from the taskbar, turning it on in its
  window, the service's notification, the Quick Settings tile, and turning it off from the notification. Link in the
  Play Console.
