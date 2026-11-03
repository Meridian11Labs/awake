package com.meridian11.books.awake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meridian11.books.awake.AwakeApp
import kotlinx.coroutines.launch

/**
 * Reconciles Taskbar Coffee Cup icon and Keep-Awake state across device reboots,
 * app updates, and AC charger connect/disconnect events.
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val app = context?.applicationContext as? AwakeApp ?: return
        val action = intent?.action ?: return

        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED -> {
                        // Reset to Empty Coffee Cup on fresh boot so taskbar icon matches stopped service
                        app.controller.disableKeepAwake()
                    }
                    Intent.ACTION_POWER_CONNECTED -> {
                        app.controller.onChargerStateChanged(isPluggedIn = true)
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        app.controller.onChargerStateChanged(isPluggedIn = false)
                    }
                    Intent.ACTION_BATTERY_LOW -> {
                        app.controller.onBatteryStateChanged(levelPercent = 15, isCharging = false)
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
