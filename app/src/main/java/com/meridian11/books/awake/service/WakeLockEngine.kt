package com.meridian11.books.awake.service

import android.content.Context
import android.os.PowerManager

/**
 * Acquires and releases CPU (`PARTIAL_WAKE_LOCK`) and Display
 * (`SCREEN_BRIGHT_WAKE_LOCK` or `SCREEN_DIM_WAKE_LOCK`) wake locks with
 * bounded safety timeouts so a force-killed process never leaks an infinite lock.
 *
 * Renewals extend held non-reference-counted locks in-place without releasing first,
 * preventing dimmed displays from pulsing bright or brief suspend windows during switches.
 */
class WakeLockEngine(context: Context) {

    private val powerManager = context.applicationContext.getSystemService(PowerManager::class.java)
    private var cpuWakeLock: PowerManager.WakeLock? = null
    private var screenWakeLock: PowerManager.WakeLock? = null

    var lastAcquiredScreenWakeLockFlags: Int = 0
        private set

    @Synchronized
    fun acquire(
        allowScreenDimming: Boolean,
        timeoutMillis: Long = DEFAULT_SAFETY_TIMEOUT_MS
    ) {
        val pm = powerManager ?: return
        val boundedTimeout = timeoutMillis.coerceAtLeast(MIN_TIMEOUT_MS)

        @Suppress("DEPRECATION")
        val screenFlags = if (allowScreenDimming) {
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE
        } else {
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE
        }

        val existingCpu = cpuWakeLock
        val existingScreen = screenWakeLock
        if (existingCpu != null &&
            existingScreen != null &&
            lastAcquiredScreenWakeLockFlags == screenFlags
        ) {
            existingCpu.acquire(boundedTimeout)
            existingScreen.acquire(boundedTimeout)
            return
        }

        val newCpuLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            CPU_WAKELOCK_TAG
        ).apply {
            setReferenceCounted(false)
            acquire(boundedTimeout)
        }

        val newDisplayLock = try {
            pm.newWakeLock(
                screenFlags,
                SCREEN_WAKELOCK_TAG
            ).apply {
                setReferenceCounted(false)
                acquire(boundedTimeout)
            }
        } catch (t: Throwable) {
            runCatching { if (newCpuLock.isHeld) newCpuLock.release() }
            throw t
        }

        releaseQuietly(existingCpu)
        releaseQuietly(existingScreen)

        cpuWakeLock = newCpuLock
        screenWakeLock = newDisplayLock
        lastAcquiredScreenWakeLockFlags = screenFlags
    }

    @Synchronized
    fun isHeld(): Boolean {
        return (cpuWakeLock?.isHeld == true) && (screenWakeLock?.isHeld == true)
    }

    @Synchronized
    fun release() {
        releaseQuietly(cpuWakeLock)
        cpuWakeLock = null
        releaseQuietly(screenWakeLock)
        screenWakeLock = null
    }

    private fun releaseQuietly(lock: PowerManager.WakeLock?) {
        try {
            if (lock != null && lock.isHeld) {
                lock.release()
            }
        } catch (_: Throwable) {
        }
    }

    companion object {
        const val CPU_WAKELOCK_TAG = "com.meridian11.books.awake:CpuKeepAwake"
        const val SCREEN_WAKELOCK_TAG = "com.meridian11.books.awake:ScreenKeepAwake"
        const val DEFAULT_SAFETY_TIMEOUT_MS = 12L * 60L * 60L * 1000L // 12 hours safety cap
        private const val MIN_TIMEOUT_MS = 1_000L
    }
}
