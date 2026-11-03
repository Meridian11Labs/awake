# Awake (`com.meridian11.books.awake`)

**Awake** is a Material Design 3 Expressive Keep-Awake utility for **Googlebooks** (Android 14–17 Desktop Mode / ChromeOS large-screen laptops) by **Meridian11 LLC**.

## Key Features

1. **Minimal Material Pictogram Coffee Cup Icons**:
   - **Active (`FullCupAlias`)**: Completely filled coffee cup pictogram with dual pill steam bars (`108dp` Adaptive + Material You `<monochrome>` + `24dp` System Tray icon).
   - **Deactive (`EmptyCupAlias`)**: Half-empty coffee cup pictogram (upper 50% open, lower 50% filled, no steam) for normal Googlebook suspend/resume.
2. **Adaptive Desktop Control Window & Zero-UI Taskbar Toggle Mode**:
   - **Default Mode**: Tapping the Taskbar or Launcher icon opens a responsive 1-column / 2-column (`w640dp`) Material Design 3 desktop window featuring an interactive `CoffeeCupPictogramView` liquid-fill animation, duration presets (`∞`, `30m`, `1h`, `2h`), simulated tap fallback, and Googlebook hardware guardrails.
   - **"Hide settings, tap icon to toggle" Mode**: When enabled, left-clicking the Taskbar coffee cup toggles Awake immediately with zero window; right-clicking the Taskbar icon (`"⚙️ Awake Settings…"`) or long-pressing the Quick Settings tile opens Settings.
3. **Quick Settings Tile (`KeepAwakeTileService`)**:
   - Tap the system tray tile to toggle between Active (Filled Cup) and Deactive (Half-Empty Cup).
   - Long-press the tile (`ACTION_QS_TILE_PREFERENCES`) to open the Awake Settings window.
   - Includes a 1-click **"＋ Add Quick Settings Tile"** action (`StatusBarManager.requestAddTileService`).
4. **1×1 Squircle Toggle Widget (`AwakeSquircleWidgetProvider`)**:
   - Compact `1×1` squircle desktop widget with the same Filled vs. Half-Empty coffee cup toggle logic, kept 100% synchronized with the Taskbar icon, Quick Settings tile, and Main Window.
5. **Multi-Tier Keep-Awake Engine & Smart Googlebook Guardrails**:
   - **Tier 1**: Foreground service (`specialUse`) holding `PARTIAL_WAKE_LOCK` + `SCREEN_BRIGHT_WAKE_LOCK` / `SCREEN_DIM_WAKE_LOCK` with automatic 4-hour renewal.
   - **Tier 2 & 3 (Fallback)**: Optional Accessibility `1×1` `FLAG_KEEP_SCREEN_ON` overlay + periodic corner micro-tap (`dispatchGesture`) every `X` seconds when WakeLocks are restricted by enterprise policy.
   - **Hardware Guardrails**: Backpack Lid-Close Auto-Shutoff (`DisplayManager.DisplayListener`), Low-Battery Cutoff (`≤ 20%` unplugged), and Plug-in Brew (Auto-Awake on AC Charger).

## Testing & Build

Built with strict Test-Driven Development (40/40 JUnit 4 + Robolectric unit tests across 13 suites):

```bash
./gradlew testDebugUnitTest assembleDebug
```
