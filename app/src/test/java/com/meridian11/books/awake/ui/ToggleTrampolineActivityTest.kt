package com.meridian11.books.awake.ui

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AwakeApp::class)
class ToggleTrampolineActivityTest {

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
    fun `by default when directToggleOnTap is false, launching ToggleTrampolineActivity opens SettingsDialogActivity without toggling`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            val controller = Robolectric.buildActivity(ToggleTrampolineActivity::class.java).create()
            val activity = controller.get()

            assertTrue("Trampoline must finish immediately", activity.isFinishing)
            assertFalse("Keep Awake state must remain unchanged until toggled in window", repository.stateFlow.first().isAwake)

            val nextIntent = shadowOf(app).nextStartedActivity
            assertNotNull("Must launch SettingsDialogActivity by default when tapped", nextIntent)
            assertEquals(SettingsDialogActivity::class.java.name, nextIntent.component?.className)
        }

    @Test
    fun `when directToggleOnTap is true, launching ToggleTrampolineActivity toggles Keep Awake state ON and OFF with zero window`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.setDirectToggleOnTap(true)
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            // 1st click: OFF -> ON with zero window
            val firstController = Robolectric.buildActivity(ToggleTrampolineActivity::class.java).create()
            assertTrue("Activity must finish inside onCreate with zero UI", firstController.get().isFinishing)
            assertNull("Must NOT open SettingsDialogActivity when directToggleOnTap is true", shadowOf(app).nextStartedActivity)
            assertTrue("Keep Awake must be ON after first click", repository.stateFlow.first().isAwake)
            assertEquals(true, iconPort.lastIsFullCup)
            assertTrue(enginePort.isRunning)

            // 2nd click: ON -> OFF with zero window
            val secondController = Robolectric.buildActivity(ToggleTrampolineActivity::class.java).create()
            assertTrue(secondController.get().isFinishing)
            assertNull("Must NOT open SettingsDialogActivity on second click", shadowOf(app).nextStartedActivity)
            assertFalse("Keep Awake must be OFF after second click", repository.stateFlow.first().isAwake)
            assertEquals(false, iconPort.lastIsFullCup)
            assertFalse(enginePort.isRunning)
        }
}
