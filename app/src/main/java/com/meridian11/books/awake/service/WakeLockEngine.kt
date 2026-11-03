package com.meridian11.books.awake.service

import android.content.Context
import android.os.PowerManager

/**
 * Acquires and releases CPU (`PARTIAL_WAKE_LOCK`) and Display
 * (`SCREEN_BRIGHT_WAKE_LOCK` or `SCREEN_DIM_WAKE_LOCK`) wake locks with
 * bounded safety timeouts so a force-killed process never leaks an infinite lock.
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
        release()

        val pm = powerManager ?: return

        val cpuLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            CPU_WAKELOCK_TAG
        ).apply {
            setReferenceCounted(false)
            acquire(timeoutMillis)
        }
        cpuWakeLock = cpuLock

        @Suppress("DEPRECATION")
        val screenFlags = if (allowScreenDimming) {
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE
        } else {
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE
        }
        lastAcquiredScreenWakeLockFlags = screenFlags

        val displayLock = pm.newWakeLock(
            screenFlags,
            SCREEN_WAKELOCK_TAG
        ).apply {
            setReferenceCounted(false)
            acquire(timeoutMillis)
        }
        screenWakeLock = displayLock
    }

    @Synchronized
    fun isHeld(): Boolean {
        return (cpuWakeLock?.isHeld == true) || (screenWakeLock?.isHeld == true)
    }

    @Synchronized
    fun release() {
        try {
            cpuWakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Throwable) {
        } finally {
            cpuWakeLock = null
        }

        try {
            screenWakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Throwable) {
        } finally {
            screenWakeLock = null
        }
    }

    companion object {
        const val CPU_WAKELOCK_TAG = "com.meridian11.books.awake:CpuKeepAwake"
        const val SCREEN_WAKELOCK_TAG = "com.meridian11.books.awake:ScreenKeepAwake"
        const val DEFAULT_SAFETY_TIMEOUT_MS = 12L * 60L * 60L * 1000L // 12 hours safety cap
    }
}
