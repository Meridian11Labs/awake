package com.meridian11.books.awake.domain

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreAwakePreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `keys written by earlier builds are ignored`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
                File(tempFolder.root, "awake_preferences.preferences_pb")
            }
            dataStore.edit { prefs ->
                prefs[booleanPreferencesKey("tap_sim_enabled")] = true
                prefs[intPreferencesKey("tap_interval_seconds")] = 30
                prefs[booleanPreferencesKey("accessibility_disclosure_accepted")] = true
                prefs[booleanPreferencesKey("is_awake")] = true
                prefs[intPreferencesKey("duration_minutes")] = 60
            }

            val repository = DataStoreAwakePreferencesRepository(dataStore)
            assertEquals(AwakeState(isAwake = true, durationMinutes = 60), repository.stateFlow.first())

            repository.setDirectToggleOnTap(true)
            assertTrue(repository.stateFlow.first().directToggleOnTap)
        } finally {
            scope.cancel()
        }
    }
}
