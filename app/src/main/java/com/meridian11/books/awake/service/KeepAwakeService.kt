package com.meridian11.books.awake.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.Display
import androidx.core.app.NotificationCompat
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service (`foregroundServiceType="specialUse"`) that holds the active
 * `WakeLockEngine`, monitors Googlebook hardware guardrails (Lid-Close display state
 * and Low-Battery cutoff), and provides system-tray quick actions.
 */
class KeepAwakeService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var wakeLockEngine: WakeLockEngine
    private var wakeLockRenewalJob: Job? = null
    private var displayManager: DisplayManager? = null
    private var isBatteryReceiverRegistered = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = checkDisplayStates()
        override fun onDisplayChanged(displayId: Int) = checkDisplayStates()
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            if (level < 0) return

            val percent = (level * 100) / scale
            val isCharging = plugged != 0 ||
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

            val app = applicationContext as? AwakeApp ?: return
            app.appScope.launch {
                app.controller.onBatteryStateChanged(levelPercent = percent, isCharging = isCharging)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        wakeLockEngine = WakeLockEngine(this)
        createNotificationChannel()
        registerHardwareMonitors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as? AwakeApp
        when (intent?.action) {
            ACTION_STOP_KEEP_AWAKE -> {
                // Launch on appScope so serviceScope.cancel() in onDestroy() never aborts DataStore/icon reset
                app?.appScope?.launch {
                    app.controller.disableKeepAwake()
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_EXTEND_30_MIN -> {
                app?.appScope?.launch {
                    app.controller.extendSession(additionalMinutes = 30)
                }
                return START_STICKY
            }
        }

        val allowDimming = intent?.getBooleanExtra(EXTRA_ALLOW_DIMMING, false) ?: false
        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 0) ?: 0
        val tapSimEnabled = intent?.getBooleanExtra(EXTRA_TAP_SIM_ENABLED, false) ?: false
        val tapIntervalSec = intent?.getIntExtra(EXTRA_TAP_INTERVAL_SEC, 60) ?: 60

        val notification = buildOngoingNotification(
            durationMinutes = durationMinutes,
            tapSimEnabled = tapSimEnabled,
            tapIntervalSec = tapIntervalSec,
            allowDimming = allowDimming
        )

        startForegroundCompat(notification)
        acquireAndScheduleWakeLockRenewal(allowScreenDimming = allowDimming)

        serviceScope.launch {
            val state = app?.repository?.stateFlow?.first()
            if (state != null) {
                if (intent == null) {
                    // Service was restarted by OS (START_STICKY) — reconcile from persisted state
                    if (!state.isAwake) {
                        stopSelf()
                        return@launch
                    }
                    acquireAndScheduleWakeLockRenewal(allowScreenDimming = state.allowScreenDimming)
                    val syncedNotification = buildOngoingNotification(
                        durationMinutes = state.durationMinutes,
                        tapSimEnabled = state.tapSimulationEnabled,
                        tapIntervalSec = state.tapIntervalSeconds,
                        allowDimming = state.allowScreenDimming
                    )
                    getSystemService(NotificationManager::class.java)
                        ?.notify(NOTIFICATION_ID, syncedNotification)
                }
                KeepAwakeAccessibilityService.instance?.applyState(state)
            }
        }

        return START_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun acquireAndScheduleWakeLockRenewal(allowScreenDimming: Boolean) {
        wakeLockEngine.acquire(allowScreenDimming = allowScreenDimming)
        wakeLockRenewalJob?.cancel()
        wakeLockRenewalJob = serviceScope.launch {
            while (isActive) {
                delay(WAKELOCK_RENEWAL_INTERVAL_MS)
                if (isActive) {
                    wakeLockEngine.acquire(allowScreenDimming = allowScreenDimming)
                }
            }
        }
    }

    private fun checkDisplayStates() {
        val dm = displayManager ?: return
        val displays = dm.displays
        val defaultDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY)
        val isPrimaryOff = defaultDisplay != null && defaultDisplay.state == Display.STATE_OFF
        if (!isPrimaryOff) return

        val hasActiveExternal = displays.any {
            it.displayId != Display.DEFAULT_DISPLAY && it.state == Display.STATE_ON
        }
        val app = applicationContext as? AwakeApp ?: return
        app.appScope.launch {
            app.controller.onPrimaryDisplayOff(hasActiveExternalDisplay = hasActiveExternal)
        }
    }

    private fun registerHardwareMonitors() {
        displayManager = getSystemService(DisplayManager::class.java)?.also { dm ->
            dm.registerDisplayListener(displayListener, null)
        }
        try {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            isBatteryReceiverRegistered = true
        } catch (_: Throwable) {
        }
    }

    private fun unregisterHardwareMonitors() {
        try {
            displayManager?.unregisterDisplayListener(displayListener)
        } catch (_: Throwable) {
        }
        if (isBatteryReceiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Throwable) {
            }
            isBatteryReceiverRegistered = false
        }
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Awake Active Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Awake is keeping your Googlebook active"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildOngoingNotification(
        durationMinutes: Int,
        tapSimEnabled: Boolean,
        tapIntervalSec: Int,
        allowDimming: Boolean
    ): Notification {
        val stopIntent = Intent(this, KeepAwakeService::class.java).apply {
            action = ACTION_STOP_KEEP_AWAKE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val extendIntent = Intent(this, KeepAwakeService::class.java).apply {
            action = ACTION_EXTEND_30_MIN
        }
        val extendPendingIntent = PendingIntent.getService(
            this,
            2,
            extendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val settingsIntent = Intent(AndroidShortcutPublisher.ACTION_OPEN_SETTINGS).apply {
            setComponent(
                ComponentName(
                    packageName,
                    AndroidShortcutPublisher.SETTINGS_DIALOG_ACTIVITY_CLASS
                )
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            this,
            3,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationText = AndroidShortcutPublisher.formatDurationLong(durationMinutes)
        val modeDetails = buildList {
            add("Duration: $durationText")
            if (allowDimming) add("Dimming Allowed") else add("Full Brightness Lock")
            if (tapSimEnabled) add("Tap Sim: ${tapIntervalSec}s")
        }.joinToString(" • ")

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_coffee_full)
            .setContentTitle("Awake — Keeping Googlebook Active")
            .setContentText(modeDetails)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(settingsPendingIntent)
            .addAction(
                R.drawable.ic_stat_coffee_empty,
                "Turn Off (Suspend)",
                stopPendingIntent
            )
            .addAction(
                R.drawable.ic_shortcut_timer,
                "+30m",
                extendPendingIntent
            )
            .addAction(
                R.drawable.ic_shortcut_settings,
                "Settings",
                settingsPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        wakeLockRenewalJob?.cancel()
        wakeLockRenewalJob = null
        unregisterHardwareMonitors()
        wakeLockEngine.release()
        KeepAwakeAccessibilityService.instance?.applyState(AwakeState(isAwake = false))
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "awake_status_channel"
        const val NOTIFICATION_ID = 1101
        private const val WAKELOCK_RENEWAL_INTERVAL_MS = 4L * 60L * 60L * 1000L // Renew every 4h

        const val ACTION_START_OR_UPDATE = "com.meridian11.books.awake.action.START_OR_UPDATE"
        const val ACTION_STOP_KEEP_AWAKE = "com.meridian11.books.awake.action.STOP_KEEP_AWAKE"
        const val ACTION_EXTEND_30_MIN = "com.meridian11.books.awake.action.EXTEND_30_MIN"

        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_ALLOW_DIMMING = "extra_allow_dimming"
        const val EXTRA_TAP_SIM_ENABLED = "extra_tap_sim_enabled"
        const val EXTRA_TAP_INTERVAL_SEC = "extra_tap_interval_sec"
    }
}
