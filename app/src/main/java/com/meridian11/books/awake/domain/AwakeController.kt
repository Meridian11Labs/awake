package com.meridian11.books.awake.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Mutex-serialized domain controller coordinating Awake state transitions,
 * Taskbar coffee cup icon switching, right-click dynamic shortcut binding,
 * countdown timer expiration, and Googlebook hardware guardrails.
 */
class AwakeController(
    private val repository: AwakePreferencesRepository,
    private val iconPort: IconSwitcherPort,
    private val shortcutPort: ShortcutPublisherPort,
    private val enginePort: KeepAwakeEnginePort,
    private val externalScope: CoroutineScope,
    private val widgetPort: WidgetUpdaterPort = NoOpWidgetUpdaterPort,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()
    private var expirationJob: Job? = null

    /**
     * Toggles between Empty Coffee Cup (normal suspend/resume) and Full Coffee Cup (keep awake).
     */
    suspend fun toggle(): AwakeState = mutex.withLock {
        val current = repository.stateFlow.first()
        val targetAwake = !current.isAwake
        applyAwakeTransitionLocked(targetAwake = targetAwake, durationMinutes = current.durationMinutes)
    }

    /**
     * Explicitly enables Keep Awake with the specified duration preset (0 = Indefinite).
     */
    suspend fun enableWithDuration(durationMinutes: Int): AwakeState = mutex.withLock {
        repository.setDurationMinutes(durationMinutes)
        applyAwakeTransitionLocked(targetAwake = true, durationMinutes = durationMinutes)
    }

    /**
     * Cycles through the duration presets (Indefinite -> 30m -> 60m -> 120m -> Indefinite).
     * If currently awake, resets the timer to the newly selected duration; if inactive, enables with that duration.
     */
    suspend fun cycleDurationPresetAndApply(): AwakeState = mutex.withLock {
        val nextDuration = repository.cycleDurationPreset()
        val current = repository.stateFlow.first()
        val targetAwake = if (!current.isAwake && nextDuration > 0) true else current.isAwake
        applyAwakeTransitionLocked(targetAwake = targetAwake, durationMinutes = nextDuration)
    }

    /**
     * Toggles the simulated tap fallback on/off and updates live shortcuts/engines.
     */
    suspend fun toggleTapSimulation(): AwakeState = mutex.withLock {
        val current = repository.stateFlow.first()
        repository.setTapSimulationConfig(
            enabled = !current.tapSimulationEnabled,
            intervalSeconds = current.tapIntervalSeconds
        )
        val updated = repository.stateFlow.first()
        if (updated.isAwake) {
            enginePort.startOrUpdateKeepAwake(updated)
        }
        shortcutPort.publishShortcuts(updated)
        widgetPort.updateWidgets(updated)
        updated
    }

    /**
     * Refreshes engines, taskbar icon, right-click shortcuts, and squircle widget after settings are modified.
     */
    suspend fun syncCurrentState(): AwakeState = mutex.withLock {
        val current = repository.stateFlow.first()
        val now = clockMillis()
        if (current.isAwake && current.sessionExpiresAtEpochMillis in 1..now) {
            return@withLock applyAwakeTransitionLocked(targetAwake = false, durationMinutes = current.durationMinutes)
        }
        iconPort.setCoffeeCupIcon(isFullCup = current.isAwake)
        if (current.isAwake) {
            enginePort.startOrUpdateKeepAwake(current)
            scheduleExpirationLocked(current.sessionExpiresAtEpochMillis)
        } else {
            enginePort.stopKeepAwake()
            expirationJob?.cancel()
            expirationJob = null
        }
        shortcutPort.publishShortcuts(current)
        widgetPort.updateWidgets(current)
        current
    }

    /**
     * Explicitly turns off Keep Awake and restores Empty Coffee Cup.
     */
    suspend fun disableKeepAwake(): AwakeState = mutex.withLock {
        val current = repository.stateFlow.first()
        applyAwakeTransitionLocked(targetAwake = false, durationMinutes = current.durationMinutes)
    }

    /**
     * Extends the current active session by [additionalMinutes] (e.g., +30m from notification).
     */
    suspend fun extendSession(additionalMinutes: Int): AwakeState = mutex.withLock {
        val current = repository.stateFlow.first()
        val now = clockMillis()
        val baseMillis = if (current.isAwake && current.sessionExpiresAtEpochMillis > now) {
            current.sessionExpiresAtEpochMillis
        } else {
            now
        }
        val newExpiresAt = baseMillis + additionalMinutes * 60_000L
        val updatedDurationMinutes = ((newExpiresAt - now + 59_999L) / 60_000L).toInt()
        repository.setDurationMinutes(updatedDurationMinutes)
        repository.setAwakeState(isAwake = true, expiresAtEpochMillis = newExpiresAt)
        val updated = repository.stateFlow.first()
        iconPort.setCoffeeCupIcon(isFullCup = true)
        enginePort.startOrUpdateKeepAwake(updated)
        shortcutPort.publishShortcuts(updated)
        widgetPort.updateWidgets(updated)
        scheduleExpirationLocked(newExpiresAt)
        updated
    }

    /**
     * Evaluates low-battery guardrail when battery level or charging state changes.
     */
    suspend fun onBatteryStateChanged(levelPercent: Int, isCharging: Boolean) = mutex.withLock {
        val current = repository.stateFlow.first()
        if (current.isAwake &&
            current.lowBatteryCutoffEnabled &&
            !isCharging &&
            levelPercent <= current.lowBatteryThresholdPercent
        ) {
            applyAwakeTransitionLocked(targetAwake = false, durationMinutes = current.durationMinutes)
        }
    }

    /**
     * Evaluates "Plug-in Brew" (Auto-Awake on AC Charger) when power is connected or disconnected.
     */
    suspend fun onChargerStateChanged(isPluggedIn: Boolean) = mutex.withLock {
        val current = repository.stateFlow.first()
        if (current.autoAwakeOnChargerEnabled) {
            applyAwakeTransitionLocked(targetAwake = isPluggedIn, durationMinutes = current.durationMinutes)
        }
    }

    /**
     * Evaluates Lid-Close / Backpack guardrail when the internal laptop display turns off.
     */
    suspend fun onPrimaryDisplayOff(hasActiveExternalDisplay: Boolean) = mutex.withLock {
        val current = repository.stateFlow.first()
        if (current.isAwake && current.lidCloseAutoShutoffEnabled && !hasActiveExternalDisplay) {
            applyAwakeTransitionLocked(targetAwake = false, durationMinutes = current.durationMinutes)
        }
    }

    private suspend fun applyAwakeTransitionLocked(
        targetAwake: Boolean,
        durationMinutes: Int
    ): AwakeState {
        val currentJob = kotlin.coroutines.coroutineContext[Job]
        if (expirationJob != null && expirationJob !== currentJob) {
            expirationJob?.cancel()
        }
        expirationJob = null

        val expiresAt = if (targetAwake && durationMinutes > 0) {
            clockMillis() + durationMinutes * 60_000L
        } else {
            0L
        }

        repository.setAwakeState(isAwake = targetAwake, expiresAtEpochMillis = expiresAt)
        val updated = repository.stateFlow.first()

        // 1. Swap Taskbar Coffee Cup Icon FIRST so active alias is enabled before shortcut binding
        iconPort.setCoffeeCupIcon(isFullCup = targetAwake)

        // 2. Start or stop power engines
        if (targetAwake) {
            enginePort.startOrUpdateKeepAwake(updated)
            scheduleExpirationLocked(expiresAt)
        } else {
            enginePort.stopKeepAwake()
        }

        // 3. Re-bind right-click dynamic shortcuts to the newly enabled <activity-alias>
        shortcutPort.publishShortcuts(updated)

        // 4. Synchronize 1x1 Squircle AppWidget and Quick Settings Tile
        widgetPort.updateWidgets(updated)

        return updated
    }

    private fun scheduleExpirationLocked(expiresAtEpochMillis: Long) {
        expirationJob?.cancel()
        expirationJob = null
        if (expiresAtEpochMillis <= 0L) return

        val remainingMillis = (expiresAtEpochMillis - clockMillis()).coerceAtLeast(1L)
        expirationJob = externalScope.launch {
            delay(remainingMillis)
            disableKeepAwake()
        }
    }
}
