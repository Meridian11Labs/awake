package com.meridian11.books.awake.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Companion Quick Settings Tile for Googlebook / Android Desktop system tray.
 * Synchronizes in real time with the Taskbar coffee cup icon, Settings window, and Squircle widget.
 */
class KeepAwakeTileService : TileService() {

    private var tileScope: CoroutineScope? = null

    override fun onTileAdded() {
        super.onTileAdded()
        syncCurrentTileState()
    }

    override fun onStartListening() {
        super.onStartListening()
        activeInstance = this
        tileScope?.cancel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        tileScope = scope
        val app = applicationContext as AwakeApp
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
        val app = applicationContext as AwakeApp
        app.appScope.launch {
            val updated = app.controller.toggle()
            updateTileFromState(updated)
        }
    }

    private fun syncCurrentTileState() {
        val app = applicationContext as AwakeApp
        app.appScope.launch {
            val state = app.controller.syncCurrentState()
            updateTileFromState(state)
        }
    }

    internal fun updateTileFromState(state: AwakeState) {
        val tile = qsTile ?: return
        tile.state = if (state.isAwake) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (state.isAwake) {
                if (state.durationMinutes == 0) "Infinite" else "${state.durationMinutes}m timer"
            } else {
                "Normal sleep"
            }
        }
        val iconRes = if (state.isAwake) R.drawable.ic_stat_coffee_full else R.drawable.ic_stat_coffee_empty
        tile.icon = Icon.createWithResource(this, iconRes)
        tile.updateTile()
    }

    companion object {
        @Volatile
        internal var activeInstance: KeepAwakeTileService? = null
    }
}
