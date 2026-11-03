package com.meridian11.books.awake.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Companion Quick Settings Tile in the Googlebook system tray that stays
 * synchronized with the Taskbar Coffee Cup icon.
 */
class KeepAwakeTileService : TileService() {

    private var tileScope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        tileScope = scope
        val app = applicationContext as? AwakeApp ?: return

        scope.launch {
            app.repository.stateFlow.collectLatest { state ->
                val tile = qsTile ?: return@collectLatest
                tile.state = if (state.isAwake) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = "Awake"
                tile.subtitle = if (state.isAwake) {
                    AndroidShortcutPublisher.formatDurationShort(state.durationMinutes)
                } else {
                    "Normal Suspend"
                }
                tile.icon = Icon.createWithResource(
                    this@KeepAwakeTileService,
                    if (state.isAwake) R.drawable.ic_stat_coffee_full
                    else R.drawable.ic_stat_coffee_empty
                )
                tile.updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext as? AwakeApp ?: return
        app.appScope.launch {
            app.controller.toggle()
        }
    }

    override fun onStopListening() {
        tileScope?.cancel()
        tileScope = null
        super.onStopListening()
    }
}
