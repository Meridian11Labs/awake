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
    fun `enabling Tap Simulation before disclosure accepted shows Compose Prominent Disclosure modal with Decline and Agree actions`() =
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

            // Prominent disclosure modal must be shown and tapSimulationEnabled must still be false
            composeRule.onNodeWithTag("dialog_accessibility_disclosure").assertIsDisplayed()
            assertFalse(repository.stateFlow.first().tapSimulationEnabled)
            assertFalse(repository.stateFlow.first().accessibilityDisclosureAccepted)

            // Clicking Decline dismisses modal and keeps both false
            composeRule.onNodeWithTag("btn_disclosure_decline").performClick()
            composeRule.waitForIdle()
            assertFalse(repository.stateFlow.first().tapSimulationEnabled)
            assertFalse(repository.stateFlow.first().accessibilityDisclosureAccepted)

            // Trigger again and click Agree & Continue
            composeRule.onNodeWithTag("switch_tap_simulation").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("btn_disclosure_accept").performClick()
            composeRule.waitForIdle()

            assertTrue(repository.stateFlow.first().accessibilityDisclosureAccepted)
            assertTrue(repository.stateFlow.first().tapSimulationEnabled)
        }

    @Test
    fun `subtle footer links open About, Privacy Policy, Terms of Service, and Accessibility subpage with Meridian11 LLC ownership and consent revocation`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.setAccessibilityDisclosureAccepted(true)
            repository.setTapSimulationConfig(enabled = true, intervalSeconds = 60)

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

            // Click subtle About footer link
            composeRule.onNodeWithTag("link_footer_about").performScrollTo().performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("screen_legal_subpage").assertIsDisplayed()
            composeRule.onNodeWithTag("tv_meridian11_ownership", useUnmergedTree = true).assertIsDisplayed()

            // Switch to Privacy Policy tab
            composeRule.onNodeWithTag("tab_legal_privacy").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("tv_legal_privacy_body", useUnmergedTree = true).assertIsDisplayed()

            // Switch to Terms of Service tab
            composeRule.onNodeWithTag("tab_legal_terms").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("tv_legal_terms_body", useUnmergedTree = true).assertIsDisplayed()

            // Switch to Accessibility API tab and revoke consent
            composeRule.onNodeWithTag("tab_legal_accessibility").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("btn_revoke_accessibility_consent").performScrollTo().performClick()
            composeRule.waitForIdle()

            assertFalse(repository.stateFlow.first().accessibilityDisclosureAccepted)
            assertFalse(repository.stateFlow.first().tapSimulationEnabled)

            // Return to main Settings window
            composeRule.onNodeWithTag("btn_legal_back").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("coffee_cup_hero", useUnmergedTree = true).assertIsDisplayed()
        }
}
