package com.meridian11.books.awake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meridian11.books.awake.AwakeApp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Non-exported receiver triggered by [com.meridian11.books.awake.platform.AndroidExpirationAlarmScheduler]
 * when a timed Keep-Awake session reaches its deadline.
 *
 * Calls [com.meridian11.books.awake.domain.AwakeController.syncCurrentState] so that:
 * - Expired sessions cleanly transition OFF (releasing WakeLock, resetting icon, widget, and tile).
 * - Sessions that were extended right as the alarm was in flight remain active.
 */
class SessionExpirationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_SESSION_EXPIRED) return
        val app = context?.applicationContext as? AwakeApp ?: return

        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                withTimeoutOrNull(BROADCAST_TIMEOUT_MS) {
                    app.controller.syncCurrentState()
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        const val ACTION_SESSION_EXPIRED = "com.meridian11.books.awake.action.SESSION_EXPIRED"
        private const val BROADCAST_TIMEOUT_MS = 8_000L
    }
}
