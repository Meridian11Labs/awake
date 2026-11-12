package com.meridian11.books.awake

import android.app.Application
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.meridian11.books.awake.domain.AwakeController
import com.meridian11.books.awake.domain.AwakePreferencesRepository
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.domain.DataStoreAwakePreferencesRepository
import com.meridian11.books.awake.domain.ExpirationAlarmPort
import com.meridian11.books.awake.domain.IconSwitcherPort
import com.meridian11.books.awake.domain.KeepAwakeEnginePort
import com.meridian11.books.awake.domain.NoOpExpirationAlarmPort
import com.meridian11.books.awake.domain.NoOpWidgetUpdaterPort
import com.meridian11.books.awake.domain.ShortcutPublisherPort
import com.meridian11.books.awake.domain.WidgetUpdaterPort
import com.meridian11.books.awake.platform.AndroidExpirationAlarmScheduler
import com.meridian11.books.awake.platform.AndroidIconSwitcher
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import com.meridian11.books.awake.service.AndroidKeepAwakeEngineAdapter
import com.meridian11.books.awake.widget.AndroidWidgetUpdater
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Application container providing dependencies for Awake.
 * Supports test-time dependency overrides for Robolectric and instrumentation tests,
 * exposes a hot [uiStateFlow] for flicker-free Compose rendering, and serializes all
 * surface transitions (Taskbar Icon, Shortcuts, Widget, and Quick Settings Tile)
 * through [AwakeController].
 */
class AwakeApp : Application() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled coroutine exception in AwakeApp", throwable)
    }

    var appScope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + exceptionHandler
    )
        private set

    private var startupSyncJob: Job? = null

    lateinit var repository: AwakePreferencesRepository
        private set

    lateinit var uiStateFlow: StateFlow<AwakeState>
        private set

    lateinit var iconSwitcher: IconSwitcherPort
        private set

    lateinit var shortcutPublisher: ShortcutPublisherPort
        private set

    lateinit var enginePort: KeepAwakeEnginePort
        private set

    lateinit var widgetUpdater: WidgetUpdaterPort
        private set

    lateinit var alarmPort: ExpirationAlarmPort
        private set

    lateinit var controller: AwakeController
        private set

    override fun onCreate() {
        super.onCreate()
        if (!::controller.isInitialized) {
            val androidIconSwitcher = AndroidIconSwitcher(this)
            val androidShortcutPublisher = AndroidShortcutPublisher(this, androidIconSwitcher)
            val dataStoreRepo = DataStoreAwakePreferencesRepository(this)
            val androidEngine = AndroidKeepAwakeEngineAdapter(this)
            val androidWidgetUpdater = AndroidWidgetUpdater(this)
            val androidAlarmScheduler = AndroidExpirationAlarmScheduler(this)

            configureDependencies(
                repository = dataStoreRepo,
                iconSwitcher = androidIconSwitcher,
                shortcutPublisher = androidShortcutPublisher,
                enginePort = androidEngine,
                widgetUpdater = androidWidgetUpdater,
                alarmPort = androidAlarmScheduler,
                scope = appScope,
                clockMillis = { SystemClock.elapsedRealtime().coerceAtLeast(1L) },
                bootCountProvider = { readCurrentBootCount() }
            )
            // Ensure dynamic right-click shortcuts & squircle widgets are synced on process start
            startupSyncJob = appScope.launch {
                controller.syncCurrentState()
            }
        }
    }

    fun cancelStartupSync() {
        startupSyncJob?.cancel()
        startupSyncJob = null
    }

    fun configureDependencies(
        repository: AwakePreferencesRepository,
        iconSwitcher: IconSwitcherPort,
        shortcutPublisher: ShortcutPublisherPort,
        enginePort: KeepAwakeEnginePort,
        widgetUpdater: WidgetUpdaterPort = NoOpWidgetUpdaterPort,
        alarmPort: ExpirationAlarmPort = NoOpExpirationAlarmPort,
        scope: CoroutineScope = appScope,
        clockMillis: () -> Long = { SystemClock.elapsedRealtime().coerceAtLeast(1L) },
        bootCountProvider: () -> Int = { -1 }
    ) {
        cancelStartupSync()
        this.appScope = scope
        this.repository = repository
        this.iconSwitcher = iconSwitcher
        this.shortcutPublisher = shortcutPublisher
        this.enginePort = enginePort
        this.widgetUpdater = widgetUpdater
        this.alarmPort = alarmPort
        this.uiStateFlow = repository.stateFlow.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AwakeState()
        )
        this.controller = AwakeController(
            repository = repository,
            iconPort = iconSwitcher,
            shortcutPort = shortcutPublisher,
            enginePort = enginePort,
            externalScope = scope,
            widgetPort = widgetUpdater,
            alarmPort = alarmPort,
            clockMillis = clockMillis,
            bootCountProvider = bootCountProvider
        )
    }

    private fun readCurrentBootCount(): Int {
        return runCatching {
            Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
        }.getOrDefault(-1)
    }

    companion object {
        private const val TAG = "AwakeApp"
    }
}
