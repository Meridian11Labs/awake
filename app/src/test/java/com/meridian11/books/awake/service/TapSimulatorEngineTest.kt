package com.meridian11.books.awake.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TapSimulatorEngineTest {

    data class RecordedPulse(val x: Float, val y: Float, val durationMs: Long)

    @Test
    fun `start dispatches 1ms stationary micro-tap at bottom-right safe pixel every intervalSeconds`() =
        runTest(StandardTestDispatcher()) {
            val pulses = mutableListOf<RecordedPulse>()
            val engine = TapSimulatorEngine(
                scope = backgroundScope,
                displayBoundsProvider = { 2560 to 1600 },
                gestureDispatcher = { x, y, durationMs ->
                    pulses.add(RecordedPulse(x, y, durationMs))
                }
            )

            engine.start(intervalSeconds = 30)
            assertTrue(engine.isRunning())

            // Before first interval elapses: 0 pulses
            advanceTimeBy(29_000L)
            runCurrent()
            assertEquals(0, pulses.size)

            // At 30s: 1st pulse at safe bottom-right corner (2559, 1599) for 1ms
            advanceTimeBy(1_000L)
            runCurrent()
            assertEquals(1, pulses.size)
            assertEquals(RecordedPulse(2559f, 1599f, 1L), pulses[0])

            // Advance 60s more (to 90s total): 3 pulses total
            advanceTimeBy(60_000L)
            runCurrent()
            assertEquals(3, pulses.size)
        }

    @Test
    fun `stop cancels periodic ticker immediately and dispatches no further taps`() =
        runTest(StandardTestDispatcher()) {
            val pulses = mutableListOf<RecordedPulse>()
            val engine = TapSimulatorEngine(
                scope = backgroundScope,
                displayBoundsProvider = { 1920 to 1080 },
                gestureDispatcher = { x, y, durationMs ->
                    pulses.add(RecordedPulse(x, y, durationMs))
                }
            )

            engine.start(intervalSeconds = 15)
            advanceTimeBy(15_000L)
            runCurrent()
            assertEquals(1, pulses.size)

            engine.stop()
            assertFalse(engine.isRunning())

            advanceTimeBy(120_000L)
            runCurrent()
            assertEquals("Should not dispatch any taps after stop()", 1, pulses.size)
        }
}
