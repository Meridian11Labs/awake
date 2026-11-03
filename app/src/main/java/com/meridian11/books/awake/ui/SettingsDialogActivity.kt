package com.meridian11.books.awake.ui

import android.app.AlertDialog
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import com.meridian11.books.awake.service.KeepAwakeAccessibilityService
import com.meridian11.books.awake.service.KeepAwakeTileService
import com.meridian11.books.awake.widget.AwakeSquircleWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Desktop/Googlebook-ready, Material Design 3, reactive, adaptive Settings & Control window.
 *
 * Launched by default when clicking the Awake icon, or via right-clicking the Taskbar icon
 * ("⚙️ Awake Settings…") / long-pressing the Quick Settings tile (`ACTION_QS_TILE_PREFERENCES`)
 * when "Hide settings, tap icon to toggle" is enabled.
 */
class SettingsDialogActivity : AppCompatActivity() {

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isBindingState = false
    private var isFirstRender = true

    private lateinit var cardHeroToggle: View
    private lateinit var coffeeCupHero: CoffeeCupPictogramView
    private lateinit var tvDialogTitle: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var switchMasterAwake: Switch

    private lateinit var switchDirectToggleOnTap: Switch
    private lateinit var tvDirectToggleSubtitle: TextView

    private lateinit var btnDurationIndefinite: Button
    private lateinit var btnDuration30m: Button
    private lateinit var btnDuration1h: Button
    private lateinit var btnDuration2h: Button

    private lateinit var tvQsTileInfo: TextView
    private lateinit var btnAddQsTile: Button
    private lateinit var btnPinWidget: Button

    private lateinit var tvTapIntervalSummary: TextView
    private lateinit var switchTapSimulation: Switch
    private lateinit var btnTap15s: Button
    private lateinit var btnTap30s: Button
    private lateinit var btnTap60s: Button
    private lateinit var btnTap120s: Button
    private lateinit var btnAccessibilitySetup: Button

