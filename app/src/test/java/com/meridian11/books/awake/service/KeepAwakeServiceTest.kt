package com.meridian11.books.awake.service

import android.content.Intent
import android.os.PowerManager
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPowerManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AwakeApp::class)
class KeepAwakeServiceTest {

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
    fun `onStartCommand starts foreground notification and acquires WakeLock, onDestroy releases it`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            val serviceController = Robolectric.buildService(KeepAwakeService::class.java).create()
            val service = serviceController.get()

            val startIntent = Intent(app, KeepAwakeService::class.java).apply {
                action = KeepAwakeService.ACTION_START_OR_UPDATE
                putExtra(KeepAwakeService.EXTRA_DURATION_MINUTES, 60)
                putExtra(KeepAwakeService.EXTRA_ALLOW_DIMMING, false)
            }
            service.onStartCommand(startIntent, 0, 1)

            val latestWakeLock: PowerManager.WakeLock? = ShadowPowerManager.getLatestWakeLock()
            assertNotNull("Service must acquire a WakeLock on start", latestWakeLock)
            assertTrue("WakeLock must be held while service is active", latestWakeLock!!.isHeld)

            val shadowService = shadowOf(service)
            assertNotNull("Foreground notification must be posted", shadowService.lastForegroundNotification)

            serviceController.destroy()
            assertFalse("WakeLock must be released on service destroy", latestWakeLock.isHeld)
        }

    @Test
    fun `ACTION_STOP_KEEP_AWAKE disables Keep Awake in controller and stops service`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )
            app.controller.toggle() // ON
            assertTrue(repository.stateFlow.first().isAwake)

            val serviceController = Robolectric.buildService(KeepAwakeService::class.java).create()
            val service = serviceController.get()

            val stopIntent = Intent(app, KeepAwakeService::class.java).apply {
                action = KeepAwakeService.ACTION_STOP_KEEP_AWAKE
            }
            service.onStartCommand(stopIntent, 0, 2)
            serviceController.destroy()

            assertFalse("Controller state must be OFF after ACTION_STOP_KEEP_AWAKE", repository.stateFlow.first().isAwake)
            assertTrue("Service must stop itself", shadowOf(service).isStoppedBySelf)
        }
}
