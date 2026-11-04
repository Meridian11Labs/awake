package com.meridian11.books.awake.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.domain.KeepAwakeEnginePort

/**
 * Platform adapter implementing [KeepAwakeEnginePort] by starting/stopping [KeepAwakeService].
 */
class AndroidKeepAwakeEngineAdapter(
    private val context: Context
) : KeepAwakeEnginePort {

    override fun startOrUpdateKeepAwake(state: AwakeState) {
        val intent = Intent(context, KeepAwakeService::class.java).apply {
            action = KeepAwakeService.ACTION_START_OR_UPDATE
            putExtra(KeepAwakeService.EXTRA_DURATION_MINUTES, state.durationMinutes)
            putExtra(KeepAwakeService.EXTRA_ALLOW_DIMMING, state.allowScreenDimming)
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Throwable) {
        }
    }

    override fun stopKeepAwake() {
        val intent = Intent(context, KeepAwakeService::class.java)
        try {
            context.stopService(intent)
        } catch (_: Throwable) {
        }
    }
}
