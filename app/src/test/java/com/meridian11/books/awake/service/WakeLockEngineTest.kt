package com.meridian11.books.awake.service

import android.content.Context
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WakeLockEngineTest {

    private lateinit var context: Context
    private lateinit var wakeLockEngine: WakeLockEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        wakeLockEngine = WakeLockEngine(context)
    }

    @Test
    fun `acquire holds both CPU partial WakeLock and screen bright WakeLock when allowScreenDimming is false`() {
        assertFalse(wakeLockEngine.isHeld())

        wakeLockEngine.acquire(allowScreenDimming = false)

        assertTrue(wakeLockEngine.isHeld())
        @Suppress("DEPRECATION")
        val expectedScreenLevel = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE
        assertEquals(expectedScreenLevel, wakeLockEngine.lastAcquiredScreenWakeLockFlags)
    }

    @Test
    fun `acquire uses SCREEN_DIM_WAKE_LOCK when allowScreenDimming is true`() {
        wakeLockEngine.acquire(allowScreenDimming = true)

        assertTrue(wakeLockEngine.isHeld())
        @Suppress("DEPRECATION")
        val expectedScreenLevel = PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE
        assertEquals(expectedScreenLevel, wakeLockEngine.lastAcquiredScreenWakeLockFlags)
    }

    @Test
    fun `release cleanly releases all held WakeLocks and is idempotent`() {
        wakeLockEngine.acquire(allowScreenDimming = false)
        assertTrue(wakeLockEngine.isHeld())

        wakeLockEngine.release()
        assertFalse(wakeLockEngine.isHeld())

        // Second release call must be safe and idempotent
        wakeLockEngine.release()
        assertFalse(wakeLockEngine.isHeld())
    }
}
