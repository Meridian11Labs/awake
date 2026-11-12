package com.meridian11.books.awake.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.domain.AwakeState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Non-exported internal BroadcastReceiver handling interactive clicks from
 * [AwakeSquircleWidgetProvider] `PendingIntent`s.
 *
 * Isolating custom actions into an unexported receiver prevents external untrusted apps
 * from spoofingKeep-Awake toggle or duration broadcasts on the exported `AppWidgetProvider`.
 */
class AwakeWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val app = context?.applicationContext as? AwakeApp ?: return
        val action = intent?.action ?: return

        when (action) {
            AwakeSquircleWidgetProvider.ACTION_WIDGET_TOGGLE -> {
                val hasExplicitTarget = intent.hasExtra(AwakeSquircleWidgetProvider.EXTRA_TARGET_AWAKE)
                val targetAwake = intent.getBooleanExtra(AwakeSquircleWidgetProvider.EXTRA_TARGET_AWAKE, false)
                val pendingResult = goAsync()
                app.appScope.launch {
                    try {
                        withTimeoutOrNull(BROADCAST_TIMEOUT_MS) {
                            if (hasExplicitTarget) {
                                app.controller.setAwake(targetAwake)
                            } else {
                                app.controller.toggle()
                            }
                        }
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            AwakeSquircleWidgetProvider.ACTION_WIDGET_CYCLE_DURATION -> {
                val pendingResult = goAsync()
                app.appScope.launch {
                    try {
                        withTimeoutOrNull(BROADCAST_TIMEOUT_MS) {
                            app.controller.cycleDurationPresetAndApply()
                        }
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            AwakeSquircleWidgetProvider.ACTION_WIDGET_SET_DURATION -> {
                val minutes = intent.getIntExtra(AwakeSquircleWidgetProvider.EXTRA_DURATION_MINUTES, -1)
                if (minutes !in AwakeState.DURATION_PRESETS) return
                val pendingResult = goAsync()
                app.appScope.launch {
                    try {
                        withTimeoutOrNull(BROADCAST_TIMEOUT_MS) {
                            app.controller.enableWithDuration(minutes)
                        }
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val BROADCAST_TIMEOUT_MS = 8_000L
    }
}
