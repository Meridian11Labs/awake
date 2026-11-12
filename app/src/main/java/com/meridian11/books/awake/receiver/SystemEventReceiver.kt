package com.meridian11.books.awake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meridian11.books.awake.AwakeApp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Reconciles Taskbar Coffee Cup icon and Keep-Awake state across device reboots and app updates:
 * - [Intent.ACTION_BOOT_COMPLETED]: Resets Keep-Awake to OFF so the Taskbar icon matches the
 *   freshly booted system state (stopped service / normal sleep).
 * - [Intent.ACTION_MY_PACKAGE_REPLACED]: Preserves any active Keep-Awake session across app updates
 *   by calling [com.meridian11.books.awake.domain.AwakeController.syncCurrentState].
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val app = context?.applicationContext as? AwakeApp ?: return
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                withTimeoutOrNull(BROADCAST_TIMEOUT_MS) {
                    app.cancelStartupSync()
                    when (action) {
                        Intent.ACTION_BOOT_COMPLETED -> {
                            app.controller.disableKeepAwake()
                        }
                        Intent.ACTION_MY_PACKAGE_REPLACED -> {
                            app.controller.syncCurrentState()
                        }
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        private const val BROADCAST_TIMEOUT_MS = 8_000L
    }
}
