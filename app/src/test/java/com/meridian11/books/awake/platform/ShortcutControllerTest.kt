package com.meridian11.books.awake.platform

import android.content.Context
import android.content.pm.ShortcutManager
import androidx.test.core.app.ApplicationProvider
import com.meridian11.books.awake.domain.AwakeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShortcutControllerTest {

    private lateinit var context: Context
    private lateinit var iconSwitcher: AndroidIconSwitcher
    private lateinit var shortcutController: AndroidShortcutPublisher
    private lateinit var shortcutManager: ShortcutManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        iconSwitcher = AndroidIconSwitcher(context)
        shortcutController = AndroidShortcutPublisher(context, iconSwitcher)
        shortcutManager = context.getSystemService(ShortcutManager::class.java)
    }

    @Test
    fun `publishShortcuts in Empty Cup state binds all 3 shortcuts to EmptyCupAlias sorted by rank`() {
        iconSwitcher.setCoffeeCupIcon(isFullCup = false)
        val inactiveState = AwakeState(
            isAwake = false,
            durationMinutes = 0
        )

        shortcutController.publishShortcuts(inactiveState)

        val shortcuts = shortcutManager.dynamicShortcuts.sortedBy { it.rank }
        assertEquals(3, shortcuts.size)

        val expectedAlias = AndroidIconSwitcher.emptyCupComponent(context)
        shortcuts.forEachIndexed { index, shortcut ->
            assertEquals("Shortcut at index $index must have rank $index", index, shortcut.rank)
            assertEquals("Shortcut must bind to EmptyCupAlias", expectedAlias, shortcut.activity)
        }

        assertEquals(AndroidShortcutPublisher.ID_STATUS_TOGGLE, shortcuts[0].id)
        assertTrue(shortcuts[0].shortLabel.toString().contains("Suspend", ignoreCase = true))

        assertEquals(AndroidShortcutPublisher.ID_PRESET_TIMER, shortcuts[1].id)
        assertTrue(shortcuts[1].shortLabel.toString().contains("Infinite", ignoreCase = true))

        assertEquals(AndroidShortcutPublisher.ID_OPEN_SETTINGS, shortcuts[2].id)
        assertTrue(shortcuts[2].shortLabel.toString().contains("Settings", ignoreCase = true))
    }

    @Test
    fun `publishShortcuts in Full Cup state rebinds all 3 shortcuts to FullCupAlias and updates labels`() {
        iconSwitcher.setCoffeeCupIcon(isFullCup = true)
        val activeState = AwakeState(
            isAwake = true,
            durationMinutes = 60
        )

        shortcutController.publishShortcuts(activeState)

        val shortcuts = shortcutManager.dynamicShortcuts.sortedBy { it.rank }
        assertEquals(3, shortcuts.size)

        val expectedAlias = AndroidIconSwitcher.fullCupComponent(context)
        shortcuts.forEach { shortcut ->
            assertEquals("Shortcut must rebind to FullCupAlias when awake", expectedAlias, shortcut.activity)
        }

        assertTrue(shortcuts[0].shortLabel.toString().contains("Awake", ignoreCase = true))
        assertTrue(
            shortcuts[1].shortLabel.toString().contains("1h", ignoreCase = true) ||
                shortcuts[1].shortLabel.toString().contains("60m", ignoreCase = true)
        )
        assertEquals(AndroidShortcutPublisher.ID_OPEN_SETTINGS, shortcuts[2].id)
    }
}
