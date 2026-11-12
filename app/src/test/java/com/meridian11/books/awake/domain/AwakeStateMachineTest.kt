package com.meridian11.books.awake.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AwakeStateMachineTest {

    private lateinit var repository: InMemoryAwakePreferencesRepository
    private lateinit var fakeIconPort: FakeIconSwitcherPort
    private lateinit var fakeShortcutPort: FakeShortcutPublisherPort
    private lateinit var fakeEnginePort: FakeKeepAwakeEnginePort
    private lateinit var fakeWidgetPort: FakeWidgetUpdaterPort
    private lateinit var fakeAlarmPort: FakeExpirationAlarmPort
    private var fakeClockMillis: Long = 1_000_000L
    private var fakeBootCount: Int = 7

    @Before
    fun setUp() {
        repository = InMemoryAwakePreferencesRepository()
        fakeIconPort = FakeIconSwitcherPort()
        fakeShortcutPort = FakeShortcutPublisherPort()
        fakeEnginePort = FakeKeepAwakeEnginePort()
        fakeWidgetPort = FakeWidgetUpdaterPort()
        fakeAlarmPort = FakeExpirationAlarmPort()
        fakeClockMillis = 1_000_000L
        fakeBootCount = 7
    }

    private fun createController(scope: TestScope): AwakeController {
        return AwakeController(
            repository = repository,
            iconPort = fakeIconPort,
            shortcutPort = fakeShortcutPort,
            enginePort = fakeEnginePort,
            externalScope = scope.backgroundScope,
            widgetPort = fakeWidgetPort,
            alarmPort = fakeAlarmPort,
            clockMillis = { fakeClockMillis },
            bootCountProvider = { fakeBootCount }
        )
    }

    @Test
    fun `toggle from INACTIVE switches to ACTIVE Full Cup, starts engine, rebinds shortcuts, and updates squircle widget`() = runTest {
        val controller = createController(this)

        val newState = controller.toggle()

        assertTrue("State should be awake after first toggle", newState.isAwake)
        assertEquals("Taskbar icon must be Full Coffee Cup", true, fakeIconPort.lastIsFullCup)
        assertTrue("Engine should be running", fakeEnginePort.isRunning)
        assertEquals("Shortcuts must be bound to Full Cup state", true, fakeShortcutPort.lastState?.isAwake)
        assertEquals("Squircle widget must be updated to Full Cup state", true, fakeWidgetPort.lastState?.isAwake)
    }

    @Test
    fun `toggle from ACTIVE switches back to INACTIVE Empty Cup, stops engine, rebinds shortcuts, and updates squircle widget`() = runTest {
        val controller = createController(this)
        controller.toggle() // OFF -> ON
        assertTrue(fakeEnginePort.isRunning)

        val newState = controller.toggle() // ON -> OFF

        assertFalse("State should be inactive after second toggle", newState.isAwake)
        assertEquals("Taskbar icon must be Empty Coffee Cup", false, fakeIconPort.lastIsFullCup)
        assertFalse("Engine must be stopped", fakeEnginePort.isRunning)
        assertEquals("Shortcuts must be bound to Empty Cup state", false, fakeShortcutPort.lastState?.isAwake)
        assertEquals("Squircle widget must be updated to Half-Empty Cup state", false, fakeWidgetPort.lastState?.isAwake)
    }

    @Test
    fun `concurrent rapid toggles are Mutex-serialized and leave ports in consistent state`() = runTest {
        val controller = createController(this)

        // Fire 9 concurrent toggles from false -> should deterministically end at true (odd count)
        val deferreds = (1..9).map {
            async { controller.toggle() }
        }
        deferreds.awaitAll()

        val finalState = repository.stateFlow.first()
        assertTrue(finalState.isAwake)
        assertEquals(finalState.isAwake, fakeIconPort.lastIsFullCup)
        assertEquals(finalState.isAwake, fakeEnginePort.isRunning)
        assertEquals(finalState.isAwake, fakeShortcutPort.lastState?.isAwake)
        assertEquals(9, fakeIconPort.swapHistory.size)
    }

    @Test
    fun `enabling with finite duration sets expiration, schedules alarm, and auto-shuts off when timer elapses`() =
        runTest(StandardTestDispatcher()) {
            val controller = createController(this)

            controller.enableWithDuration(durationMinutes = 30)
            runCurrent()

            val activeState = repository.stateFlow.first()
            val expectedDeadline = fakeClockMillis + 30 * 60_000L
            assertTrue(activeState.isAwake)
            assertEquals(30, activeState.durationMinutes)
            assertEquals(expectedDeadline, activeState.sessionExpiresAtEpochMillis)
            assertEquals(expectedDeadline, fakeAlarmPort.scheduledAtElapsedMillis)
            assertEquals(true, fakeIconPort.lastIsFullCup)
            assertTrue(fakeEnginePort.isRunning)

            // Advance time by 29 minutes: should still be awake
            fakeClockMillis += 29 * 60_000L
            advanceTimeBy(29 * 60_000L)
            runCurrent()
            assertTrue(repository.stateFlow.first().isAwake)

            // Advance past the 30th minute: should auto-shutoff to Empty Cup and cancel alarm
            fakeClockMillis += 2 * 60_000L
            advanceTimeBy(2 * 60_000L)
            runCurrent()

            val expiredState = repository.stateFlow.first()
            assertFalse("Should automatically revert to Empty Cup after 30m", expiredState.isAwake)
            assertEquals(false, fakeIconPort.lastIsFullCup)
            assertFalse(fakeEnginePort.isRunning)
            assertEquals(null, fakeAlarmPort.scheduledAtElapsedMillis)
        }

    @Test
    fun `when enginePort blocks foreground service start, controller rolls back DataStore, icon, shortcuts, and widget to OFF`() =
        runTest {
            fakeEnginePort.allowStart = false
            val controller = createController(this)

            val result = controller.enableWithDuration(durationMinutes = 60)

            assertFalse("Blocked FGS start must never leave Phantom Awake state", result.isAwake)
            assertEquals("User preset duration must still be preserved", 60, result.durationMinutes)
            assertFalse(repository.stateFlow.first().isAwake)
            assertEquals(false, fakeIconPort.lastIsFullCup)
            assertEquals(false, fakeShortcutPort.lastState?.isAwake)
            assertEquals(false, fakeWidgetPort.lastState?.isAwake)
            assertEquals(null, fakeAlarmPort.scheduledAtElapsedMillis)
        }

    @Test
    fun `syncCurrentState invalidates stale awake state when bootCount changed across reboot`() = runTest {
        val controller = createController(this)
        controller.toggle() // ON during bootCount = 7
        assertTrue(repository.stateFlow.first().isAwake)

        // Simulate reboot to bootCount = 8 before BOOT_COMPLETED ran
        fakeBootCount = 8
        val synced = controller.syncCurrentState()

        assertFalse("Cross-reboot stale session must reset to OFF", synced.isAwake)
        assertEquals(false, fakeIconPort.lastIsFullCup)
        assertFalse(fakeEnginePort.isRunning)
    }

    @Test
    fun `cancelling caller coroutine mid-transition completes state update without tearing ports`() =
        runTest(StandardTestDispatcher()) {
            val controller = createController(this)
            val callerJob = launch {
                controller.toggle()
                throw CancellationException("Simulated Activity destroy")
            }
            runCurrent()
            callerJob.cancel()
            runCurrent()

            val state = repository.stateFlow.first()
            assertTrue(state.isAwake)
            assertEquals(true, fakeIconPort.lastIsFullCup)
            assertTrue(fakeEnginePort.isRunning)
            assertEquals(true, fakeWidgetPort.lastState?.isAwake)
        }

    @Test
    fun `onBatteryStateChanged shuts off Keep Awake when unplugged and battery at or below threshold`() = runTest {
        val controller = createController(this)
        controller.toggle() // ON
        assertTrue(repository.stateFlow.first().isAwake)

        // Battery at 18% while charging -> should stay awake
        controller.onBatteryStateChanged(levelPercent = 18, isCharging = true)
        assertTrue("Should remain awake when charging even if battery < 20%", repository.stateFlow.first().isAwake)

        // Battery at 18% while unplugged -> should shut off to Empty Cup
        controller.onBatteryStateChanged(levelPercent = 18, isCharging = false)
        assertFalse("Should shut off to Empty Cup when unplugged at 18%", repository.stateFlow.first().isAwake)
        assertEquals(false, fakeIconPort.lastIsFullCup)
        assertFalse(fakeEnginePort.isRunning)
    }

    @Test
    fun `onChargerStateChanged auto-enables Full Cup on plug-in and disables on unplug when enabled`() = runTest {
        val controller = createController(this)
        controller.updateGuardrails(
            lowBatteryCutoffEnabled = true,
            autoAwakeOnChargerEnabled = true,
            lidCloseAutoShutoffEnabled = true,
            allowScreenDimming = false
        )

        // Plug in AC charger -> auto-enable Full Cup
        controller.onChargerStateChanged(isPluggedIn = true)
        assertTrue(repository.stateFlow.first().isAwake)
        assertEquals(true, fakeIconPort.lastIsFullCup)
        assertTrue(fakeEnginePort.isRunning)

        // Unplug AC charger -> auto-revert to Empty Cup
        controller.onChargerStateChanged(isPluggedIn = false)
        assertFalse(repository.stateFlow.first().isAwake)
        assertEquals(false, fakeIconPort.lastIsFullCup)
        assertFalse(fakeEnginePort.isRunning)
    }

    @Test
    fun `onPrimaryDisplayOff shuts off Keep Awake when lidCloseAutoShutoffEnabled is true`() = runTest {
        val controller = createController(this)
        controller.toggle() // ON
        assertTrue(repository.stateFlow.first().isAwake)

        controller.onPrimaryDisplayOff(hasActiveExternalDisplay = false)

        assertFalse("Should shut off when lid closes with no external monitor", repository.stateFlow.first().isAwake)
        assertEquals(false, fakeIconPort.lastIsFullCup)
        assertFalse(fakeEnginePort.isRunning)
    }

    @Test
    fun `onPrimaryDisplayOff stays awake if docked to an active external monitor`() = runTest {
        val controller = createController(this)
        controller.toggle() // ON

        controller.onPrimaryDisplayOff(hasActiveExternalDisplay = true)

        assertTrue("Should stay awake in clamshell docked mode with external display", repository.stateFlow.first().isAwake)
        assertEquals(true, fakeIconPort.lastIsFullCup)
    }

    @Test
    fun `extendSession keeps Infinite mode Infinite and extends finite sessions without overwriting saved durationMinutes preset`() =
        runTest {
            val controller = createController(this)

            // 1. Infinite mode (durationMinutes = 0): +30m must NOT downgrade Infinite into a 30m timer
            controller.toggle()
            val infiniteExtended = controller.extendSession(additionalMinutes = 30)
            assertTrue(infiniteExtended.isAwake)
            assertEquals(0, infiniteExtended.durationMinutes)
            assertEquals(0L, infiniteExtended.sessionExpiresAtEpochMillis)

            // 2. Finite 60m session: +30m extends deadline to +90m while preserving 60m preset
            controller.enableWithDuration(durationMinutes = 60)
            val extended = controller.extendSession(additionalMinutes = 30)
            assertTrue(extended.isAwake)
            assertEquals("Saved 60m preset must not be overwritten by +30m extension", 60, extended.durationMinutes)
            assertEquals(fakeClockMillis + 90 * 60_000L, extended.sessionExpiresAtEpochMillis)
        }
}

