package com.meridian11.books.awake.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet

private val Context.awakeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "awake_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

interface AwakePreferencesRepository {
    val stateFlow: Flow<AwakeState>

    suspend fun updateState(transform: (AwakeState) -> AwakeState): AwakeState
    suspend fun setAwakeState(isAwake: Boolean, expiresAtEpochMillis: Long)
    suspend fun setDurationMinutes(durationMinutes: Int)
    suspend fun cycleDurationPreset(): Int
    suspend fun updateGuardrails(
        lowBatteryCutoffEnabled: Boolean,
        autoAwakeOnChargerEnabled: Boolean,
        lidCloseAutoShutoffEnabled: Boolean,
        allowScreenDimming: Boolean
    )
    suspend fun setDirectToggleOnTap(enabled: Boolean)
}

class InMemoryAwakePreferencesRepository(
    initialState: AwakeState = AwakeState()
) : AwakePreferencesRepository {

    private val _stateFlow = MutableStateFlow(initialState)
    override val stateFlow: Flow<AwakeState> = _stateFlow.asStateFlow()

    override suspend fun updateState(transform: (AwakeState) -> AwakeState): AwakeState {
        return _stateFlow.updateAndGet(transform)
    }

    override suspend fun setAwakeState(isAwake: Boolean, expiresAtEpochMillis: Long) {
        updateState {
            it.copy(
                isAwake = isAwake,
                sessionExpiresAtEpochMillis = if (isAwake) expiresAtEpochMillis else 0L,
                sessionBootCount = if (isAwake) it.sessionBootCount else -1
            )
        }
    }

    override suspend fun setDurationMinutes(durationMinutes: Int) {
        updateState {
            it.copy(durationMinutes = durationMinutes.coerceIn(0, AwakeState.MAX_DURATION_MINUTES))
        }
    }

    override suspend fun cycleDurationPreset(): Int {
        val updated = updateState { current ->
            val presets = AwakeState.DURATION_PRESETS
            val idx = presets.indexOf(current.durationMinutes)
            val nextPreset = if (idx == -1 || idx == presets.lastIndex) {
                presets.first()
            } else {
                presets[idx + 1]
            }
            current.copy(durationMinutes = nextPreset)
        }
        return updated.durationMinutes
    }

    override suspend fun updateGuardrails(
        lowBatteryCutoffEnabled: Boolean,
        autoAwakeOnChargerEnabled: Boolean,
        lidCloseAutoShutoffEnabled: Boolean,
        allowScreenDimming: Boolean
    ) {
        updateState {
            it.copy(
                lowBatteryCutoffEnabled = lowBatteryCutoffEnabled,
                autoAwakeOnChargerEnabled = autoAwakeOnChargerEnabled,
                lidCloseAutoShutoffEnabled = lidCloseAutoShutoffEnabled,
                allowScreenDimming = allowScreenDimming
            )
        }
    }

    override suspend fun setDirectToggleOnTap(enabled: Boolean) {
        updateState { it.copy(directToggleOnTap = enabled) }
    }
}

class DataStoreAwakePreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : AwakePreferencesRepository {

    constructor(context: Context) : this(context.applicationContext.awakeDataStore)

    // Keys written by earlier builds and no longer listed here may still be on a device;
    // DataStore keeps them and they are simply never read.
    private object Keys {
        val IS_AWAKE = booleanPreferencesKey("is_awake")
        val DURATION_MINUTES = intPreferencesKey("duration_minutes")
        val EXPIRES_AT_MILLIS = longPreferencesKey("expires_at_millis")
        val LOW_BATTERY_CUTOFF = booleanPreferencesKey("low_battery_cutoff")
        val LOW_BATTERY_THRESHOLD = intPreferencesKey("low_battery_threshold")
        val AUTO_AWAKE_CHARGER = booleanPreferencesKey("auto_awake_charger")
        val LID_CLOSE_SHUTOFF = booleanPreferencesKey("lid_close_shutoff")
        val ALLOW_SCREEN_DIMMING = booleanPreferencesKey("allow_screen_dimming")
        val DIRECT_TOGGLE_ON_TAP = booleanPreferencesKey("direct_toggle_on_tap")
        val SESSION_BOOT_COUNT = intPreferencesKey("session_boot_count")
    }

