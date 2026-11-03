package com.meridian11.books.awake.ui

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AwakeApp::class)
class QuickGlassPanelActivityTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

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
    fun `QuickGlassPanelActivity enables frosted glass blur and toggles Awake and main modes in Compose`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            val activity = Robolectric.buildActivity(QuickGlassPanelActivity::class.java)
                .setup()
                .get()

            assertTrue("QuickGlassPanelActivity must enable frosted glass blur", activity.isGlassBlurConfigured)

            // Toggle Awake via the glass hero toggle card
            assertFalse(repository.stateFlow.first().isAwake)
            composeRule.onNodeWithTag("glass_hero_toggle").performClick()
            composeRule.waitForIdle()
            assertTrue("Clicking glass hero card must enable Keep Awake", repository.stateFlow.first().isAwake)
            assertEquals(true, iconPort.lastIsFullCup)

            // Select 60m ("1h") mode pill in the glass panel
            composeRule.onNodeWithTag("glass_mode_60").performClick()
            composeRule.waitForIdle()
            assertEquals(60, repository.stateFlow.first().durationMinutes)
            assertTrue(repository.stateFlow.first().isAwake)
        }

    @Test
    fun `clicking Settings item in QuickGlassPanelActivity opens full SettingsDialogActivity and closes glass panel`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            val activity = Robolectric.buildActivity(QuickGlassPanelActivity::class.java)
                .setup()
                .get()

            composeRule.onNodeWithTag("glass_open_settings").performClick()
            composeRule.waitForIdle()

            val nextIntent = shadowOf(app).nextStartedActivity
            assertNotNull("Clicking Settings item in glass panel must open SettingsDialogActivity", nextIntent)
            assertEquals(SettingsDialogActivity::class.java.name, nextIntent.component?.className)
            assertTrue("Glass panel must finish after opening full SettingsDialogActivity", activity.isFinishing)
        }
}
