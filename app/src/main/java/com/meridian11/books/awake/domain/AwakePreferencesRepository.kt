package com.meridian11.books.awake.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private val Context.awakeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "awake_preferences"
)

interface AwakePreferencesRepository {
    val stateFlow: Flow<AwakeState>

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

    override suspend fun setAwakeState(isAwake: Boolean, expiresAtEpochMillis: Long) {
        _stateFlow.update {
            it.copy(
                isAwake = isAwake,
                sessionExpiresAtEpochMillis = if (isAwake) expiresAtEpochMillis else 0L
            )
        }
    }

    override suspend fun setDurationMinutes(durationMinutes: Int) {
        _stateFlow.update { it.copy(durationMinutes = durationMinutes.coerceAtLeast(0)) }
    }

    override suspend fun cycleDurationPreset(): Int {
        var nextPreset = 0
        _stateFlow.update { current ->
            val presets = AwakeState.DURATION_PRESETS
            val idx = presets.indexOf(current.durationMinutes)
            nextPreset = if (idx == -1 || idx == presets.lastIndex) {
                presets.first()
            } else {
                presets[idx + 1]
            }
            current.copy(durationMinutes = nextPreset)
        }
        return nextPreset
    }

    override suspend fun updateGuardrails(
        lowBatteryCutoffEnabled: Boolean,
        autoAwakeOnChargerEnabled: Boolean,
        lidCloseAutoShutoffEnabled: Boolean,
        allowScreenDimming: Boolean
    ) {
        _stateFlow.update {
            it.copy(
                lowBatteryCutoffEnabled = lowBatteryCutoffEnabled,
                autoAwakeOnChargerEnabled = autoAwakeOnChargerEnabled,
                lidCloseAutoShutoffEnabled = lidCloseAutoShutoffEnabled,
                allowScreenDimming = allowScreenDimming
            )
        }
    }

    override suspend fun setDirectToggleOnTap(enabled: Boolean) {
        _stateFlow.update { it.copy(directToggleOnTap = enabled) }
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
    }

    override val stateFlow: Flow<AwakeState> = dataStore.data.map { prefs ->
        AwakeState(
            isAwake = prefs[Keys.IS_AWAKE] ?: false,
            durationMinutes = prefs[Keys.DURATION_MINUTES] ?: 0,
            sessionExpiresAtEpochMillis = prefs[Keys.EXPIRES_AT_MILLIS] ?: 0L,
            lowBatteryCutoffEnabled = prefs[Keys.LOW_BATTERY_CUTOFF] ?: true,
            lowBatteryThresholdPercent = prefs[Keys.LOW_BATTERY_THRESHOLD] ?: 20,
            autoAwakeOnChargerEnabled = prefs[Keys.AUTO_AWAKE_CHARGER] ?: false,
            lidCloseAutoShutoffEnabled = prefs[Keys.LID_CLOSE_SHUTOFF] ?: true,
            allowScreenDimming = prefs[Keys.ALLOW_SCREEN_DIMMING] ?: false,
            directToggleOnTap = prefs[Keys.DIRECT_TOGGLE_ON_TAP] ?: false
        )
    }

    override suspend fun setAwakeState(isAwake: Boolean, expiresAtEpochMillis: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_AWAKE] = isAwake
            prefs[Keys.EXPIRES_AT_MILLIS] = if (isAwake) expiresAtEpochMillis else 0L
        }
    }

    override suspend fun setDurationMinutes(durationMinutes: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.DURATION_MINUTES] = durationMinutes.coerceAtLeast(0)
        }
    }

    override suspend fun cycleDurationPreset(): Int {
        val current = stateFlow.first().durationMinutes
        val presets = AwakeState.DURATION_PRESETS
        val idx = presets.indexOf(current)
        val nextPreset = if (idx == -1 || idx == presets.lastIndex) {
            presets.first()
        } else {
            presets[idx + 1]
        }
        dataStore.edit { prefs ->
            prefs[Keys.DURATION_MINUTES] = nextPreset
        }
        return nextPreset
    }

    override suspend fun updateGuardrails(
        lowBatteryCutoffEnabled: Boolean,
        autoAwakeOnChargerEnabled: Boolean,
        lidCloseAutoShutoffEnabled: Boolean,
        allowScreenDimming: Boolean
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.LOW_BATTERY_CUTOFF] = lowBatteryCutoffEnabled
            prefs[Keys.AUTO_AWAKE_CHARGER] = autoAwakeOnChargerEnabled
            prefs[Keys.LID_CLOSE_SHUTOFF] = lidCloseAutoShutoffEnabled
            prefs[Keys.ALLOW_SCREEN_DIMMING] = allowScreenDimming
        }
    }

    override suspend fun setDirectToggleOnTap(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DIRECT_TOGGLE_ON_TAP] = enabled
        }
    }
}
