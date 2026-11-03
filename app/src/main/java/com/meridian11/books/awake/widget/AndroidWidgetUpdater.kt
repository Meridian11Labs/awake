package com.meridian11.books.awake.widget

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.domain.WidgetUpdaterPort
import com.meridian11.books.awake.service.KeepAwakeTileService

/**
 * Android platform adapter that updates all active 1x1 Squircle AppWidgets
 * and requests a Quick Settings tile refresh whenever AwakeState transitions.
 */
class AndroidWidgetUpdater(context: Context) : WidgetUpdaterPort {

    private val appContext = context.applicationContext

    override fun updateWidgets(state: AwakeState) {
        try {
            AwakeSquircleWidgetProvider.updateAllWidgets(appContext, state)
        } catch (_: Throwable) {
        }
        try {
            TileService.requestListeningState(
                appContext,
                ComponentName(appContext, KeepAwakeTileService::class.java)
            )
        } catch (_: Throwable) {
        }
    }
}
