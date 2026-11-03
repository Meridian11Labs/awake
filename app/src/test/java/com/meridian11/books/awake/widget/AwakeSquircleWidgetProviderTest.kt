package com.meridian11.books.awake.widget

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.domain.FakeIconSwitcherPort
import com.meridian11.books.awake.domain.FakeKeepAwakeEnginePort
import com.meridian11.books.awake.domain.FakeShortcutPublisherPort
import com.meridian11.books.awake.domain.FakeWidgetUpdaterPort
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AwakeApp::class)
class AwakeSquircleWidgetProviderTest {

    private lateinit var app: AwakeApp
    private lateinit var repository: InMemoryAwakePreferencesRepository
    private lateinit var iconPort: FakeIconSwitcherPort
    private lateinit var shortcutPort: FakeShortcutPublisherPort
    private lateinit var enginePort: FakeKeepAwakeEnginePort
    private lateinit var widgetPort: FakeWidgetUpdaterPort

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        repository = InMemoryAwakePreferencesRepository()
        iconPort = FakeIconSwitcherPort()
        shortcutPort = FakeShortcutPublisherPort()
        enginePort = FakeKeepAwakeEnginePort()
        widgetPort = FakeWidgetUpdaterPort()
    }

    @Test
    fun `tapping squircle widget via ACTION_WIDGET_TOGGLE toggles Keep Awake ON and OFF and updates widget state`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                widgetUpdater = widgetPort,
                scope = backgroundScope
            )

            val receiver = AwakeSquircleWidgetProvider()
            val toggleIntent = Intent(app, AwakeSquircleWidgetProvider::class.java).apply {
                action = AwakeSquircleWidgetProvider.ACTION_WIDGET_TOGGLE
            }

            // 1st tap on squircle widget -> activates Keep Awake (Filled Cup)
            receiver.onReceive(app, toggleIntent)
            assertTrue("Squircle widget tap must activate Keep Awake", repository.stateFlow.first().isAwake)
            assertEquals(true, iconPort.lastIsFullCup)
            assertEquals(true, widgetPort.lastState?.isAwake)

            // 2nd tap on squircle widget -> deactivates Keep Awake (Half-Empty Cup)
            receiver.onReceive(app, toggleIntent)
            assertFalse("Second squircle widget tap must deactivate Keep Awake", repository.stateFlow.first().isAwake)
            assertEquals(false, iconPort.lastIsFullCup)
            assertEquals(false, widgetPort.lastState?.isAwake)
        }

    @Test
    fun `resolveVisualSpec returns Filled Cup resources when active and Half-Empty Cup resources when deactive`() {
        val inactiveSpec = AwakeSquircleWidgetProvider.resolveVisualSpec(AwakeState(isAwake = false))
        assertEquals(R.drawable.ic_launcher_foreground_empty, inactiveSpec.cupIconRes)
        assertEquals(R.drawable.bg_widget_squircle_empty, inactiveSpec.backgroundRes)

        val activeSpec = AwakeSquircleWidgetProvider.resolveVisualSpec(AwakeState(isAwake = true, durationMinutes = 60))
        assertEquals(R.drawable.ic_launcher_foreground_full, activeSpec.cupIconRes)
        assertEquals(R.drawable.bg_widget_squircle_full, activeSpec.backgroundRes)
    }
}