    private lateinit var switchLidCloseShutoff: Switch
    private lateinit var switchLowBatteryCutoff: Switch
    private lateinit var switchAutoAwakeCharger: Switch
    private lateinit var switchAllowDimming: Switch
    private lateinit var btnCloseSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_dialog)

        bindViews()
        setupListeners()

        val app = applicationContext as? AwakeApp ?: return
        uiScope.launch {
            app.repository.stateFlow.collectLatest { state ->
                renderState(state)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityButtonLabel()
    }

    private fun bindViews() {
        cardHeroToggle = findViewById(R.id.card_hero_toggle)
        coffeeCupHero = findViewById(R.id.coffee_cup_hero)
        tvDialogTitle = findViewById(R.id.tv_dialog_title)
        tvStatusSubtitle = findViewById(R.id.tv_status_subtitle)
        switchMasterAwake = findViewById(R.id.switch_master_awake)

        switchDirectToggleOnTap = findViewById(R.id.switch_direct_toggle_on_tap)
        tvDirectToggleSubtitle = findViewById(R.id.tv_direct_toggle_subtitle)

        btnDurationIndefinite = findViewById(R.id.btn_duration_indefinite)
        btnDuration30m = findViewById(R.id.btn_duration_30m)
        btnDuration1h = findViewById(R.id.btn_duration_1h)
        btnDuration2h = findViewById(R.id.btn_duration_2h)

        tvQsTileInfo = findViewById(R.id.tv_qs_tile_info)
        btnAddQsTile = findViewById(R.id.btn_add_qs_tile)
        btnPinWidget = findViewById(R.id.btn_pin_widget)

        tvTapIntervalSummary = findViewById(R.id.tv_tap_interval_summary)
        switchTapSimulation = findViewById(R.id.switch_tap_simulation)
        btnTap15s = findViewById(R.id.btn_tap_15s)
        btnTap30s = findViewById(R.id.btn_tap_30s)
        btnTap60s = findViewById(R.id.btn_tap_60s)
        btnTap120s = findViewById(R.id.btn_tap_120s)
        btnAccessibilitySetup = findViewById(R.id.btn_accessibility_setup)

        switchLidCloseShutoff = findViewById(R.id.switch_lid_close_shutoff)
        switchLowBatteryCutoff = findViewById(R.id.switch_low_battery_cutoff)
        switchAutoAwakeCharger = findViewById(R.id.switch_auto_awake_charger)
        switchAllowDimming = findViewById(R.id.switch_allow_dimming)
        btnCloseSettings = findViewById(R.id.btn_close_settings)
    }

    private fun setupListeners() {
        val app = applicationContext as? AwakeApp ?: return

        val toggleAwakeAction = {
            if (!isBindingState) {
                uiScope.launch {
                    app.controller.toggle()
                }
            }
        }

        cardHeroToggle.setOnClickListener { toggleAwakeAction() }
        coffeeCupHero.setOnClickListener { toggleAwakeAction() }
        switchMasterAwake.setOnClickListener { toggleAwakeAction() }

        switchDirectToggleOnTap.setOnClickListener {
            if (isBindingState) return@setOnClickListener
            val desired = switchDirectToggleOnTap.isChecked
            uiScope.launch {
                app.repository.setDirectToggleOnTap(desired)
                app.controller.syncCurrentState()
            }
        }

        btnDurationIndefinite.setOnClickListener { selectDuration(0) }
        btnDuration30m.setOnClickListener { selectDuration(30) }
        btnDuration1h.setOnClickListener { selectDuration(60) }
        btnDuration2h.setOnClickListener { selectDuration(120) }

        btnAddQsTile.setOnClickListener {
            requestAddQuickSettingsTile()
        }

        btnPinWidget.setOnClickListener {
            requestPinSquircleWidget()
        }

        switchTapSimulation.setOnClickListener {
            if (isBindingState) return@setOnClickListener
            val desired = switchTapSimulation.isChecked
            uiScope.launch {
                val current = app.repository.stateFlow.first()
                if (desired && !current.accessibilityDisclosureAccepted) {
                    switchTapSimulation.isChecked = false
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

        btnTap15s.setOnClickListener { selectTapInterval(15) }
        btnTap30s.setOnClickListener { selectTapInterval(30) }
        btnTap60s.setOnClickListener { selectTapInterval(60) }
        btnTap120s.setOnClickListener { selectTapInterval(120) }

        btnAccessibilitySetup.setOnClickListener {
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

        val guardrailClickListener = {
            if (!isBindingState) {
                uiScope.launch {
                    app.repository.updateGuardrails(
                        lowBatteryCutoffEnabled = switchLowBatteryCutoff.isChecked,
                        autoAwakeOnChargerEnabled = switchAutoAwakeCharger.isChecked,
                        lidCloseAutoShutoffEnabled = switchLidCloseShutoff.isChecked,
                        allowScreenDimming = switchAllowDimming.isChecked
                    )
                    app.controller.syncCurrentState()
                }
            }
        }

        switchLidCloseShutoff.setOnClickListener { guardrailClickListener() }
        switchLowBatteryCutoff.setOnClickListener { guardrailClickListener() }
        switchAutoAwakeCharger.setOnClickListener { guardrailClickListener() }
        switchAllowDimming.setOnClickListener { guardrailClickListener() }

        btnCloseSettings.setOnClickListener { finish() }
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

    private fun renderState(state: AwakeState) {
        isBindingState = true
        try {
            coffeeCupHero.setAwakeState(isAwake = state.isAwake, animate = !isFirstRender)
            isFirstRender = false

            cardHeroToggle.setBackgroundResource(
                if (state.isAwake) R.drawable.bg_hero_card_active
                else R.drawable.bg_hero_card_inactive
            )

            val durationLabel = AndroidShortcutPublisher.formatDurationLong(state.durationMinutes)
            tvDialogTitle.text = if (state.isAwake) {
                "Filled Cup • Keeping Awake"
            } else {
                "Half-Empty • Normal Sleep"
            }
            tvStatusSubtitle.text = if (state.isAwake) {
                "Active ($durationLabel) • Click card, Taskbar icon, QS tile, or widget to toggle"
            } else {
                "Deactive ($durationLabel preset) • Normal Googlebook suspend/resume"
            }
            switchMasterAwake.isChecked = state.isAwake

            switchDirectToggleOnTap.isChecked = state.directToggleOnTap
            tvDirectToggleSubtitle.text = if (state.directToggleOnTap) {
                "Active: Tapping the Taskbar icon toggles Awake immediately with no window. Right-click the Taskbar icon (or long-press the Quick Settings tile) to open Settings."
            } else {
                "Off (Default): Tapping the Taskbar icon opens this window. Turn ON to hide settings and toggle Awake directly on icon tap."
            }

            highlightSelectedButton(
                mapOf(
                    0 to btnDurationIndefinite,
                    30 to btnDuration30m,
                    60 to btnDuration1h,
                    120 to btnDuration2h
                ),
                state.durationMinutes
            )

            switchTapSimulation.isChecked = state.tapSimulationEnabled
            tvTapIntervalSummary.text = if (state.tapSimulationEnabled) {
                "Active: Simulating corner micro-tap every ${state.tapIntervalSeconds}s"
            } else {
                "Off: Micro-tap every ${state.tapIntervalSeconds}s when WakeLock can't be used"
            }

            highlightSelectedButton(
                mapOf(
                    15 to btnTap15s,
                    30 to btnTap30s,
                    60 to btnTap60s,
                    120 to btnTap120s
                ),
                state.tapIntervalSeconds
            )

            switchLidCloseShutoff.isChecked = state.lidCloseAutoShutoffEnabled
            switchLowBatteryCutoff.isChecked = state.lowBatteryCutoffEnabled
            switchAutoAwakeCharger.isChecked = state.autoAwakeOnChargerEnabled
            switchAllowDimming.isChecked = state.allowScreenDimming

            updateAccessibilityButtonLabel()
        } finally {
            isBindingState = false
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

    private fun highlightSelectedButton(buttons: Map<Int, Button>, selectedKey: Int) {
        val selectedTextColor = getColor(R.color.chip_selected_text)
        val unselectedTextColor = getColor(R.color.chip_unselected_text)
        buttons.forEach { (key, button) ->
            val isSelected = key == selectedKey
            button.setBackgroundResource(
                if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
            )
            button.setTextColor(if (isSelected) selectedTextColor else unselectedTextColor)
            button.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun updateAccessibilityButtonLabel() {
        val enabled = KeepAwakeAccessibilityService.isAccessibilityServiceEnabled(this)
        btnAccessibilitySetup.text = if (enabled) {
            "✓ Accessibility Keep-Alive Service Enabled"
        } else {
            "Enable Accessibility Service for Tap Simulation →"
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

    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }
}
