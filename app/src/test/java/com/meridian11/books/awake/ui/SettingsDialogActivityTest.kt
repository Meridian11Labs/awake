package com.meridian11.books.awake.ui

import android.content.Intent
import android.service.quicksettings.TileService
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
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
    fun `toggling master Awake switch and guardrail switches in SettingsDialogActivity updates repository and hero cup pictogram`() =
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

            val heroCup = activity.findViewById<CoffeeCupPictogramView>(R.id.coffee_cup_hero)
            assertNotNull("Hero CoffeeCupPictogramView must be present", heroCup)
            assertEquals("Deactive state must render Half-Empty (0.5f) cup", 0.5f, heroCup.targetFillLevel, 0.01f)

            val masterSwitch = activity.findViewById<Switch>(R.id.switch_master_awake)
            assertFalse(masterSwitch.isChecked)

            masterSwitch.performClick()
            assertTrue(repository.stateFlow.first().isAwake)
            assertEquals(true, iconPort.lastIsFullCup)
            assertEquals("Active state must render Filled (1.0f) cup", 1.0f, heroCup.targetFillLevel, 0.01f)

            val dimSwitch = activity.findViewById<Switch>(R.id.switch_allow_dimming)
            assertFalse(dimSwitch.isChecked)
            dimSwitch.performClick()
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

            val activity = Robolectric.buildActivity(SettingsDialogActivity::class.java)
                .setup()
                .get()

            val hideSettingsSwitch = activity.findViewById<Switch>(R.id.switch_direct_toggle_on_tap)
            assertNotNull("Hide settings, tap icon to toggle switch must exist", hideSettingsSwitch)
            assertFalse(hideSettingsSwitch.isChecked)

            hideSettingsSwitch.performClick()
            assertTrue(
                "Enabling switch must set directToggleOnTap = true in repository",
                repository.stateFlow.first().directToggleOnTap
            )

            val qsInfo = activity.findViewById<TextView>(R.id.tv_qs_tile_info)
            assertNotNull("Window must explain the Quick Settings tile", qsInfo)
            assertTrue(qsInfo.text.toString().contains("Quick Settings", ignoreCase = true))

            val addQsBtn = activity.findViewById<Button>(R.id.btn_add_qs_tile)
            assertNotNull("Add Quick Settings Tile button must exist", addQsBtn)
        }

    @Test
    fun `SettingsDialogActivity resolves and launches from Quick Settings long-press ACTION_QS_TILE_PREFERENCES`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

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

            val activity = Robolectric.buildActivity(SettingsDialogActivity::class.java)
                .setup()
                .get()

            val tapSwitch = activity.findViewById<Switch>(R.id.switch_tap_simulation)
            assertFalse(tapSwitch.isChecked)

            tapSwitch.performClick()

            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            assertTrue("Prominent disclosure AlertDialog must be shown before enabling", dialog != null && dialog.isShowing)
        }
}
