package com.meridian11.books.awake.receiver

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.domain.AwakeState
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AwakeApp::class)
class SystemEventReceiverTest {

    private lateinit var app: AwakeApp
    private lateinit var repository: InMemoryAwakePreferencesRepository
    private lateinit var iconPort: FakeIconSwitcherPort
    private lateinit var shortcutPort: FakeShortcutPublisherPort
    private lateinit var enginePort: FakeKeepAwakeEnginePort

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        repository = InMemoryAwakePreferencesRepository(
            AwakeState(isAwake = true, autoAwakeOnChargerEnabled = true)
        )
        iconPort = FakeIconSwitcherPort()
        shortcutPort = FakeShortcutPublisherPort()
        enginePort = FakeKeepAwakeEnginePort()
    }

    @Test
    fun `ACTION_BOOT_COMPLETED resets awake state to Empty Cup so taskbar icon matches stopped service`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            val receiver = SystemEventReceiver()
            receiver.onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

            assertFalse("Boot must reset awake state to Empty Cup", repository.stateFlow.first().isAwake)
            assertEquals(false, iconPort.lastIsFullCup)
        }

    @Test
    fun `ACTION_MY_PACKAGE_REPLACED preserves active Keep-Awake session across app updates`() =
        runTest(UnconfinedTestDispatcher()) {
            app.configureDependencies(
                repository = repository,
                iconSwitcher = iconPort,
                shortcutPublisher = shortcutPort,
                enginePort = enginePort,
                scope = backgroundScope
            )

            val receiver = SystemEventReceiver()
            receiver.onReceive(app, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

            assertTrue("Package update must preserve an active Keep-Awake session", repository.stateFlow.first().isAwake)
            assertEquals(true, iconPort.lastIsFullCup)
            assertTrue("Engine must be restarted after package update when active", enginePort.isRunning)
        }
}
