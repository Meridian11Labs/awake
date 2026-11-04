package com.meridian11.books.awake.widget

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.domain.WidgetUpdaterPort
import com.meridian11.books.awake.service.KeepAwakeTileService

/**
 * Production implementation of [WidgetUpdaterPort] that refreshes both:
 * 1. All placed instances of the [AwakeSquircleWidgetProvider] home/desktop widget (1x1, 2x1, and Expanded).
 * 2. The Quick Settings tile ([KeepAwakeTileService]) via both direct active instance push and [TileService.requestListeningState].
 */
class AndroidWidgetUpdater(
    private val appContext: Context
) : WidgetUpdaterPort {

    override fun updateWidgets(state: AwakeState) {
        AwakeSquircleWidgetProvider.updateAllWidgets(appContext, state)
        runCatching {
            KeepAwakeTileService.activeInstance?.updateTileFromState(state)
            TileService.requestListeningState(
                appContext,
                ComponentName(appContext, KeepAwakeTileService::class.java)
            )
        }
    }
}
