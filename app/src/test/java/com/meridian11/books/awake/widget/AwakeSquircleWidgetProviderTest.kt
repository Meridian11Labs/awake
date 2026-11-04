package com.meridian11.books.awake.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
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
import org.junit.Assert.assertNotNull
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
    fun `expanded widget controls update duration and tap simulation and synchronize all surfaces`() =
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

            // Tap 60m duration preset on expanded widget -> activates Keep Awake with 60m timer
            receiver.onReceive(
                app,
                Intent(app, AwakeSquircleWidgetProvider::class.java).apply {
                    action = AwakeSquircleWidgetProvider.ACTION_WIDGET_SET_DURATION
                    putExtra(AwakeSquircleWidgetProvider.EXTRA_DURATION_MINUTES, 60)
                }
            )
            val afterDuration = repository.stateFlow.first()
            assertTrue("Selecting duration on expanded widget must activate Keep Awake", afterDuration.isAwake)
            assertEquals(60, afterDuration.durationMinutes)
            assertEquals(true, iconPort.lastIsFullCup)
            assertEquals(60, widgetPort.lastState?.durationMinutes)

            // Tap Tap Sim toggle on expanded widget -> toggles tap simulation ON
            receiver.onReceive(
                app,
                Intent(app, AwakeSquircleWidgetProvider::class.java).apply {
                    action = AwakeSquircleWidgetProvider.ACTION_WIDGET_TOGGLE_TAP_SIM
                }
            )
            val afterTapSim = repository.stateFlow.first()
            assertTrue("Tapping Tap Sim on expanded widget must enable tap simulation", afterTapSim.tapSimulationEnabled)
            assertEquals(true, widgetPort.lastState?.tapSimulationEnabled)
        }

    @Test
    fun `resolveVisualSpec returns Filled Cup resources when active and Half-Empty Cup resources when deactive`() {
        val inactiveSpec = AwakeSquircleWidgetProvider.resolveVisualSpec(AwakeState(isAwake = false))
        assertEquals(R.drawable.ic_widget_cup_empty, inactiveSpec.cupIconRes)
        assertEquals(R.drawable.bg_widget_squircle_empty, inactiveSpec.backgroundRes)

        val activeSpec = AwakeSquircleWidgetProvider.resolveVisualSpec(AwakeState(isAwake = true, durationMinutes = 60))
        assertEquals(R.drawable.ic_widget_cup_full, activeSpec.cupIconRes)
        assertEquals(R.drawable.bg_widget_squircle_full, activeSpec.backgroundRes)
    }

    @Test
    fun `resolveWidgetSizeBucket maps 1x1, 2x1, and Expanded dimensions to responsive widget layouts`() {
        val compact = AwakeSquircleWidgetProvider.resolveWidgetSizeBucket(widthDp = 96, heightDp = 95)
        assertEquals(AwakeSquircleWidgetProvider.WidgetSizeBucket.COMPACT_1X1, compact)
        assertEquals(R.layout.widget_awake_squircle, compact.layoutRes)

        val medium = AwakeSquircleWidgetProvider.resolveWidgetSizeBucket(widthDp = 216, heightDp = 95)
        assertEquals(AwakeSquircleWidgetProvider.WidgetSizeBucket.MEDIUM_2X1, medium)
        assertEquals(R.layout.widget_awake_medium, medium.layoutRes)

        val expanded = AwakeSquircleWidgetProvider.resolveWidgetSizeBucket(widthDp = 216, heightDp = 215)
        assertEquals(AwakeSquircleWidgetProvider.WidgetSizeBucket.EXPANDED, expanded)
        assertEquals(R.layout.widget_awake_expanded, expanded.layoutRes)
    }

    @Test
    fun `widget provider info XML defines 1x1 default minimum size (28dp x 28dp) and horizontal+vertical resizeMode`() {
        val parser = app.resources.getXml(R.xml.awake_squircle_widget_info)
        var foundProvider = false
        var minWidthAttr = ""
        var minHeightAttr = ""
        var resizeModeInt = 0
        val androidNs = "http://schemas.android.com/apk/res/android"
        while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "appwidget-provider") {
                foundProvider = true
                minWidthAttr = parser.getAttributeValue(androidNs, "minWidth") ?: ""
                minHeightAttr = parser.getAttributeValue(androidNs, "minHeight") ?: ""
                resizeModeInt = parser.getAttributeIntValue(androidNs, "resizeMode", 0)
            }
        }
        assertTrue(foundProvider)
        assertEquals("28.0dip", minWidthAttr)
        assertEquals("28.0dip", minHeightAttr)
        assertEquals(
            AppWidgetProviderInfo.RESIZE_HORIZONTAL or AppWidgetProviderInfo.RESIZE_VERTICAL,
            resizeModeInt
        )
    }
}
