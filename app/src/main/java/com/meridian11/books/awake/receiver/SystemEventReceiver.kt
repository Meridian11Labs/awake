package com.meridian11.books.awake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meridian11.books.awake.AwakeApp
import kotlinx.coroutines.launch

/**
 * Reconciles Taskbar Coffee Cup icon and Keep-Awake state across device reboots and app updates.
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
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
