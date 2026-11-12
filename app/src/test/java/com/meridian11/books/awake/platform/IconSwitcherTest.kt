package com.meridian11.books.awake.platform

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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
class IconSwitcherTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private val recordedTransitions = mutableListOf<Pair<ComponentName, Int>>()
    private lateinit var iconSwitcher: AndroidIconSwitcher

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        packageManager = context.packageManager
        recordedTransitions.clear()
        iconSwitcher = AndroidIconSwitcher(
            context = context,
            onComponentStateChange = { component, newState ->
                recordedTransitions.add(component to newState)
            }
        )
    }

    @Test
    fun `setCoffeeCupIcon true enables FullCupAlias BEFORE disabling EmptyCupAlias`() {
        iconSwitcher.setCoffeeCupIcon(isFullCup = true)

        val fullAlias = AndroidIconSwitcher.fullCupComponent(context)
        val emptyAlias = AndroidIconSwitcher.emptyCupComponent(context)

        assertEquals(2, recordedTransitions.size)
        // Rule 1: Enable incoming alias FIRST
        assertEquals(fullAlias, recordedTransitions[0].first)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, recordedTransitions[0].second)
        // Rule 2: Disable outgoing alias SECOND
        assertEquals(emptyAlias, recordedTransitions[1].first)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, recordedTransitions[1].second)

        assertTrue(iconSwitcher.isFullCupAliasEnabled())
        assertEquals(fullAlias, iconSwitcher.getActiveAliasComponent())

        // Duplicate call with identical state must be a zero-IPC no-op
        recordedTransitions.clear()
        iconSwitcher.setCoffeeCupIcon(isFullCup = true)
        assertEquals("Idempotent repeat call must not re-apply PackageManager component writes", 0, recordedTransitions.size)
    }

    @Test
    fun `setCoffeeCupIcon false enables EmptyCupAlias BEFORE disabling FullCupAlias`() {
        iconSwitcher.setCoffeeCupIcon(isFullCup = true)
        recordedTransitions.clear()

        iconSwitcher.setCoffeeCupIcon(isFullCup = false)

        val fullAlias = AndroidIconSwitcher.fullCupComponent(context)
        val emptyAlias = AndroidIconSwitcher.emptyCupComponent(context)

        assertEquals(2, recordedTransitions.size)
        // Enable EmptyCupAlias FIRST
        assertEquals(emptyAlias, recordedTransitions[0].first)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, recordedTransitions[0].second)
        // Disable FullCupAlias SECOND
        assertEquals(fullAlias, recordedTransitions[1].first)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, recordedTransitions[1].second)

        assertFalse(iconSwitcher.isFullCupAliasEnabled())
        assertEquals(emptyAlias, iconSwitcher.getActiveAliasComponent())
    }

    @Test
    fun `self-healing repairs corrupted state when both aliases were disabled`() {
        val fullAlias = AndroidIconSwitcher.fullCupComponent(context)
        val emptyAlias = AndroidIconSwitcher.emptyCupComponent(context)
        packageManager.setComponentEnabledSetting(
            fullAlias,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(
            emptyAlias,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        iconSwitcher.setCoffeeCupIcon(isFullCup = false)

        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            packageManager.getComponentEnabledSetting(emptyAlias)
        )
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            packageManager.getComponentEnabledSetting(fullAlias)
        )
    }
}