    override val stateFlow: Flow<AwakeState> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs -> prefs.toAwakeState() }

    override suspend fun updateState(transform: (AwakeState) -> AwakeState): AwakeState {
        var result = AwakeState()
        try {
            dataStore.edit { prefs ->
                val current = prefs.toAwakeState()
                val updated = transform(current)
                prefs.writeAwakeState(updated)
                result = updated
            }
        } catch (_: IOException) {
        }
        return result
    }

    override suspend fun setAwakeState(isAwake: Boolean, expiresAtEpochMillis: Long) {
        updateState { current ->
            current.copy(
                isAwake = isAwake,
                sessionExpiresAtEpochMillis = if (isAwake) expiresAtEpochMillis else 0L,
                sessionBootCount = if (isAwake) current.sessionBootCount else -1
            )
        }
    }

    override suspend fun setDurationMinutes(durationMinutes: Int) {
        updateState { current ->
            current.copy(durationMinutes = durationMinutes.coerceIn(0, AwakeState.MAX_DURATION_MINUTES))
        }
    }

    override suspend fun cycleDurationPreset(): Int {
        val updated = updateState { current ->
            val presets = AwakeState.DURATION_PRESETS
            val idx = presets.indexOf(current.durationMinutes)
            val nextPreset = if (idx == -1 || idx == presets.lastIndex) {
                presets.first()
            } else {
                presets[idx + 1]
            }
            current.copy(durationMinutes = nextPreset)
        }
        return updated.durationMinutes
    }

    override suspend fun updateGuardrails(
        lowBatteryCutoffEnabled: Boolean,
        autoAwakeOnChargerEnabled: Boolean,
        lidCloseAutoShutoffEnabled: Boolean,
        allowScreenDimming: Boolean
    ) {
        updateState { current ->
            current.copy(
                lowBatteryCutoffEnabled = lowBatteryCutoffEnabled,
                autoAwakeOnChargerEnabled = autoAwakeOnChargerEnabled,
                lidCloseAutoShutoffEnabled = lidCloseAutoShutoffEnabled,
                allowScreenDimming = allowScreenDimming
            )
        }
    }

    override suspend fun setDirectToggleOnTap(enabled: Boolean) {
        updateState { current -> current.copy(directToggleOnTap = enabled) }
    }

    private fun Preferences.toAwakeState(): AwakeState = AwakeState(
        isAwake = this[Keys.IS_AWAKE] ?: false,
        durationMinutes = (this[Keys.DURATION_MINUTES] ?: 0).coerceIn(0, AwakeState.MAX_DURATION_MINUTES),
        sessionExpiresAtEpochMillis = this[Keys.EXPIRES_AT_MILLIS] ?: 0L,
        lowBatteryCutoffEnabled = this[Keys.LOW_BATTERY_CUTOFF] ?: true,
        lowBatteryThresholdPercent = this[Keys.LOW_BATTERY_THRESHOLD] ?: 20,
        autoAwakeOnChargerEnabled = this[Keys.AUTO_AWAKE_CHARGER] ?: false,
        lidCloseAutoShutoffEnabled = this[Keys.LID_CLOSE_SHUTOFF] ?: true,
        allowScreenDimming = this[Keys.ALLOW_SCREEN_DIMMING] ?: false,
        directToggleOnTap = this[Keys.DIRECT_TOGGLE_ON_TAP] ?: false,
        sessionBootCount = this[Keys.SESSION_BOOT_COUNT] ?: -1
    )

    private fun MutablePreferences.writeAwakeState(state: AwakeState) {
        this[Keys.IS_AWAKE] = state.isAwake
        this[Keys.DURATION_MINUTES] = state.durationMinutes.coerceIn(0, AwakeState.MAX_DURATION_MINUTES)
        this[Keys.EXPIRES_AT_MILLIS] = if (state.isAwake) state.sessionExpiresAtEpochMillis else 0L
        this[Keys.LOW_BATTERY_CUTOFF] = state.lowBatteryCutoffEnabled
        this[Keys.LOW_BATTERY_THRESHOLD] = state.lowBatteryThresholdPercent
        this[Keys.AUTO_AWAKE_CHARGER] = state.autoAwakeOnChargerEnabled
        this[Keys.LID_CLOSE_SHUTOFF] = state.lidCloseAutoShutoffEnabled
        this[Keys.ALLOW_SCREEN_DIMMING] = state.allowScreenDimming
        this[Keys.DIRECT_TOGGLE_ON_TAP] = state.directToggleOnTap
        this[Keys.SESSION_BOOT_COUNT] = if (state.isAwake) state.sessionBootCount else -1
    }
}
