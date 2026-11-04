package com.meridian11.books.awake

import android.app.Application
import com.meridian11.books.awake.domain.AwakeController
import com.meridian11.books.awake.domain.AwakePreferencesRepository
import com.meridian11.books.awake.domain.DataStoreAwakePreferencesRepository
import com.meridian11.books.awake.domain.IconSwitcherPort
import com.meridian11.books.awake.domain.KeepAwakeEnginePort
import com.meridian11.books.awake.domain.NoOpWidgetUpdaterPort
import com.meridian11.books.awake.domain.ShortcutPublisherPort
import com.meridian11.books.awake.domain.WidgetUpdaterPort
import com.meridian11.books.awake.platform.AndroidIconSwitcher
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import com.meridian11.books.awake.service.AndroidKeepAwakeEngineAdapter
import com.meridian11.books.awake.widget.AndroidWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Application container providing dependencies for Awake.
 * Also supports test-time dependency overrides for Robolectric and instrumentation tests,
 * and continuously synchronizes all surfaces (App Icon, Settings Window, Widget, and Quick Settings Tile)
 * whenever [AwakePreferencesRepository.stateFlow] emits.
 */
class AwakeApp : Application() {

    var appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private set

    private var startupSyncJob: Job? = null
    private var continuousSurfaceSyncJob: Job? = null

    lateinit var repository: AwakePreferencesRepository
        private set

    lateinit var iconSwitcher: IconSwitcherPort
        private set

    lateinit var shortcutPublisher: ShortcutPublisherPort
        private set

    lateinit var enginePort: KeepAwakeEnginePort
        private set

    lateinit var widgetUpdater: WidgetUpdaterPort
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

            configureDependencies(
                repository = dataStoreRepo,
                iconSwitcher = androidIconSwitcher,
                shortcutPublisher = androidShortcutPublisher,
                enginePort = androidEngine,
                widgetUpdater = androidWidgetUpdater,
                scope = appScope
            )
            // Ensure dynamic right-click shortcuts & squircle widgets are synced on process start
            startupSyncJob = appScope.launch {
                controller.syncCurrentState()
            }
        }
    }

    fun configureDependencies(
        repository: AwakePreferencesRepository,
        iconSwitcher: IconSwitcherPort,
        shortcutPublisher: ShortcutPublisherPort,
        enginePort: KeepAwakeEnginePort,
        widgetUpdater: WidgetUpdaterPort = NoOpWidgetUpdaterPort,
        scope: CoroutineScope = appScope
    ) {
        startupSyncJob?.cancel()
        startupSyncJob = null
        continuousSurfaceSyncJob?.cancel()
        this.appScope = scope
        this.repository = repository
        this.iconSwitcher = iconSwitcher
        this.shortcutPublisher = shortcutPublisher
        this.enginePort = enginePort
        this.widgetUpdater = widgetUpdater
        this.controller = AwakeController(
            repository = repository,
            iconPort = iconSwitcher,
            shortcutPort = shortcutPublisher,
            enginePort = enginePort,
            externalScope = scope,
            widgetPort = widgetUpdater
        )
        // Continuous reactive synchronization across App Icon, Shortcuts, Widget, and Quick Settings Tile
        continuousSurfaceSyncJob = scope.launch {
            var isFirstEmission = true
            repository.stateFlow.collectLatest { state ->
                if (isFirstEmission) {
                    isFirstEmission = false
                    return@collectLatest
                }
                iconSwitcher.setCoffeeCupIcon(state.isAwake)
                shortcutPublisher.publishShortcuts(state)
                widgetUpdater.updateWidgets(state)
            }
        }
    }
}
