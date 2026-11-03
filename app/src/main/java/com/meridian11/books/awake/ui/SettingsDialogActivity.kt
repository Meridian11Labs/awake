package com.meridian11.books.awake.ui

import android.app.AlertDialog
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.service.KeepAwakeAccessibilityService
import com.meridian11.books.awake.service.KeepAwakeTileService
import com.meridian11.books.awake.widget.AwakeSquircleWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Full Desktop/Googlebook Control & Settings Window built in Jetpack Compose.
 *
 * Uses an opaque Material 3 Expressive surface with a seamless transparent window caption bar
 * (`APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND`) and zero background blur (blur is reserved
 * exclusively for `QuickGlassPanelActivity`).
 */
class SettingsDialogActivity : AppCompatActivity() {

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val isGlassBlurConfigured: Boolean = false

    private var isAccessibilityEnabledState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSeamlessDesktopWindowHeader()
        isAccessibilityEnabledState = KeepAwakeAccessibilityService.isAccessibilityServiceEnabled(this)

        val app = applicationContext as? AwakeApp
        setContent {
            val state by if (app != null) {
                app.repository.stateFlow.collectAsState(initial = AwakeState())
            } else {
                androidx.compose.runtime.remember { mutableStateOf(AwakeState()) }
            }

            SettingsWindowScreen(
                state = state,
                isAccessibilityEnabled = isAccessibilityEnabledState,
                onToggleAwake = {
                    if (app != null) {
                        uiScope.launch {
                            app.controller.toggle()
                        }
                    }
                },
                onSelectDuration = { minutes ->
                    selectDuration(minutes)
                },
                onToggleDirectTap = { desired ->
                    if (app != null) {
                        uiScope.launch {
                            app.repository.setDirectToggleOnTap(desired)
                            app.controller.syncCurrentState()
                        }
                    }
                },
                onAddQsTile = {
                    requestAddQuickSettingsTile()
                },
                onPinWidget = {
                    requestPinSquircleWidget()
                },
                onToggleTapSimulation = { desired ->
                    if (app != null) {
                        uiScope.launch {
                            val current = app.repository.stateFlow.first()
                            if (desired && !current.accessibilityDisclosureAccepted) {
                                showProminentDisclosureDialog(onAccepted = {
                                    uiScope.launch {
                                        app.repository.setAccessibilityDisclosureAccepted(true)
                                        app.repository.setTapSimulationConfig(
                                            enabled = true,
                                            intervalSeconds = current.tapIntervalSeconds
                                        )
                                        app.controller.syncCurrentState()
                                    }
                                })
                            } else {
                                app.repository.setTapSimulationConfig(
                                    enabled = desired,
                                    intervalSeconds = current.tapIntervalSeconds
                                )
                                app.controller.syncCurrentState()
                            }
                        }
                    }
                },
                onSelectTapInterval = { seconds ->
                    selectTapInterval(seconds)
                },
                onOpenAccessibilitySetup = {
                    if (app != null) {
                        uiScope.launch {
                            val current = app.repository.stateFlow.first()
                            if (!current.accessibilityDisclosureAccepted) {
                                showProminentDisclosureDialog(onAccepted = {
                                    uiScope.launch {
                                        app.repository.setAccessibilityDisclosureAccepted(true)
                                        openSystemAccessibilitySettings()
                                    }
                                })
                            } else {
                                openSystemAccessibilitySettings()
                            }
                        }
                    }
                },
                onUpdateGuardrails = { lidClose, lowBattery, charger, dimming ->
                    if (app != null) {
                        uiScope.launch {
                            app.repository.updateGuardrails(
                                lowBatteryCutoffEnabled = lowBattery,
                                autoAwakeOnChargerEnabled = charger,
                                lidCloseAutoShutoffEnabled = lidClose,
                                allowScreenDimming = dimming
                            )
                            app.controller.syncCurrentState()
                        }
                    }
                },
                onCloseWindow = {
                    finish()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        isAccessibilityEnabledState = KeepAwakeAccessibilityService.isAccessibilityServiceEnabled(this)
    }

    /**
     * Makes the Googlebook desktop freeform window caption bar 100% transparent so
     * the window header and app ground (`@color/window_bg`) form a single seamless surface.
     */
    private fun enableSeamlessDesktopWindowHeader() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val controller = window.insetsController ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val clearCaption = WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
            val lightCaption = WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS
            val appearance = if (!isNight) clearCaption or lightCaption else clearCaption
            controller.setSystemBarsAppearance(appearance, clearCaption or lightCaption)
        }
    }

    private fun selectDuration(minutes: Int) {
        val app = applicationContext as? AwakeApp ?: return
        uiScope.launch {
            app.repository.setDurationMinutes(minutes)
            val current = app.repository.stateFlow.first()
            if (current.isAwake) {
                app.controller.enableWithDuration(minutes)
            } else {
                app.controller.syncCurrentState()
            }
        }
    }

    private fun selectTapInterval(seconds: Int) {
        val app = applicationContext as? AwakeApp ?: return
        uiScope.launch {
            val current = app.repository.stateFlow.first()
            app.repository.setTapSimulationConfig(
                enabled = current.tapSimulationEnabled,
                intervalSeconds = seconds
            )
            app.controller.syncCurrentState()
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

    private fun showProminentDisclosureDialog(onAccepted: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.disclosure_title)
            .setMessage(R.string.disclosure_message)
            .setPositiveButton(R.string.disclosure_accept) { dialog, _ ->
                dialog.dismiss()
                onAccepted()
            }
            .setNegativeButton(R.string.disclosure_decline) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openSystemAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Throwable) {
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }
}
