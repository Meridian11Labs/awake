package com.meridian11.books.awake.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Mutex-serialized, cancellation-safe domain controller coordinating Awake state transitions,
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
    private val alarmPort: ExpirationAlarmPort = NoOpExpirationAlarmPort,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val bootCountProvider: () -> Int = { -1 }
) {
    private val mutex = Mutex()
    private var expirationJob: Job? = null

    /**
     * Toggles between Empty Coffee Cup (normal suspend/resume) and Full Coffee Cup (keep awake).
     * Evaluates effective liveness (expiration + boot count) so an expired session toggles ON cleanly.
     */
    suspend fun toggle(): AwakeState = runLockedNonCancellable {
        val current = repository.stateFlow.first()
        val now = clockMillis()
        val wasEffectivelyAwake = current.isEffectivelyAwake(now, bootCountProvider())
        applyAwakeTransitionLocked(
            targetAwake = !wasEffectivelyAwake,
            durationMinutes = current.durationMinutes
        )
    }

    /**
     * Idempotently sets Keep Awake to [targetAwake] using the current saved duration preset.
     * Used by stateful Widget/Tile intents so tapping an "ON" surface always turns Awake OFF
     * even if the timer had already expired in the background.
     */
    suspend fun setAwake(targetAwake: Boolean): AwakeState = runLockedNonCancellable {
        val current = repository.stateFlow.first()
        applyAwakeTransitionLocked(
            targetAwake = targetAwake,
            durationMinutes = current.durationMinutes
        )
    }

    /**
     * Explicitly enables Keep Awake with the specified duration preset (0 = Infinite).
     */
    suspend fun enableWithDuration(durationMinutes: Int): AwakeState = runLockedNonCancellable {
        val sanitized = durationMinutes.coerceIn(0, AwakeState.MAX_DURATION_MINUTES)
        applyAwakeTransitionLocked(targetAwake = true, durationMinutes = sanitized)
    }

    /**
     * Selects a duration preset from the Settings window:
     * - If Keep Awake is currently active, resets the active session timer to the new preset.
     * - If inactive, saves the preset for future sessions and refreshes shortcuts & widgets.
     */
    suspend fun selectDurationPreset(durationMinutes: Int): AwakeState = runLockedNonCancellable {
        val sanitized = durationMinutes.coerceIn(0, AwakeState.MAX_DURATION_MINUTES)
        val current = repository.stateFlow.first()
        val isCurrentlyAwake = current.isEffectivelyAwake(clockMillis(), bootCountProvider())
        if (isCurrentlyAwake) {
            applyAwakeTransitionLocked(targetAwake = true, durationMinutes = sanitized)
        } else {
            val updated = repository.updateState {
                it.copy(
                    isAwake = false,
                    durationMinutes = sanitized,
                    sessionExpiresAtEpochMillis = 0L,
                    sessionBootCount = -1
                )
            }
            shortcutPort.publishShortcuts(updated)
            widgetPort.updateWidgets(updated)
            updated
        }
    }

    /**
     * Updates the "Hide settings, tap icon to toggle" preference under controller serialization.
     */
    suspend fun setDirectToggleOnTap(enabled: Boolean): AwakeState = runLockedNonCancellable {
        repository.updateState { it.copy(directToggleOnTap = enabled) }
    }

    /**
     * Atomically updates guardrail settings and refreshes the running engine if active.
     */
    suspend fun updateGuardrails(
        lowBatteryCutoffEnabled: Boolean,
        autoAwakeOnChargerEnabled: Boolean,
        lidCloseAutoShutoffEnabled: Boolean,
        allowScreenDimming: Boolean
    ): AwakeState = runLockedNonCancellable {
        val now = clockMillis()
        val bootCount = bootCountProvider()
        val updated = repository.updateState { current ->
            current.copy(
                lowBatteryCutoffEnabled = lowBatteryCutoffEnabled,
                autoAwakeOnChargerEnabled = autoAwakeOnChargerEnabled,
                lidCloseAutoShutoffEnabled = lidCloseAutoShutoffEnabled,
                allowScreenDimming = allowScreenDimming
            )
        }
        if (updated.isEffectivelyAwake(now, bootCount)) {
            if (!enginePort.startOrUpdateKeepAwake(updated)) {
                return@runLockedNonCancellable rollbackFailedEngineStartLocked(updated.durationMinutes)
            }
        }
        updated
    }

    /**
     * Cycles through the duration presets (Infinite -> 30m -> 60m -> 120m -> Infinite).
     * If currently awake, resets the timer to the newly selected duration; if inactive, enables with that duration.
     */
    suspend fun cycleDurationPresetAndApply(): AwakeState = runLockedNonCancellable {
        val current = repository.stateFlow.first()
        val presets = AwakeState.DURATION_PRESETS
        val idx = presets.indexOf(current.durationMinutes)
        val nextDuration = if (idx == -1 || idx == presets.lastIndex) {
            presets.first()
        } else {
            presets[idx + 1]
        }
        val wasAwake = current.isEffectivelyAwake(clockMillis(), bootCountProvider())
        val targetAwake = if (!wasAwake && nextDuration > 0) true else wasAwake
        applyAwakeTransitionLocked(targetAwake = targetAwake, durationMinutes = nextDuration)
    }

    /**
     * Refreshes engines, taskbar icon, right-click shortcuts, and squircle widget on startup/update.
     */
    suspend fun syncCurrentState(): AwakeState = runLockedNonCancellable {
        val current = repository.stateFlow.first()
        val now = clockMillis()
        val bootCount = bootCountProvider()
        if (current.isAwake && !current.isEffectivelyAwake(now, bootCount)) {
            return@runLockedNonCancellable applyAwakeTransitionLocked(
                targetAwake = false,
                durationMinutes = current.durationMinutes
            )
        }
        iconPort.setCoffeeCupIcon(isFullCup = current.isAwake)
        if (current.isAwake) {
            if (!enginePort.startOrUpdateKeepAwake(current)) {
                return@runLockedNonCancellable rollbackFailedEngineStartLocked(current.durationMinutes)
            }
            scheduleExpirationLocked(current.sessionExpiresAtEpochMillis)
        } else {
            enginePort.stopKeepAwake()
            cancelExpirationLocked()
        }
        shortcutPort.publishShortcuts(current)
        widgetPort.updateWidgets(current)
        current
    }

    /**
     * Explicitly turns off Keep Awake and restores Empty Coffee Cup.
     */
    suspend fun disableKeepAwake(): AwakeState = runLockedNonCancellable {
        val current = repository.stateFlow.first()
        applyAwakeTransitionLocked(targetAwake = false, durationMinutes = current.durationMinutes)
    }

    /**
     * Extends an active timed session by [additionalMinutes] (e.g., +30m from notification)
     * WITHOUT overwriting the user's saved [AwakeState.durationMinutes] preset or shortening
     * an Infinite (`0`) session.
     */
    suspend fun extendSession(additionalMinutes: Int): AwakeState = runLockedNonCancellable {
        val current = repository.stateFlow.first()
        val now = clockMillis()
        val bootCount = bootCountProvider()
        val isEffectivelyAwake = current.isEffectivelyAwake(now, bootCount)

        // Never downgrade an already-Infinite session into a finite timer
        if (isEffectivelyAwake && current.sessionExpiresAtEpochMillis == 0L) {
            return@runLockedNonCancellable current
        }

        val addedMs = additionalMinutes.coerceIn(1, AwakeState.MAX_DURATION_MINUTES) * 60_000L
        val baseMillis = if (isEffectivelyAwake && current.sessionExpiresAtEpochMillis > now) {
            current.sessionExpiresAtEpochMillis
        } else {
            now
        }
        val newExpiresAt = baseMillis + addedMs
        val updated = repository.updateState { state ->
            state.copy(
                isAwake = true,
                sessionExpiresAtEpochMillis = newExpiresAt,
                sessionBootCount = bootCount
            )
        }

        iconPort.setCoffeeCupIcon(isFullCup = true)
        if (!enginePort.startOrUpdateKeepAwake(updated)) {
            return@runLockedNonCancellable rollbackFailedEngineStartLocked(updated.durationMinutes)
        }
        scheduleExpirationLocked(newExpiresAt)
        shortcutPort.publishShortcuts(updated)
        widgetPort.updateWidgets(updated)
        updated
    }

    /**
     * Evaluates low-battery guardrail when battery level or charging state changes.
     */
    suspend fun onBatteryStateChanged(levelPercent: Int, isCharging: Boolean) = runLockedNonCancellable {
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
    suspend fun onChargerStateChanged(isPluggedIn: Boolean) = runLockedNonCancellable {
        val current = repository.stateFlow.first()
        if (current.autoAwakeOnChargerEnabled) {
            applyAwakeTransitionLocked(targetAwake = isPluggedIn, durationMinutes = current.durationMinutes)
        }
    }

    /**
     * Evaluates Lid-Close / Backpack guardrail when the internal laptop display turns off.
     */
    suspend fun onPrimaryDisplayOff(hasActiveExternalDisplay: Boolean) = runLockedNonCancellable {
        val current = repository.stateFlow.first()
        if (current.isAwake && current.lidCloseAutoShutoffEnabled && !hasActiveExternalDisplay) {
            applyAwakeTransitionLocked(targetAwake = false, durationMinutes = current.durationMinutes)
        }
    }

    private suspend fun <T> runLockedNonCancellable(block: suspend () -> T): T {
        val callerJob = kotlin.coroutines.coroutineContext[Job]
        return mutex.withLock {
            withContext(NonCancellable) {
                if (expirationJob != null && expirationJob === callerJob) {
                    expirationJob = null
                }
                block()
            }
        }
    }

    private suspend fun applyAwakeTransitionLocked(
        targetAwake: Boolean,
        durationMinutes: Int
    ): AwakeState {
        cancelExpirationJobOnlyLocked()

        val sanitizedDuration = durationMinutes.coerceIn(0, AwakeState.MAX_DURATION_MINUTES)
        val expiresAt = if (targetAwake && sanitizedDuration > 0) {
            clockMillis() + sanitizedDuration * 60_000L
        } else {
            0L
        }
        val bootCount = if (targetAwake) bootCountProvider() else -1

        val updated = repository.updateState { current ->
            current.copy(
                isAwake = targetAwake,
                durationMinutes = sanitizedDuration,
                sessionExpiresAtEpochMillis = expiresAt,
                sessionBootCount = bootCount
            )
        }

        // 1. Swap Taskbar Coffee Cup Icon FIRST so active alias is enabled before shortcut binding
        iconPort.setCoffeeCupIcon(isFullCup = targetAwake)

        // 2. Start or stop power engines (rolling back UI/DataStore if OS blocks FGS startup)
        if (targetAwake) {
            if (!enginePort.startOrUpdateKeepAwake(updated)) {
                return rollbackFailedEngineStartLocked(sanitizedDuration)
            }
            scheduleExpirationLocked(expiresAt)
        } else {
            alarmPort.cancelExpirationAlarm()
            enginePort.stopKeepAwake()
        }

        // 3. Re-bind right-click dynamic shortcuts to the newly enabled <activity-alias>
        shortcutPort.publishShortcuts(updated)

        // 4. Synchronize Squircle AppWidget and Quick Settings Tile
        widgetPort.updateWidgets(updated)

        return updated
    }

    private suspend fun rollbackFailedEngineStartLocked(durationMinutes: Int): AwakeState {
        cancelExpirationLocked()
        val rolledBack = repository.updateState { current ->
            current.copy(
                isAwake = false,
                durationMinutes = durationMinutes,
                sessionExpiresAtEpochMillis = 0L,
                sessionBootCount = -1
            )
        }
        iconPort.setCoffeeCupIcon(isFullCup = false)
        shortcutPort.publishShortcuts(rolledBack)
        widgetPort.updateWidgets(rolledBack)
        return rolledBack
    }

    private fun cancelExpirationJobOnlyLocked() {
        expirationJob?.cancel()
        expirationJob = null
    }

    private fun cancelExpirationLocked() {
        cancelExpirationJobOnlyLocked()
        alarmPort.cancelExpirationAlarm()
    }

    private fun scheduleExpirationLocked(expiresAtEpochMillis: Long) {
        cancelExpirationJobOnlyLocked()
        if (expiresAtEpochMillis <= 0L) {
            alarmPort.cancelExpirationAlarm()
            return
        }

        alarmPort.scheduleExpirationAlarm(expiresAtEpochMillis)
        val remainingMillis = (expiresAtEpochMillis - clockMillis()).coerceAtLeast(1L)
        expirationJob = externalScope.launch {
            delay(remainingMillis)
            disableKeepAwake()
        }
    }
}
