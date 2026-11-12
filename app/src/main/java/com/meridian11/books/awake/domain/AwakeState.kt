package com.meridian11.books.awake.domain

/**
 * Immutable domain state and user preferences for the Awake Googlebook utility.
 *
 * Note: When [isAwake] is true and timed, [sessionExpiresAtEpochMillis] stores the deadline
 * in the active controller clock domain (`SystemClock.elapsedRealtime()` in production,
 * paired with [sessionBootCount] so cross-reboot state is deterministically invalidated).
 */
data class AwakeState(
    val isAwake: Boolean = false,
    val durationMinutes: Int = 0, // 0 = Infinite, or 30, 60, 120
    val sessionExpiresAtEpochMillis: Long = 0L,
    val lowBatteryCutoffEnabled: Boolean = true,
    val lowBatteryThresholdPercent: Int = 20,
    val autoAwakeOnChargerEnabled: Boolean = false,
    val lidCloseAutoShutoffEnabled: Boolean = true,
    val allowScreenDimming: Boolean = false,
    val directToggleOnTap: Boolean = false, // false = Open window on tap (default); true = Hide settings, tap icon to toggle
    val sessionBootCount: Int = -1
) {
    fun isEffectivelyAwake(nowMillis: Long, currentBootCount: Int = -1): Boolean {
        if (!isAwake) return false
        if (currentBootCount >= 0 && sessionBootCount >= 0 && sessionBootCount != currentBootCount) {
            return false
        }
        if (sessionExpiresAtEpochMillis in 1..nowMillis) {
            return false
        }
        return true
    }

    fun remainingMinutesCeil(nowMillis: Long): Int {
        if (sessionExpiresAtEpochMillis <= 0L) return 0
        val remainingMs = (sessionExpiresAtEpochMillis - nowMillis).coerceAtLeast(0L)
        return ((remainingMs + 59_999L) / 60_000L).toInt()
    }

    companion object {
        val DURATION_PRESETS = listOf(0, 30, 60, 120)
        const val MAX_DURATION_MINUTES = 24 * 60
    }
}

/**
 * Port for switching the Taskbar launcher icon between Half-Empty Coffee Cup (normal suspend)
 * and Full Coffee Cup (keep awake active).
 */
interface IconSwitcherPort {
    fun setCoffeeCupIcon(isFullCup: Boolean)
    fun isFullCupAliasEnabled(): Boolean
}

/**
 * Port for publishing and re-binding the 3 right-click Taskbar dynamic shortcuts
 * to the currently enabled <activity-alias>.
 */
interface ShortcutPublisherPort {
    fun publishShortcuts(state: AwakeState)
}

/**
 * Port for starting, updating, or stopping the active Keep-Awake power engines.
 */
interface KeepAwakeEnginePort {
    /**
     * Starts or updates the Keep-Awake foreground engine.
     * @return true if started/updated successfully, false if blocked by OS background restrictions.
     */
    fun startOrUpdateKeepAwake(state: AwakeState): Boolean
    fun stopKeepAwake()
}

/**
 * Port for keeping the 1x1 Squircle AppWidget and Quick Settings Tile synchronized
 * whenever AwakeState transitions.
 */
interface WidgetUpdaterPort {
    fun updateWidgets(state: AwakeState)
}

object NoOpWidgetUpdaterPort : WidgetUpdaterPort {
    override fun updateWidgets(state: AwakeState) = Unit
}

/**
 * Port for scheduling OS-level wakeup alarms so timed Keep-Awake sessions expire
 * reliably even if the process is cached or killed.
 */
interface ExpirationAlarmPort {
    fun scheduleExpirationAlarm(triggerAtElapsedMillis: Long)
    fun cancelExpirationAlarm()
}

object NoOpExpirationAlarmPort : ExpirationAlarmPort {
    override fun scheduleExpirationAlarm(triggerAtElapsedMillis: Long) = Unit
    override fun cancelExpirationAlarm() = Unit
}
