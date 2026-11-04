package com.meridian11.books.awake.ui

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.domain.FakeIconSwitcherPort
import com.meridian11.books.awake.domain.FakeKeepAwakeEnginePort
import com.meridian11.books.awake.domain.FakeShortcutPublisherPort
import com.meridian11.books.awake.domain.InMemoryAwakePreferencesRepository
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AwakeApp::class)
class ShortcutActionActivityTest {

    private lateinit var app: AwakeApp
    private lateinit var repository: InMemoryAwakePreferencesRepository
    private lateinit var iconPort: FakeIconSwitcherPort
    private lateinit var shortcutPort: FakeShortcutPublisherPort
    private lateinit var enginePort: FakeKeepAwakeEnginePort

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        repository = InMemoryAwakePreferencesRepository()
        iconPort = FakeIconSwitcherPort()
        shortcutPort = FakeShortcutPublisherPort()
        enginePort = FakeKeepAwakeEnginePort()
    }

    @Test
    fun `ACTION_SHORTCUT_CYCLE_TIMER cycles duration preset from 0 to 30m, activates Full Cup, and finishes`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            val intent = Intent(AndroidShortcutPublisher.ACTION_SHORTCUT_CYCLE_TIMER)
            val activity = Robolectric.buildActivity(ShortcutActionActivity::class.java, intent)
                .create()
                .get()

            assertTrue(activity.isFinishing)
            val state = repository.stateFlow.first()
            assertEquals(30, state.durationMinutes)
            assertTrue(state.isAwake)
            assertEquals(true, iconPort.lastIsFullCup)
        }

    @Test
    fun `ACTION_SHORTCUT_TOGGLE_AWAKE toggles Keep Awake ON and finishes`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            val intent = Intent(AndroidShortcutPublisher.ACTION_SHORTCUT_TOGGLE_AWAKE)
            val activity = Robolectric.buildActivity(ShortcutActionActivity::class.java, intent)
                .create()
                .get()

            assertTrue(activity.isFinishing)
            assertTrue(repository.stateFlow.first().isAwake)
            assertEquals(true, iconPort.lastIsFullCup)
        }
}
