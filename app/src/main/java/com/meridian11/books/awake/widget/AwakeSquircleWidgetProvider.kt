package com.meridian11.books.awake.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.ui.SettingsDialogActivity
import kotlinx.coroutines.launch

/**
 * Responsive Material 3 Expressive Coffee Cup Widget (`1x1` default squircle, resizable to `2x1` and `Expanded`).
 * - Default `1x1` (`COMPACT_1X1`): Full-bleed squircle coffee cup button (Half-Empty Pastel Blue when inactive, Filled Pastel Orange when active).
 * - Medium `2x1` (`MEDIUM_2X1`): Horizontal hero banner with coffee cup badge, Awake/Sleeping status, interactive Duration Cycle Pill (`Infinite | 30m | 1h | 2h`), and Settings button.
 * - Expanded (`EXPANDED`, `2x2+` / `3x2+`): Elevated Material 3 card with Hero Toggle Row, 4-button Segmented Duration Pills (`Infinite | 30m | 1h | 2h`), Tap Sim toggle, and Settings launch button.
 */
class AwakeSquircleWidgetProvider : AppWidgetProvider() {

    enum class WidgetSizeBucket(@param:LayoutRes val layoutRes: Int) {
        COMPACT_1X1(R.layout.widget_awake_squircle),
        MEDIUM_2X1(R.layout.widget_awake_medium),
        EXPANDED(R.layout.widget_awake_expanded)
    }

