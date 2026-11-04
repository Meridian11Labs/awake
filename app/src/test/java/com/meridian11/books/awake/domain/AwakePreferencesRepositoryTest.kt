package com.meridian11.books.awake.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AwakePreferencesRepositoryTest {

    private lateinit var repository: InMemoryAwakePreferencesRepository

    @Before
    fun setUp() {
        repository = InMemoryAwakePreferencesRepository()
    }

    @Test
    fun `initial state has expected safe Googlebook defaults`() = runTest {
        val state = repository.stateFlow.first()

        assertFalse("Should start inactive (Empty Cup)", state.isAwake)
        assertEquals("Default duration should be 0 (Indefinite)", 0, state.durationMinutes)
        assertEquals(0L, state.sessionExpiresAtEpochMillis)
        assertTrue("Low battery cutoff should default to true", state.lowBatteryCutoffEnabled)
        assertEquals(20, state.lowBatteryThresholdPercent)
        assertFalse("Auto awake on charger should default to false", state.autoAwakeOnChargerEnabled)
        assertTrue("Lid-close backpack safety should default to true", state.lidCloseAutoShutoffEnabled)
        assertFalse("Allow screen dimming should default to false", state.allowScreenDimming)
        assertFalse("directToggleOnTap should default to false (open window on tap)", state.directToggleOnTap)
    }

    @Test
    fun `setDirectToggleOnTap persists hide settings tap-to-toggle preference`() = runTest {
        assertFalse(repository.stateFlow.first().directToggleOnTap)

        repository.setDirectToggleOnTap(true)
        assertTrue(repository.stateFlow.first().directToggleOnTap)

        repository.setDirectToggleOnTap(false)
        assertFalse(repository.stateFlow.first().directToggleOnTap)
    }

    @Test
    fun `updating awake state persists isAwake and expiration timestamp`() = runTest {
        repository.setAwakeState(isAwake = true, expiresAtEpochMillis = 123456789L)
        val state = repository.stateFlow.first()

        assertTrue(state.isAwake)
        assertEquals(123456789L, state.sessionExpiresAtEpochMillis)

        repository.setAwakeState(isAwake = false, expiresAtEpochMillis = 0L)
        val updated = repository.stateFlow.first()
        assertFalse(updated.isAwake)
        assertEquals(0L, updated.sessionExpiresAtEpochMillis)
    }

    @Test
    fun `cycleDurationPreset cycles through 0, 30, 60, 120 and back to 0`() = runTest {
        assertEquals(0, repository.stateFlow.first().durationMinutes)

        assertEquals(30, repository.cycleDurationPreset())
        assertEquals(30, repository.stateFlow.first().durationMinutes)

        assertEquals(60, repository.cycleDurationPreset())
        assertEquals(60, repository.stateFlow.first().durationMinutes)

        assertEquals(120, repository.cycleDurationPreset())
        assertEquals(120, repository.stateFlow.first().durationMinutes)

        assertEquals(0, repository.cycleDurationPreset())
        assertEquals(0, repository.stateFlow.first().durationMinutes)
    }

    @Test
    fun `guardrail settings update independently`() = runTest {
        repository.updateGuardrails(
            lowBatteryCutoffEnabled = false,
            autoAwakeOnChargerEnabled = true,
            lidCloseAutoShutoffEnabled = false,
            allowScreenDimming = true
        )
        val state = repository.stateFlow.first()
        assertFalse(state.lowBatteryCutoffEnabled)
        assertTrue(state.autoAwakeOnChargerEnabled)
        assertFalse(state.lidCloseAutoShutoffEnabled)
        assertTrue(state.allowScreenDimming)
    }
}