class FakeIconSwitcherPort : IconSwitcherPort {
    var lastIsFullCup: Boolean? = null
    val swapHistory = mutableListOf<Boolean>()

    override fun setCoffeeCupIcon(isFullCup: Boolean) {
        lastIsFullCup = isFullCup
        swapHistory.add(isFullCup)
    }

    override fun isFullCupAliasEnabled(): Boolean = lastIsFullCup == true
}

class FakeShortcutPublisherPort : ShortcutPublisherPort {
    var lastState: AwakeState? = null

    override fun publishShortcuts(state: AwakeState) {
        lastState = state
    }
}

class FakeKeepAwakeEnginePort : KeepAwakeEnginePort {
    var allowStart: Boolean = true
    var isRunning: Boolean = false
    var lastState: AwakeState? = null

    override fun startOrUpdateKeepAwake(state: AwakeState): Boolean {
        if (!allowStart) {
            isRunning = false
            return false
        }
        isRunning = true
        lastState = state
        return true
    }

    override fun stopKeepAwake() {
        isRunning = false
    }
}

class FakeWidgetUpdaterPort : WidgetUpdaterPort {
    var lastState: AwakeState? = null

    override fun updateWidgets(state: AwakeState) {
        lastState = state
    }
}

class FakeExpirationAlarmPort : ExpirationAlarmPort {
    var scheduledAtElapsedMillis: Long? = null

    override fun scheduleExpirationAlarm(triggerAtElapsedMillis: Long) {
        scheduledAtElapsedMillis = triggerAtElapsedMillis
    }

    override fun cancelExpirationAlarm() {
        scheduledAtElapsedMillis = null
    }
}