    data class WidgetVisualSpec(
        @param:DrawableRes val cupIconRes: Int,
        @param:DrawableRes val backgroundRes: Int,
        @param:DrawableRes val badgeBackgroundRes: Int,
        val contentDescription: String
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as AwakeApp
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                val state = app.controller.syncCurrentState()
                for (widgetId in appWidgetIds) {
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    appWidgetManager.updateAppWidget(widgetId, buildResponsiveRemoteViews(context, state, options))
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val app = context.applicationContext as AwakeApp
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                val state = app.controller.syncCurrentState()
                appWidgetManager.updateAppWidget(
                    appWidgetId,
                    buildResponsiveRemoteViews(context, state, newOptions)
                )
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val app = context.applicationContext as AwakeApp
        when (intent.action) {
            ACTION_WIDGET_TOGGLE -> {
                val pendingResult = goAsync()
                app.appScope.launch {
                    try {
                        val updated = app.controller.toggle()
                        updateAllWidgets(context, updated)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ACTION_WIDGET_CYCLE_DURATION -> {
                val pendingResult = goAsync()
                app.appScope.launch {
                    try {
                        val updated = app.controller.cycleDurationPresetAndApply()
                        updateAllWidgets(context, updated)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ACTION_WIDGET_SET_DURATION -> {
                val minutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)
                val pendingResult = goAsync()
                app.appScope.launch {
                    try {
                        val updated = app.controller.enableWithDuration(minutes)
                        updateAllWidgets(context, updated)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ACTION_WIDGET_TOGGLE_TAP_SIM -> {
                val pendingResult = goAsync()
                app.appScope.launch {
                    try {
                        val updated = app.controller.toggleTapSimulation()
                        updateAllWidgets(context, updated)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.meridian11.books.awake.action.WIDGET_TOGGLE"
        const val ACTION_WIDGET_CYCLE_DURATION = "com.meridian11.books.awake.action.WIDGET_CYCLE_DURATION"
        const val ACTION_WIDGET_SET_DURATION = "com.meridian11.books.awake.action.WIDGET_SET_DURATION"
        const val ACTION_WIDGET_TOGGLE_TAP_SIM = "com.meridian11.books.awake.action.WIDGET_TOGGLE_TAP_SIM"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

        private const val REQUEST_CODE_TOGGLE = 401
        private const val REQUEST_CODE_OPEN_SETTINGS = 402
        private const val REQUEST_CODE_TAP_SIM = 403
        private const val REQUEST_CODE_CYCLE_DURATION = 404
        private const val REQUEST_CODE_DURATION_BASE = 410

        fun resolveVisualSpec(state: AwakeState): WidgetVisualSpec {
            return if (state.isAwake) {
                WidgetVisualSpec(
                    cupIconRes = R.drawable.ic_widget_cup_full,
                    backgroundRes = R.drawable.bg_widget_squircle_full,
                    badgeBackgroundRes = R.drawable.bg_widget_cup_badge_full,
                    contentDescription = "Awake active (filled coffee cup). Tap to allow sleep."
                )
            } else {
                WidgetVisualSpec(
                    cupIconRes = R.drawable.ic_widget_cup_empty,
                    backgroundRes = R.drawable.bg_widget_squircle_empty,
                    badgeBackgroundRes = R.drawable.bg_widget_cup_badge_empty,
                    contentDescription = "Awake inactive (half-empty coffee cup). Tap to keep awake."
                )
            }
        }

        fun resolveWidgetSizeBucket(widthDp: Int, heightDp: Int): WidgetSizeBucket {
            return when {
                heightDp >= 135 && widthDp >= 130 -> WidgetSizeBucket.EXPANDED
                widthDp >= 130 -> WidgetSizeBucket.MEDIUM_2X1
                else -> WidgetSizeBucket.COMPACT_1X1
            }
        }

        fun updateAllWidgets(context: Context, state: AwakeState) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, AwakeSquircleWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids != null && ids.isNotEmpty()) {
                for (widgetId in ids) {
                    val options = manager.getAppWidgetOptions(widgetId)
                    manager.updateAppWidget(widgetId, buildResponsiveRemoteViews(context, state, options))
                }
            }
        }

        internal fun buildResponsiveRemoteViews(
            context: Context,
            state: AwakeState,
            options: Bundle?
        ): RemoteViews {
            val compactViews = buildBucketRemoteViews(context, state, WidgetSizeBucket.COMPACT_1X1)
            val mediumViews = buildBucketRemoteViews(context, state, WidgetSizeBucket.MEDIUM_2X1)
            val expandedViews = buildBucketRemoteViews(context, state, WidgetSizeBucket.EXPANDED)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return RemoteViews(
                    mapOf(
                        SizeF(28f, 28f) to compactViews,
                        SizeF(130f, 28f) to mediumViews,
                        SizeF(130f, 135f) to expandedViews
                    )
                )
            }

            val widthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
            val heightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
            return when (resolveWidgetSizeBucket(widthDp, heightDp)) {
                WidgetSizeBucket.COMPACT_1X1 -> compactViews
                WidgetSizeBucket.MEDIUM_2X1 -> mediumViews
                WidgetSizeBucket.EXPANDED -> expandedViews
            }
        }

        internal fun buildBucketRemoteViews(
            context: Context,
            state: AwakeState,
            bucket: WidgetSizeBucket
        ): RemoteViews {
            val spec = resolveVisualSpec(state)
            val views = RemoteViews(context.packageName, bucket.layoutRes)

            // Primary Coffee Cup + Squircle Hero Container
            views.setImageViewResource(R.id.img_widget_cup, spec.cupIconRes)
            views.setContentDescription(R.id.img_widget_cup, spec.contentDescription)
            views.setInt(R.id.widget_squircle_container, "setBackgroundResource", spec.backgroundRes)
            views.setOnClickPendingIntent(R.id.widget_squircle_container, createTogglePendingIntent(context))

            when (bucket) {
                WidgetSizeBucket.COMPACT_1X1 -> {
                    views.setOnClickPendingIntent(R.id.widget_root, createTogglePendingIntent(context))
                }
                WidgetSizeBucket.MEDIUM_2X1 -> {
                    views.setInt(R.id.widget_cup_badge, "setBackgroundResource", spec.badgeBackgroundRes)
                    views.setTextViewText(
                        R.id.txt_widget_title,
                        if (state.isAwake) "Awake" else "Asleep"
                    )
                    views.setTextViewText(
                        R.id.txt_widget_subtitle,
                        if (state.isAwake) {
                            if (state.tapSimulationEnabled) "Tap ${state.tapIntervalSeconds}s" else "Active"
                        } else {
                            "Normal"
                        }
                    )
                    val durChipText = when (state.durationMinutes) {
                        0 -> "∞"
                        30 -> "30m"
                        60 -> "1h"
                        120 -> "2h"
                        else -> "${state.durationMinutes}m"
                    }
                    views.setTextViewText(R.id.btn_widget_cycle_duration, durChipText)
                    views.setInt(
                        R.id.btn_widget_cycle_duration,
                        "setBackgroundResource",
                        if (state.isAwake) R.drawable.bg_chip_selected else R.drawable.bg_segmented_track
                    )
                    views.setTextColor(
                        R.id.btn_widget_cycle_duration,
                        ContextCompat.getColor(
                            context,
                            if (state.isAwake) R.color.chip_selected_text else R.color.chip_unselected_text
                        )
                    )
                    views.setOnClickPendingIntent(
                        R.id.btn_widget_cycle_duration,
                        createCycleDurationPendingIntent(context)
                    )
                    views.setOnClickPendingIntent(
                        R.id.btn_widget_open_settings,
                        createOpenSettingsPendingIntent(context)
                    )
                }
                WidgetSizeBucket.EXPANDED -> {
                    views.setInt(R.id.widget_cup_badge, "setBackgroundResource", spec.badgeBackgroundRes)
                    views.setTextViewText(
                        R.id.txt_widget_title,
                        if (state.isAwake) "Keeping Awake" else "Normal Sleep"
                    )
                    views.setTextViewText(
                        R.id.txt_widget_subtitle,
                        formatSubtitle(state)
                    )
                    views.setTextViewText(
                        R.id.txt_widget_state_pill,
                        if (state.isAwake) "ON" else "OFF"
                    )
                    views.setInt(
                        R.id.txt_widget_state_pill,
                        "setBackgroundResource",
                        if (state.isAwake) R.drawable.bg_chip_selected else R.drawable.bg_segmented_track
                    )
                    views.setTextColor(
                        R.id.txt_widget_state_pill,
                        ContextCompat.getColor(
                            context,
                            if (state.isAwake) R.color.chip_selected_text else R.color.chip_unselected_text
                        )
                    )

                    // Duration Segmented Pill Track (Infinite | 30m | 1h | 2h)
                    val durationButtons = listOf(
                        R.id.btn_widget_dur_infinite to 0,
                        R.id.btn_widget_dur_30 to 30,
                        R.id.btn_widget_dur_60 to 60,
                        R.id.btn_widget_dur_120 to 120
                    )
                    val selectedTextColor = ContextCompat.getColor(context, R.color.chip_selected_text)
                    val unselectedTextColor = ContextCompat.getColor(context, R.color.chip_unselected_text)

                    for ((viewId, minutes) in durationButtons) {
                        val isSelected = state.durationMinutes == minutes
                        views.setInt(
                            viewId,
                            "setBackgroundResource",
                            if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
                        )
                        views.setTextColor(viewId, if (isSelected) selectedTextColor else unselectedTextColor)
                        views.setOnClickPendingIntent(
                            viewId,
                            createDurationPendingIntent(context, minutes)
                        )
                    }

                    // Tap Simulation Toggle Pill
                    views.setTextViewText(
                        R.id.btn_widget_tap_sim,
                        if (state.tapSimulationEnabled) "Tap Sim: ON (${state.tapIntervalSeconds}s)" else "Tap Sim: OFF"
                    )
                    views.setInt(
                        R.id.btn_widget_tap_sim,
                        "setBackgroundResource",
                        if (state.tapSimulationEnabled) R.drawable.bg_chip_selected else R.drawable.bg_segmented_track
                    )
                    views.setTextColor(
                        R.id.btn_widget_tap_sim,
                        if (state.tapSimulationEnabled) selectedTextColor else unselectedTextColor
                    )
                    views.setOnClickPendingIntent(
                        R.id.btn_widget_tap_sim,
                        createTapSimPendingIntent(context)
                    )

                    // Open Settings Window Button
                    views.setOnClickPendingIntent(
                        R.id.btn_widget_open_settings,
                        createOpenSettingsPendingIntent(context)
                    )
                }
            }
            return views
        }

        private fun formatSubtitle(state: AwakeState): String {
            return if (state.isAwake) {
                val durLabel = if (state.durationMinutes == 0) "Infinite" else "${state.durationMinutes}m timer"
                val tapLabel = if (state.tapSimulationEnabled) " • Tap ${state.tapIntervalSeconds}s" else ""
                "$durLabel$tapLabel"
            } else {
                val preset = if (state.durationMinutes == 0) "Infinite" else "${state.durationMinutes}m"
                "Tap to activate ($preset preset)"
            }
        }

        private fun createTogglePendingIntent(context: Context): PendingIntent {
            val toggleIntent = Intent(context, AwakeSquircleWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_TOGGLE,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createCycleDurationPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AwakeSquircleWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_CYCLE_DURATION
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_CYCLE_DURATION,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createDurationPendingIntent(context: Context, minutes: Int): PendingIntent {
            val intent = Intent(context, AwakeSquircleWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_SET_DURATION
                putExtra(EXTRA_DURATION_MINUTES, minutes)
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_DURATION_BASE + minutes,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createTapSimPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AwakeSquircleWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE_TAP_SIM
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_TAP_SIM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createOpenSettingsPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, SettingsDialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_SETTINGS,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
