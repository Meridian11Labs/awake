package com.meridian11.books.awake.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Companion Quick Settings Tile for Googlebook system tray.
 * Synchronizes in real time with the Taskbar coffee cup icon, Settings window, and Squircle widget.
 */
class KeepAwakeTileService : TileService() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled error in KeepAwakeTileService", throwable)
    }
    private var tileScope: CoroutineScope? = null

    override fun onTileAdded() {
        super.onTileAdded()
        val app = applicationContext as? AwakeApp ?: return
        app.appScope.launch {
            val state = app.repository.stateFlow.first()
            updateTileFromState(state)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        activeInstance = this
        tileScope?.cancel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + exceptionHandler)
        tileScope = scope
        val app = applicationContext as? AwakeApp ?: return
        scope.launch {
            app.repository.stateFlow.collectLatest { state ->
                updateTileFromState(state)
            }
        }
    }

    override fun onStopListening() {
        if (activeInstance === this) {
            activeInstance = null
        }
        tileScope?.cancel()
        tileScope = null
        super.onStopListening()
    }

    override fun onDestroy() {
        if (activeInstance === this) {
            activeInstance = null
        }
        tileScope?.cancel()
        tileScope = null
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext as? AwakeApp ?: return
        app.appScope.launch {
            val updated = app.controller.toggle()
            updateTileFromState(updated)
        }
    }

    internal fun updateTileFromState(state: AwakeState) {
        val tile = qsTile ?: return
        runCatching {
            tile.state = if (state.isAwake) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = getString(R.string.app_name)
            tile.subtitle = if (state.isAwake) {
                AndroidShortcutPublisher.formatTimerSubtitle(state.durationMinutes)
            } else {
                getString(R.string.tile_subtitle_normal_sleep)
            }
            val iconRes = if (state.isAwake) {
                R.drawable.ic_stat_coffee_full
            } else {
                R.drawable.ic_stat_coffee_empty
            }
            tile.icon = Icon.createWithResource(this, iconRes)
            tile.updateTile()
        }
    }

    companion object {
        private const val TAG = "KeepAwakeTileService"

        @Volatile
        internal var activeInstance: KeepAwakeTileService? = null
    }
}
