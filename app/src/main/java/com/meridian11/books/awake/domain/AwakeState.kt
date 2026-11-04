package com.meridian11.books.awake.domain

/**
 * Immutable domain state and user preferences for the Awake Googlebook utility.
 */
data class AwakeState(
    val isAwake: Boolean = false,
    val durationMinutes: Int = 0, // 0 = Indefinite, or 30, 60, 120, 240
    val sessionExpiresAtEpochMillis: Long = 0L,
    val lowBatteryCutoffEnabled: Boolean = true,
    val lowBatteryThresholdPercent: Int = 20,
    val autoAwakeOnChargerEnabled: Boolean = false,
    val lidCloseAutoShutoffEnabled: Boolean = true,
    val allowScreenDimming: Boolean = false,
    val directToggleOnTap: Boolean = false // false = Open window on tap (default); true = Hide settings, tap icon to toggle
) {
    companion object {
        val DURATION_PRESETS = listOf(0, 30, 60, 120)
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
    fun startOrUpdateKeepAwake(state: AwakeState)
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
