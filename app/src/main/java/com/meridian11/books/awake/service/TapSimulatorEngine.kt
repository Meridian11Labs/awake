package com.meridian11.books.awake.service

import com.meridian11.books.awake.domain.AwakeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Periodically dispatches a 1ms stationary micro-tap at the bottom-right corner pixel
 * `(displayWidth - 1f, displayHeight - 1f)` to reset system/session idle timers when
 * standard WakeLocks cannot be used.
 */
class TapSimulatorEngine(
    private val scope: CoroutineScope,
    private val displayBoundsProvider: () -> Pair<Int, Int>,
    private val gestureDispatcher: (x: Float, y: Float, durationMs: Long) -> Unit
) {
    private var tickerJob: Job? = null

    fun start(intervalSeconds: Int) {
        stop()
        val clampedSeconds = intervalSeconds.coerceIn(
            AwakeState.MIN_TAP_INTERVAL_SECONDS,
            AwakeState.MAX_TAP_INTERVAL_SECONDS
        )
        val intervalMs = clampedSeconds * 1_000L

        tickerJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                if (!isActive) break
                dispatchSinglePulse()
            }
        }
    }

    fun dispatchSinglePulse() {
        val (width, height) = displayBoundsProvider()
        val safeX = (width - 1).coerceAtLeast(1).toFloat()
        val safeY = (height - 1).coerceAtLeast(1).toFloat()
        gestureDispatcher(safeX, safeY, PULSE_DURATION_MS)
    }

    fun isRunning(): Boolean = tickerJob?.isActive == true

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
    }

    companion object {
        const val PULSE_DURATION_MS = 1L
    }
}
