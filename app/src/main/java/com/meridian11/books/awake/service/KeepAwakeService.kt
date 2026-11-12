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
import android.os.SystemClock
import android.util.Log
import android.view.Display
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service (`foregroundServiceType="specialUse"`) that holds the active
 * [WakeLockEngine], monitors Googlebook hardware guardrails (Lid-Close display state,
 * Low-Battery cutoff, and multi-user foreground switches), and provides system-tray quick actions.
 */
class KeepAwakeService : Service() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled error in KeepAwakeService", throwable)
    }
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + exceptionHandler
    )

    private lateinit var wakeLockEngine: WakeLockEngine
    private var wakeLockRenewalJob: Job? = null
    private var displayManager: DisplayManager? = null
    private var isBatteryReceiverRegistered = false
    private var isUserSwitchReceiverRegistered = false
    private var isUserInBackground = false
    private var currentAllowDimming = false
    private var currentExpiresAtElapsedMillis = 0L

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = checkDisplayStates()
        override fun onDisplayChanged(displayId: Int) = checkDisplayStates()
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleBatteryIntent(intent)
        }
    }

    private val userSwitchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_BACKGROUND -> {
                    isUserInBackground = true
                    wakeLockRenewalJob?.cancel()
                    wakeLockRenewalJob = null
                    wakeLockEngine.release()
                }
                Intent.ACTION_USER_FOREGROUND -> {
                    isUserInBackground = false
                    acquireAndScheduleWakeLockRenewal(
                        allowScreenDimming = currentAllowDimming,
                        expiresAtElapsedMillis = currentExpiresAtElapsedMillis
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        wakeLockEngine = WakeLockEngine(this)
        createNotificationChannel()
        registerHardwareMonitors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as? AwakeApp
        when (intent?.action) {
            ACTION_STOP_KEEP_AWAKE -> {
                wakeLockRenewalJob?.cancel()
                wakeLockRenewalJob = null
                wakeLockEngine.release()
                if (app != null) {
                    // Stop service only after DataStore + Taskbar icon transition finishes
                    app.appScope.launch {
                        try {
                            app.controller.disableKeepAwake()
                        } finally {
                            stopSelf()
                        }
                    }
                } else {
                    stopSelf()
                }
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
        val expiresAtElapsedMillis = intent?.getLongExtra(EXTRA_EXPIRES_AT_ELAPSED_MILLIS, 0L) ?: 0L

        currentAllowDimming = allowDimming
        currentExpiresAtElapsedMillis = expiresAtElapsedMillis

        val notification = buildOngoingNotification(
            durationMinutes = durationMinutes,
            allowDimming = allowDimming,
            expiresAtElapsedMillis = expiresAtElapsedMillis
        )

        startForegroundCompat(notification)
        if (!isUserInBackground) {
            acquireAndScheduleWakeLockRenewal(
                allowScreenDimming = allowDimming,
                expiresAtElapsedMillis = expiresAtElapsedMillis
            )
        }
        evaluateCurrentStickyBatteryState()

        if (intent == null) {
            // Service was restarted by OS (START_STICKY) — reconcile against persisted expiration & bootCount
            serviceScope.launch {
                val synced = app?.controller?.syncCurrentState()
                if (synced == null || !synced.isAwake) {
                    stopSelf()
                }
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

    private fun acquireAndScheduleWakeLockRenewal(
        allowScreenDimming: Boolean,
        expiresAtElapsedMillis: Long
    ) {
        wakeLockEngine.acquire(
            allowScreenDimming = allowScreenDimming,
            timeoutMillis = computeBoundedWakeLockTimeoutMs(expiresAtElapsedMillis)
        )
        wakeLockRenewalJob?.cancel()
        wakeLockRenewalJob = serviceScope.launch {
            while (isActive) {
                delay(WAKELOCK_RENEWAL_INTERVAL_MS)
                if (isActive && !isUserInBackground) {
                    wakeLockEngine.acquire(
                        allowScreenDimming = allowScreenDimming,
                        timeoutMillis = computeBoundedWakeLockTimeoutMs(expiresAtElapsedMillis)
                    )
                }
            }
        }
    }

    private fun computeBoundedWakeLockTimeoutMs(expiresAtElapsedMillis: Long): Long {
        if (expiresAtElapsedMillis <= 0L) {
            return WakeLockEngine.DEFAULT_SAFETY_TIMEOUT_MS
        }
        val remainingWithGrace = expiresAtElapsedMillis - SystemClock.elapsedRealtime() + 60_000L
        return remainingWithGrace.coerceIn(60_000L, WakeLockEngine.DEFAULT_SAFETY_TIMEOUT_MS)
    }

    private fun handleBatteryIntent(intent: Intent?) {
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

    private fun evaluateCurrentStickyBatteryState() {
        runCatching {
            val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            handleBatteryIntent(sticky)
        }
    }

    private fun checkDisplayStates() {
        val dm = displayManager ?: return
        val defaultDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY)
        val isPrimaryOff = defaultDisplay != null && defaultDisplay.state == Display.STATE_OFF
        if (!isPrimaryOff) return

        // Only count real public presentation external monitors (excluding virtual screen-cast/private displays)
        val presentationDisplays = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        val hasActiveExternal = presentationDisplays.any { display ->
            display.displayId != Display.DEFAULT_DISPLAY &&
                display.state == Display.STATE_ON &&
                (display.flags and Display.FLAG_PRIVATE) == 0
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
        try {
            val userFilter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_BACKGROUND)
                addAction(Intent.ACTION_USER_FOREGROUND)
            }
            ContextCompat.registerReceiver(
                this,
                userSwitchReceiver,
                userFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isUserSwitchReceiverRegistered = true
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
        if (isUserSwitchReceiverRegistered) {
            try {
                unregisterReceiver(userSwitchReceiver)
            } catch (_: Throwable) {
            }
            isUserSwitchReceiverRegistered = false
        }
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_description)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildOngoingNotification(
        durationMinutes: Int,
        allowDimming: Boolean,
        expiresAtElapsedMillis: Long
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

        val isFiniteTimer = expiresAtElapsedMillis > 0L || durationMinutes > 0
        val durationText = if (expiresAtElapsedMillis > 0L && durationMinutes == 0) {
            getString(R.string.notif_duration_timed)
        } else {
            AndroidShortcutPublisher.formatDurationLong(durationMinutes)
        }
        val brightnessMode = if (allowDimming) {
            getString(R.string.notif_mode_dimming_allowed)
        } else {
            getString(R.string.notif_mode_full_brightness)
        }
        val modeDetails = getString(R.string.notif_content_format, durationText, brightnessMode)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_coffee_full)
            .setContentTitle(getString(R.string.notif_title_active))
            .setContentText(modeDetails)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(settingsPendingIntent)
            .addAction(
                R.drawable.ic_stat_coffee_empty,
                getString(R.string.notif_action_turn_off),
                stopPendingIntent
            )

        if (expiresAtElapsedMillis > 0L) {
            val remainingMs = (expiresAtElapsedMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            builder
                .setWhen(System.currentTimeMillis() + remainingMs)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }

        if (isFiniteTimer) {
            builder.addAction(
                R.drawable.ic_shortcut_timer,
                getString(R.string.notif_action_extend_30m),
                extendPendingIntent
            )
        }

        builder.addAction(
            R.drawable.ic_shortcut_settings,
            getString(R.string.notif_action_settings),
            settingsPendingIntent
        )

        return builder.build()
    }

    override fun onDestroy() {
        isRunning = false
        wakeLockRenewalJob?.cancel()
        wakeLockRenewalJob = null
        unregisterHardwareMonitors()
        wakeLockEngine.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "KeepAwakeService"
        const val CHANNEL_ID = "awake_status_channel"
        const val NOTIFICATION_ID = 1101
        private const val WAKELOCK_RENEWAL_INTERVAL_MS = 4L * 60L * 60L * 1000L // Renew every 4h

        @Volatile
        var isRunning: Boolean = false
            private set

        const val ACTION_START_OR_UPDATE = "com.meridian11.books.awake.action.START_OR_UPDATE"
        const val ACTION_STOP_KEEP_AWAKE = "com.meridian11.books.awake.action.STOP_KEEP_AWAKE"
        const val ACTION_EXTEND_30_MIN = "com.meridian11.books.awake.action.EXTEND_30_MIN"

        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_ALLOW_DIMMING = "extra_allow_dimming"
        const val EXTRA_EXPIRES_AT_ELAPSED_MILLIS = "extra_expires_at_elapsed_millis"
    }
}
