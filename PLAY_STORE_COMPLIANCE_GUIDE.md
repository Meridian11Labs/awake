# Google Play Store Compliance & Submission Guide — Awake (`com.meridian11.books.awake`)

**Owner & Publisher:** **Meridian11 LLC** (`meridian11labs@gmail.com`)  
**Package Name:** `com.meridian11.books.awake`  
**Version:** `2.0.0` (`versionCode = 2`)  
**Target / Compile SDK:** `API 36` (`minSdk = 31`)

This document contains the complete checklist and **ready-to-paste Google Play Console declaration answers** required to publish **Awake** on Google Play.

---

## 1. Signed Release Artifacts Ready for Upload

Run:
```bash
./gradlew bundleRelease assembleRelease
```

- **Google Play App Bundle (`.aab`):** `app/build/outputs/bundle/release/app-release.aab` *(Upload this file to Google Play Console -> Production / Internal Testing)*
- **Signed Release APK (`.apk`):** `app/build/outputs/apk/release/app-release.apk`
- **Upload Keystore:** `app/keystore/meridian11-release.jks` (`O=Meridian11 LLC`, RSA 2048-bit, 10,000-day validity)
- **R8 / ProGuard Minification & Resource Shrinking:** Enabled (`isMinifyEnabled = true`, `isShrinkResources = true`) with [`app/proguard-rules.pro`](app/proguard-rules.pro) preserving all dynamic `<activity-alias>`, `AccessibilityService`, `TileService`, and `AppWidgetProvider` components.

---

## 2. Google Play Console -> Policy -> App Content Declarations

### A. Privacy Policy URL
- **URL to paste in Play Console:**  
  `https://github.com/kuscher/awake/blob/main/PRIVACY_POLICY.md` *(or raw/GitHub Pages URL)*
- **In-App Access:** Available on the main Awake window footer (`About  •  Privacy Policy  •  Terms of Service  •  Accessibility Disclosure  •  © Meridian11 LLC`).

---

### B. Data Safety Questionnaire
- **Does your app collect or share any of the required user data types?**  
  Select: **`No`**
- **Technical Verification:**  
  `com.meridian11.books.awake` does **not** request `android.permission.INTERNET` and stores all user preferences strictly locally on-device via Android Jetpack DataStore.

---

### C. AccessibilityService API Declaration (`BIND_ACCESSIBILITY_SERVICE`)

Navigate to **Play Console -> Policy -> App content -> AccessibilityService API**:

1. **Does your app use the AccessibilityService API?**  
   Select: **`Yes`**
2. **Is your app an accessibility tool designed specifically to support users with disabilities (`isAccessibilityTool="true"`)?**  
   Select: **`No`** *(Matches `android:isAccessibilityTool="false"` in [`res/xml/accessibility_service_config.xml`](app/src/main/res/xml/accessibility_service_config.xml))*
3. **Does your app display a Prominent Disclosure and obtain affirmative user consent before enabling the AccessibilityService?**  
   Select: **`Yes`**
4. **Why does your app need the AccessibilityService API? (Copy-Paste Justification):**
   > Awake (by Meridian11 LLC) is a user-initiated keep-awake utility for Googlebooks, Chromebooks, and Android Desktop devices. On enterprise-managed laptops and desktop configurations where system power policies restrict standard `PowerManager.WakeLock` flags from keeping the display awake, Awake offers an optional fallback powered by `KeepAwakeAccessibilityService`.
   >
   > Only after the user reads and clicks "Agree & Continue" on the standalone in-app Prominent Disclosure modal (`AccessibilityProminentDisclosureModal`), the service:
   > 1. Attaches a 1×1 transparent window (`TYPE_ACCESSIBILITY_OVERLAY`) with `FLAG_KEEP_SCREEN_ON` while Keep-Awake is active.
   > 2. When the user explicitly enables "Simulate Tap / Click Fallback", dispatches a deterministic 1-millisecond stationary micro-tap (`dispatchGesture`) at screen coordinate `(1, 1)` every 15–120 seconds.
   >
   > Privacy & Policy Safeguards:
   > - `canRetrieveWindowContent="false"` and `accessibilityEventTypes=""` are enforced in `accessibility_service_config.xml`.
   > - Awake never reads screen content, never logs keystrokes, and does not request `android.permission.INTERNET`.
   > - Even if enabled in Android System Settings, `KeepAwakeAccessibilityService` is hard-gated in code (`if (!state.accessibilityDisclosureAccepted) return`) so it never activates without affirmative in-app consent, and users can revoke consent at any time inside `Awake -> Accessibility Disclosure -> Revoke Accessibility Consent & Disable Fallback`.

5. **Mandatory Video Walkthrough Checklist (Unlisted YouTube / Google Drive Link for Play Reviewer):**
   Record a 30–45 second screen capture on the Googlebook showing:
   1. Opening **Awake** from the launcher/taskbar.
   2. Toggling **"Simulate Tap / Click Fallback"** ON (or clicking **"Enable Accessibility Service for Tap Simulation"**).
   3. Showing the standalone **Prominent Disclosure: AccessibilityService API** modal appearing in-app with the full disclosure text and **"Decline"** / **"Agree & Continue"** buttons.
   4. Clicking **"Agree & Continue"**, which redirects to Android System Accessibility Settings, and enabling **Awake Display & Tap Keep-Alive**.
   5. Returning to Awake, toggling **Keep-Awake** ON, and opening the **Accessibility Disclosure** footer link to show the **"Revoke Accessibility Consent & Disable Fallback"** button.

---

### D. Foreground Service Permission Declaration (`FOREGROUND_SERVICE_SPECIAL_USE`)

Navigate to **Play Console -> Policy -> App content -> Foreground service permissions**:

1. **Selected Type:** `specialUse` (`android.permission.FOREGROUND_SERVICE_SPECIAL_USE`)
2. **Manifest Subtype Property:** Declared in [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) via `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.
3. **Why is `specialUse` required? (Copy-Paste Justification):**
   > Awake is a dedicated keep-awake (caffeine) utility for Googlebooks, Chromebooks, and Android Desktop devices. When the user explicitly toggles Awake ON (via the Taskbar coffee cup icon, Quick Settings tile, desktop widget, or app window), `KeepAwakeService` starts as a Foreground Service with a persistent status notification ("Keep-Alive Active" with a one-tap "Turn Off" action and "+30m" timer extension action).
   >
   > While running, `KeepAwakeService` holds a `PowerManager.WakeLock` to prevent CPU/display sleep and registers live hardware guardrail receivers (`ACTION_BATTERY_CHANGED`, `ACTION_SCREEN_OFF`, and `DisplayManager.DisplayListener`) so it can automatically shut off Keep-Awake when the laptop lid closes ("Backpack Safety") or when battery drops below 20% unplugged ("Battery Guard"). None of Android's other predefined Foreground Service types (`mediaPlayback`, `connectedDevice`, `dataSync`, `health`, etc.) apply to an interactive system keep-awake utility.

---

### E. App Access, Ads, Target Audience & Device Catalog
- **App Access:** All functionality is available without sign-in or special credentials.
- **Ads:** Select **`No, my app does not contain ads`**.
- **Target Audience:** Select **`18 and over`** (or `13+`, do **not** opt into "Designed for Families").
- **Chromebook / Googlebook / Non-Touchscreen Compatibility:** [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) explicitly declares `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />` and `<uses-feature android:name="android.software.app_widgets" android:required="false" />`.
