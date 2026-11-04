# Awake (`com.meridian11.books.awake`)

**Awake** is a Material Design 3 Expressive Keep-Awake utility for **Googlebooks** (Android 14–17 Desktop Mode / ChromeOS large-screen laptops) owned, designed, and built by **Meridian11 LLC** ([https://github.com/Meridian11Labs/awake](https://github.com/Meridian11Labs/awake)).

## Key Features

1. **Material Design Themed Pictogram Coffee Cup Icons**:
   - **Active (`FullCupAlias`)**: Completely filled coffee cup pictogram with dual vertical steam bars (`108dp` Adaptive + Material You `<monochrome>` + `24dp` System Tray icon).
   - **Deactive (`EmptyCupAlias`)**: Half-empty coffee cup pictogram (upper 50% open, lower 50% filled, no steam) for normal Googlebook suspend/resume.
2. **Adaptive Jetpack Compose Desktop Control Window & Zero-UI Taskbar Toggle Mode**:
   - **Default Mode**: Tapping the Taskbar or Launcher icon opens a responsive 1-column / 2-column (`w640dp`) Material 3 Expressive Jetpack Compose desktop window (`SettingsDialogActivity`) with a seamless transparent window caption header, outline-free Pastel Blue / Pastel Orange Hero Toggle Card, animated `CoffeeCupPictogramComposable`, single-line segmented duration presets (`Infinite`, `30m`, `1h`, `2h`), simulated tap fallback, and Googlebook hardware guardrails.
   - **"Hide settings, tap icon to toggle" Mode**: When enabled, left-clicking the Taskbar coffee cup toggles Awake immediately with zero window; right-clicking the Taskbar icon (`"Awake Settings…"`) or long-pressing the Quick Settings tile opens Settings.
3. **100% Surface Synchronization Across All 4 Entry Points**:
   - **Taskbar App Icon**, **Inside-App Settings Window**, **Quick Settings Tile (`KeepAwakeTileService`)**, and **Desktop Widget (`AwakeSquircleWidgetProvider`)** stay synchronized in real time via a continuous application-scope state observer (`AwakeApp`) and direct bound tile updates (`AndroidWidgetUpdater`).
4. **True `1×1` Default + Resizable Multi-Size Desktop Widget (`AwakeSquircleWidgetProvider`)**:
   - **`1×1` Default (`COMPACT_1X1`)**: Full-bleed outline-free Pastel Blue / Pastel Orange squircle coffee cup toggle.
   - **`2×1` Medium (`MEDIUM_2X1`)**: Horizontal hero banner with coffee cup badge, `Awake` / `Asleep` status, interactive **Duration Cycle Pill** (`∞` / `30m` / `1h` / `2h`), and **Settings** button.
   - **`Expanded` (`EXPANDED`, `2×2+` / `3×2+`)**: Elevated Material 3 card with the Hero Toggle Container, 4-button **Segmented Duration Pill Row** (`Infinite | 30m | 1h | 2h`), **Tap Sim Toggle Pill** (`Tap Sim: ON/OFF`), and **Settings** button.
5. **Multi-Tier Keep-Awake Engine, Google Play-Compliant Accessibility Disclosure & Smart Guardrails**:
   - **Tier 1**: Foreground service (`specialUse`) holding `PARTIAL_WAKE_LOCK` + `SCREEN_BRIGHT_WAKE_LOCK` / `SCREEN_DIM_WAKE_LOCK` with automatic 4-hour renewal.
   - **Tier 2 & 3 (Fallback)**: Optional Accessibility `1×1` `FLAG_KEEP_SCREEN_ON` overlay + periodic corner micro-tap (`dispatchGesture`) gated behind a standalone in-app **Prominent Disclosure Modal** (`AccessibilityProminentDisclosureModal`) and hard code-level consent check (`KeepAwakeAccessibilityService`).
   - **Hardware Guardrails**: Backpack Lid-Close Auto-Shutoff (`DisplayManager.DisplayListener`), Low-Battery Cutoff (`≤ 20%` unplugged), and Plug-in Brew (Auto-Awake on AC Charger).

## Legal & Google Play Compliance

- **Privacy Policy:** [`PRIVACY_POLICY.md`](https://github.com/Meridian11Labs/awake/blob/main/PRIVACY_POLICY.md)
- **Terms of Service:** [`TERMS_OF_SERVICE.md`](https://github.com/Meridian11Labs/awake/blob/main/TERMS_OF_SERVICE.md)
- **Google Play Console Compliance & Declaration Guide:** [`PLAY_STORE_COMPLIANCE_GUIDE.md`](https://github.com/Meridian11Labs/awake/blob/main/PLAY_STORE_COMPLIANCE_GUIDE.md)
- **Releases:** [https://github.com/Meridian11Labs/awake/releases](https://github.com/Meridian11Labs/awake/releases)

## Testing & Build

Built with strict Test-Driven Development (48/48 JUnit 4 + Robolectric + Jetpack Compose UI tests across 14 suites):

```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease bundleRelease
```

Copyright © 2026 **Meridian11 LLC** (`meridian11labs@gmail.com`). All rights reserved.
