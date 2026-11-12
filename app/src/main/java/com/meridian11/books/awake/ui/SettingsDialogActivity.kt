package com.meridian11.books.awake.ui

import android.Manifest
import android.app.ActivityManager
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import com.meridian11.books.awake.service.KeepAwakeTileService
import com.meridian11.books.awake.widget.AwakeSquircleWidgetProvider
import kotlinx.coroutines.launch

/**
 * Full Googlebook Control & Settings Window built in Jetpack Compose.
 *
 * Uses an opaque Material 3 Expressive surface with a seamless transparent window caption bar
 * (`APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND` on Android 15+) and zero background blur.
 */
class SettingsDialogActivity : AppCompatActivity() {

    @VisibleForTesting
    val isGlassBlurConfigured: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null && intent?.action == AndroidShortcutPublisher.ACTION_OPEN_SETTINGS) {
            AndroidShortcutPublisher.reportShortcutUsed(this, AndroidShortcutPublisher.ID_OPEN_SETTINGS)
        }

        val app = applicationContext as? AwakeApp
        setContent {
            val state by if (app != null) {
                app.uiStateFlow.collectAsState()
            } else {
                remember { mutableStateOf(AwakeState()) }
            }

            LaunchedEffect(state.isAwake) {
                updateWindowTaskDescription(isAwake = state.isAwake)
            }

            SettingsWindowScreen(
                state = state,
                onToggleAwake = {
                    if (!state.isAwake &&
                        state.lowBatteryCutoffEnabled &&
                        isCurrentlyBlockedByLowBattery(state.lowBatteryThresholdPercent)
                    ) {
                        Toast.makeText(
                            this,
                            getString(R.string.toast_blocked_by_low_battery, state.lowBatteryThresholdPercent),
                            Toast.LENGTH_LONG
                        ).show()
                        return@SettingsWindowScreen
                    }
                    if (!state.isAwake) {
                        askForNotificationsIfNeeded()
                    }
                    if (app != null) {
                        app.appScope.launch {
                            app.controller.toggle()
                        }
                    }
                },
                onSelectDuration = { minutes ->
                    if (app != null) {
                        app.appScope.launch {
                            app.controller.selectDurationPreset(minutes)
                        }
                    }
                },
                onToggleDirectTap = { desired ->
                    if (app != null) {
                        app.appScope.launch {
                            app.controller.setDirectToggleOnTap(desired)
                        }
                    }
                },
                onAddQsTile = {
                    requestAddQuickSettingsTile()
                },
                onPinWidget = {
                    requestPinSquircleWidget()
                },
                onUpdateGuardrails = { lidClose, lowBattery, dimming ->
                    if (app != null) {
                        app.appScope.launch {
                            app.controller.updateGuardrails(
                                lowBatteryCutoffEnabled = lowBattery,
                                autoAwakeOnChargerEnabled = state.autoAwakeOnChargerEnabled,
                                lidCloseAutoShutoffEnabled = lidClose,
                                allowScreenDimming = dimming
                            )
                        }
                    }
                },
                onCloseWindow = {
                    finish()
                }
            )
        }

        enableSeamlessDesktopWindowHeader()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == AndroidShortcutPublisher.ACTION_OPEN_SETTINGS) {
            AndroidShortcutPublisher.reportShortcutUsed(this, AndroidShortcutPublisher.ID_OPEN_SETTINGS)
        }
    }

    /**
     * Makes the Googlebook desktop freeform window caption bar 100% transparent so
     * the window header and app ground (`@color/window_bg`) form a single seamless surface.
     */
    private fun enableSeamlessDesktopWindowHeader() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val controller = window.decorView.windowInsetsController ?: window.insetsController ?: return
            val clearCaption = WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
            val lightCaption = WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS
            val appearance = if (!isNight) clearCaption or lightCaption else clearCaption
            controller.setSystemBarsAppearance(appearance, clearCaption or lightCaption)
        }
    }

    private fun updateWindowTaskDescription(isAwake: Boolean) {
        runCatching {
            val iconRes = if (isAwake) R.mipmap.ic_launcher_full else R.mipmap.ic_launcher_empty
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val description = ActivityManager.TaskDescription.Builder()
                    .setLabel(getString(R.string.app_name))
                    .setIcon(iconRes)
                    .build()
                setTaskDescription(description)
            } else {
                @Suppress("DEPRECATION")
                val bitmap = BitmapFactory.decodeResource(resources, iconRes)
                @Suppress("DEPRECATION")
                setTaskDescription(ActivityManager.TaskDescription(getString(R.string.app_name), bitmap))
            }
        }
    }

    private fun isCurrentlyBlockedByLowBattery(thresholdPercent: Int): Boolean {
        val sticky = runCatching {
            registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull() ?: return false

        val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        if (level < 0) return false
        val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isCharging = plugged != 0 ||
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val percent = (level * 100) / scale
        return !isCharging && percent <= thresholdPercent
    }

    /**
     * The foreground service's notification (Turn off, +30 min) shows only with POST_NOTIFICATIONS, which Android 13+
     * grants at runtime. Asked when the user turns Awake on from this window; Android stops showing the prompt after
     * two refusals, and Awake keeps working without it.
     */
    private fun askForNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            val app = applicationContext as? AwakeApp ?: return
            app.appScope.launch {
                app.controller.syncCurrentState()
            }
        }
    }

    private fun requestAddQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val statusBarManager = getSystemService(StatusBarManager::class.java)
                statusBarManager?.requestAddTileService(
                    ComponentName(this, KeepAwakeTileService::class.java),
                    getString(R.string.app_name),
                    Icon.createWithResource(this, R.drawable.ic_stat_coffee_full),
                    mainExecutor
                ) { result ->
                    val message = when (result) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                            "Awake Quick Settings tile added!"
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                            "Awake tile is already in your Quick Settings."
                        else ->
                            "Open Quick Settings in the system tray to use the Awake tile."
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun requestPinSquircleWidget() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val provider = ComponentName(this, AwakeSquircleWidgetProvider::class.java)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(provider, null, null)
            } else {
                Toast.makeText(
                    this,
                    "Long-press desktop to add the 1×1 Awake Squircle Widget.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: Throwable) {
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 1
    }
}
