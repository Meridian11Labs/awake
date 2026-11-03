package com.meridian11.books.awake.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Compact 1x1 Squircle AppWidgetProvider displaying the Material Pictogram Coffee Cup:
 * - Filled Cup on warm amber squircle when Keep Awake is active.
 * - Half-Empty Cup on cool slate squircle when inactive (normal suspend/resume).
 *
 * Tapping the squircle widget directly toggles Keep Awake and stays 100% synchronized
 * with the Taskbar icon, Quick Settings tile, and Main Window.
 */
class AwakeSquircleWidgetProvider : AppWidgetProvider() {

    data class WidgetVisualSpec(
        val cupIconRes: Int,
        val backgroundRes: Int,
        val contentDescription: String
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val app = context.applicationContext as? AwakeApp ?: return
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                val state = app.repository.stateFlow.first()
                updateAllWidgets(context, state, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE) {
            val app = context.applicationContext as? AwakeApp ?: return
            val pendingResult = goAsync()
            app.appScope.launch {
                try {
                    app.controller.toggle()
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.meridian11.books.awake.action.WIDGET_TOGGLE"

        fun resolveVisualSpec(state: AwakeState): WidgetVisualSpec {
            return if (state.isAwake) {
                WidgetVisualSpec(
                    cupIconRes = R.drawable.ic_launcher_foreground_full,
                    backgroundRes = R.drawable.bg_widget_squircle_full,
                    contentDescription = "Awake: Active (Filled Cup — Tap to deactivate)"
                )
            } else {
                WidgetVisualSpec(
                    cupIconRes = R.drawable.ic_launcher_foreground_empty,
                    backgroundRes = R.drawable.bg_widget_squircle_empty,
                    contentDescription = "Awake: Inactive (Half-Empty Cup — Tap to keep awake)"
                )
            }
        }

        fun buildRemoteViews(context: Context, state: AwakeState): RemoteViews {
            val spec = resolveVisualSpec(state)
            val views = RemoteViews(context.packageName, R.layout.widget_awake_squircle)
            views.setInt(R.id.widget_squircle_container, "setBackgroundResource", spec.backgroundRes)
            views.setImageViewResource(R.id.iv_widget_cup, spec.cupIconRes)
            views.setContentDescription(R.id.iv_widget_cup, spec.contentDescription)

            val toggleIntent = Intent(context, AwakeSquircleWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_squircle_container, pendingIntent)
            views.setOnClickPendingIntent(R.id.iv_widget_cup, pendingIntent)
            return views
        }

        fun updateAllWidgets(
            context: Context,
            state: AwakeState,
            appWidgetManager: AppWidgetManager? = AppWidgetManager.getInstance(context),
            appWidgetIds: IntArray? = null
        ) {
            val manager = appWidgetManager ?: return
            val ids = appWidgetIds ?: manager.getAppWidgetIds(
                ComponentName(context, AwakeSquircleWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            val views = buildRemoteViews(context, state)
            manager.updateAppWidget(ids, views)
        }
    }
}
