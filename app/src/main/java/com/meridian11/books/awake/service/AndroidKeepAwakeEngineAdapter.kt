package com.meridian11.books.awake.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.domain.KeepAwakeEnginePort

/**
 * Platform adapter implementing [KeepAwakeEnginePort] by starting/stopping [KeepAwakeService].
 */
class AndroidKeepAwakeEngineAdapter(
    private val context: Context
) : KeepAwakeEnginePort {

    @Volatile
    private var startRequested: Boolean = false

    override fun startOrUpdateKeepAwake(state: AwakeState): Boolean {
        val intent = Intent(context, KeepAwakeService::class.java).apply {
            action = KeepAwakeService.ACTION_START_OR_UPDATE
            putExtra(KeepAwakeService.EXTRA_DURATION_MINUTES, state.durationMinutes)
            putExtra(KeepAwakeService.EXTRA_ALLOW_DIMMING, state.allowScreenDimming)
            putExtra(KeepAwakeService.EXTRA_EXPIRES_AT_ELAPSED_MILLIS, state.sessionExpiresAtEpochMillis)
        }
        return try {
            ContextCompat.startForegroundService(context, intent)
            startRequested = true
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Blocked from starting KeepAwakeService from current context", t)
            false
        }
    }

    override fun stopKeepAwake() {
        if (!KeepAwakeService.isRunning && !startRequested) {
            return
        }
        startRequested = false
        val intent = Intent(context, KeepAwakeService::class.java)
        try {
            context.stopService(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to stop KeepAwakeService", t)
        }
    }

    companion object {
        private const val TAG = "KeepAwakeEngineAdapter"
    }
}
