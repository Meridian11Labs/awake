package com.meridian11.books.awake.service

import androidx.test.core.app.ApplicationProvider
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.domain.FakeIconSwitcherPort
import com.meridian11.books.awake.domain.FakeKeepAwakeEnginePort
import com.meridian11.books.awake.domain.FakeShortcutPublisherPort
import com.meridian11.books.awake.domain.InMemoryAwakePreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class KeepAwakeTileServiceTest {

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
    fun `tapping Quick Settings tile toggles Keep Awake ON and tapping again toggles OFF`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            val tileService = Robolectric.buildService(KeepAwakeTileService::class.java)
                .create()
                .get()

            // First tap on QS tile: activates Keep Awake (Filled Cup)
            tileService.onClick()
            assertTrue("First QS tile tap must activate Keep Awake", repository.stateFlow.first().isAwake)
            assertEquals(true, iconPort.lastIsFullCup)
            assertTrue(enginePort.isRunning)

            // Second tap on QS tile: deactivates Keep Awake (Half-Empty Cup)
            tileService.onClick()
            assertFalse("Second QS tile tap must deactivate Keep Awake", repository.stateFlow.first().isAwake)
            assertEquals(false, iconPort.lastIsFullCup)
            assertFalse(enginePort.isRunning)
        }
}
