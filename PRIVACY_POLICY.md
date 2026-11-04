# Privacy Policy — Awake for Googlebooks & Android Desktop

**Effective Date:** November 4, 2026  
**Application Name:** Awake (`com.meridian11.books.awake`)  
**Owner & Data Controller:** **Meridian11 LLC**  
**Contact Email:** [meridian11labs@gmail.com](mailto:meridian11labs@gmail.com)

---

## 1. Overview & Zero Data Collection Guarantee

**Awake** is designed, built, and owned by **Meridian11 LLC** with a strict **zero-data-collection, zero-network** architecture:

- **No Internet Permission:** Awake does **not** request `android.permission.INTERNET` or any network access permission in its `AndroidManifest.xml`. It is technically incapable of transmitting any information off your device.
- **Zero Personal or Sensitive Data Collected:** Awake does **not** collect, store, transmit, share, or sell any personal data, device identifiers, IP addresses, usage telemetry, crash analytics, or location data to Meridian11 LLC or any third party.
- **Zero Third-Party SDKs or Trackers:** Awake contains no advertising SDKs, analytics trackers, or external cloud libraries.

---

## 2. Local-Only Device Preferences (`Jetpack DataStore`)

Awake stores your functional preferences strictly locally on your device using Android Jetpack DataStore (`awake_preferences`), including:
- Current Keep-Awake state (`isAwake`) and active countdown expiration timestamp
- Selected Keep-Awake duration preset (`Infinite`, `30m`, `1h`, or `2h`)
- Smart Googlebook Guardrail toggles (Backpack Safety lid-close auto-shutoff, Low-Battery 20% cutoff, Plug-in Brew AC charger auto-awake, and screen dimming preference)
- Direct taskbar icon tap-to-toggle preference
- Optional Simulate Tap / Click Fallback state, interval (`15s`–`120s`), and in-app Accessibility Prominent Disclosure consent status

All local preference data remains exclusively inside your device's private application sandbox and is permanently deleted when you clear app data or uninstall Awake.

---

## 3. Android AccessibilityService API Disclosure (`BIND_ACCESSIBILITY_SERVICE`)

On enterprise-managed Googlebooks, Chromebooks, and Android Desktop configurations where system power policies restrict standard `PowerManager.WakeLock` flags from keeping the display awake, Awake offers an **optional, user-initiated** fallback powered by Android's `AccessibilityService` API (`com.meridian11.books.awake.service.KeepAwakeAccessibilityService`).

### How Awake Uses the AccessibilityService API:
Only after you explicitly read and click **"Agree & Continue"** on Awake's standalone in-app Prominent Disclosure modal AND enable the service in Android System Accessibility Settings:
1. **1×1 Keep-Screen-On Window Overlay (`TYPE_ACCESSIBILITY_OVERLAY`):** While Keep-Awake is active, Awake attaches an invisible 1×1 pixel window with `FLAG_KEEP_SCREEN_ON` so the display does not sleep.
2. **Optional Periodic Idle Micro-Tap (`dispatchGesture`):** If you explicitly turn on **"Simulate Tap / Click Fallback"**, Awake dispatches a 1-millisecond stationary micro-tap gesture at screen coordinate `(1, 1)` every `15` to `120` seconds while Keep-Awake is active.

### Strict Accessibility Privacy & Security Safeguards:
- **`canRetrieveWindowContent="false"`:** Awake is configured at the OS manifest level so that it **cannot** inspect or retrieve window hierarchies or screen text.
- **`accessibilityEventTypes=""`:** Awake subscribes to **zero** accessibility events. It never observes your screen, never reads notifications, never logs keystrokes, and never monitors your activity in other apps.
- **`isAccessibilityTool="false"`:** Awake transparently declares that it is a user-initiated keep-awake utility rather than a disability screen reader.
- **Hard In-App Consent Gate & Instant Revocation:** Even if enabled in Android System Settings, Awake's `AccessibilityService` performs zero actions unless you have accepted the in-app Prominent Disclosure. You can revoke this consent at any time inside **Awake → Accessibility Disclosure → Revoke Accessibility Consent & Disable Fallback**, which immediately detaches any overlay and stops gesture dispatch.

---

## 4. Foreground Service (`specialUse`) & Hardware Guardrail Sensors

When you toggle Awake ON, `KeepAwakeService` runs as a user-visible Android Foreground Service (`foregroundServiceType="specialUse"`) displaying a persistent status notification with a one-tap **"Turn Off"** action. While active, it:
- Holds a local `PowerManager.WakeLock` to prevent processor and display sleep.
- Monitors device battery level (`ACTION_BATTERY_CHANGED`), AC power connection, and display/lid state (`DisplayManager` / `ACTION_SCREEN_OFF`) strictly in memory on your device to automatically turn off Keep-Awake when your laptop lid closes or when battery drops below 20% unplugged.

No sensor or battery state information is ever logged to disk or transmitted anywhere.

---

## 5. Children's Privacy

Awake does not knowingly collect any personal information from anyone, including children under the age of 13.

---

## 6. Changes to This Privacy Policy & Contact Information

Meridian11 LLC may update this Privacy Policy from time to time to reflect app updates or regulatory requirements. Any updates will be published in-app under **Privacy Policy** and at this repository URL.

If you have any questions or concerns regarding this Privacy Policy or Awake's privacy practices, please contact:

**Meridian11 LLC**  
Email: [meridian11labs@gmail.com](mailto:meridian11labs@gmail.com)  
Copyright © 2026 Meridian11 LLC. All rights reserved.
