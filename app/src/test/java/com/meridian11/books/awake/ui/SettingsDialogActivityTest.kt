package com.meridian11.books.awake.ui

import android.content.Intent
import android.service.quicksettings.TileService
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AwakeApp::class)
class SettingsDialogActivityTest {

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
    fun `toggling master Awake switch and guardrail switches in Compose SettingsDialogActivity updates repository and keeps full app non-blurred`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            val activity = Robolectric.buildActivity(SettingsDialogActivity::class.java)
                .setup()
                .get()

            assertFalse("Full app SettingsDialogActivity must NOT have glass blur", activity.isGlassBlurConfigured)

            composeRule.onNodeWithTag("coffee_cup_hero", useUnmergedTree = true).assertIsDisplayed()
            assertFalse(repository.stateFlow.first().isAwake)

            composeRule.onNodeWithTag("switch_master_awake").performClick()
            composeRule.waitForIdle()
            assertTrue(repository.stateFlow.first().isAwake)
            assertEquals(true, iconPort.lastIsFullCup)

            composeRule.onNodeWithTag("switch_allow_dimming").performScrollTo().performClick()
            composeRule.waitForIdle()
            assertTrue(repository.stateFlow.first().allowScreenDimming)
        }

    @Test
    fun `toggling Hide settings tap icon to toggle switch updates directToggleOnTap in repository`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            Robolectric.buildActivity(SettingsDialogActivity::class.java)
                .setup()
                .get()

            assertFalse(repository.stateFlow.first().directToggleOnTap)

            composeRule.onNodeWithTag("switch_direct_toggle_on_tap").performScrollTo().performClick()
            composeRule.waitForIdle()
            assertTrue(
                "Enabling switch must set directToggleOnTap = true in repository",
                repository.stateFlow.first().directToggleOnTap
            )

            composeRule.onNodeWithTag("tv_qs_tile_info").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("btn_add_qs_tile").performScrollTo().assertIsDisplayed()
        }

    @Test
    fun `SettingsDialogActivity resolves and launches from Quick Settings long-press ACTION_QS_TILE_PREFERENCES`() =
        runTest(UnconfinedTestDispatcher()) {
            val qsIntent = Intent(TileService.ACTION_QS_TILE_PREFERENCES).apply {
                setPackage(app.packageName)
            }
            val resolved = app.packageManager.resolveActivity(qsIntent, 0)
            assertNotNull("SettingsDialogActivity must handle ACTION_QS_TILE_PREFERENCES for QS tile long-press", resolved)
            assertEquals(SettingsDialogActivity::class.java.name, resolved?.activityInfo?.name)
        }

    @Test
    fun `enabling Tap Simulation before disclosure accepted shows Prominent Disclosure dialog`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            Robolectric.buildActivity(SettingsDialogActivity::class.java)
                .setup()
                .get()

            composeRule.onNodeWithTag("switch_tap_simulation").performScrollTo().performClick()
            composeRule.waitForIdle()

            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            assertTrue("Prominent disclosure AlertDialog must be shown before enabling", dialog != null && dialog.isShowing)
        }
}
