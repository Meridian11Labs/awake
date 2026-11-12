package com.meridian11.books.awake.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import com.meridian11.books.awake.ui.SettingsDialogActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Responsive Material 3 Expressive Coffee Cup Widget (`1x1` default squircle, resizable to `2x1` and `Expanded`).
 * - Default `1x1` (`COMPACT_1X1`): Full-bleed squircle coffee cup button (Half-Empty Pastel Blue when inactive, Filled Pastel Orange when active).
 * - Medium `2x1` (`MEDIUM_2X1`): Horizontal hero banner with coffee cup badge, Awake/Sleeping status + live countdown chronometer, interactive Duration Cycle Pill (`Infinite | 30m | 1h | 2h`), and Settings button.
 * - Expanded (`EXPANDED`, `2x2+` / `3x2+`): Elevated Material 3 card with Hero Toggle Row, 4-button Segmented Duration Pills (`Infinite | 30m | 1h | 2h`), and Settings launch button.
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
        val app = context.applicationContext as? AwakeApp ?: return
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                withTimeoutOrNull(WIDGET_UPDATE_TIMEOUT_MS) {
                    val state = app.repository.stateFlow.first()
                    for (widgetId in appWidgetIds) {
                        val options = appWidgetManager.getAppWidgetOptions(widgetId)
                        appWidgetManager.updateAppWidget(
                            widgetId,
                            buildResponsiveRemoteViews(context, state, options)
                        )
                    }
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
        val app = context.applicationContext as? AwakeApp ?: return
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                withTimeoutOrNull(WIDGET_UPDATE_TIMEOUT_MS) {
                    val state = app.repository.stateFlow.first()
                    appWidgetManager.updateAppWidget(
                        appWidgetId,
                        buildResponsiveRemoteViews(context, state, newOptions)
                    )
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.meridian11.books.awake.action.WIDGET_TOGGLE"
        const val ACTION_WIDGET_CYCLE_DURATION = "com.meridian11.books.awake.action.WIDGET_CYCLE_DURATION"
        const val ACTION_WIDGET_SET_DURATION = "com.meridian11.books.awake.action.WIDGET_SET_DURATION"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_TARGET_AWAKE = "extra_target_awake"

        private const val WIDGET_UPDATE_TIMEOUT_MS = 8_000L
        private const val REQUEST_CODE_TOGGLE_ON = 401
        private const val REQUEST_CODE_OPEN_SETTINGS = 402
        private const val REQUEST_CODE_TOGGLE_OFF = 403
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
                widthDp >= 200 && heightDp >= 184 -> WidgetSizeBucket.EXPANDED
                widthDp >= 180 && heightDp >= 56 -> WidgetSizeBucket.MEDIUM_2X1
                else -> WidgetSizeBucket.COMPACT_1X1
            }
        }

        fun updateAllWidgets(context: Context, state: AwakeState) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, AwakeSquircleWidgetProvider::class.java)
            val ids = runCatching { manager.getAppWidgetIds(component) }.getOrNull()
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
            @Suppress("UNUSED_PARAMETER") options: Bundle?
        ): RemoteViews {
            val compactViews = buildBucketRemoteViews(context, state, WidgetSizeBucket.COMPACT_1X1)
            val mediumViews = buildBucketRemoteViews(context, state, WidgetSizeBucket.MEDIUM_2X1)
            val expandedViews = buildBucketRemoteViews(context, state, WidgetSizeBucket.EXPANDED)

            return RemoteViews(
                mapOf(
                    SizeF(28f, 28f) to compactViews,
                    SizeF(180f, 56f) to mediumViews,
                    SizeF(200f, 184f) to expandedViews
                )
            )
        }

        internal fun buildBucketRemoteViews(
            context: Context,
            state: AwakeState,
            bucket: WidgetSizeBucket
        ): RemoteViews {
            val spec = resolveVisualSpec(state)
            val views = RemoteViews(context.packageName, bucket.layoutRes)
            val togglePendingIntent = createTogglePendingIntent(
                context = context,
                targetAwake = !state.isAwake
            )

            // Primary Coffee Cup + Squircle Hero Container
            views.setImageViewResource(R.id.img_widget_cup, spec.cupIconRes)
            views.setContentDescription(R.id.img_widget_cup, spec.contentDescription)
            views.setInt(R.id.widget_squircle_container, "setBackgroundResource", spec.backgroundRes)
            views.setOnClickPendingIntent(R.id.widget_squircle_container, togglePendingIntent)

            when (bucket) {
                WidgetSizeBucket.COMPACT_1X1 -> {
                    // Flattened root IS widget_squircle_container in COMPACT_1X1
                }
                WidgetSizeBucket.MEDIUM_2X1 -> {
                    views.setInt(R.id.widget_cup_badge, "setBackgroundResource", spec.badgeBackgroundRes)
                    views.setTextViewText(
                        R.id.txt_widget_title,
                        context.getString(
                            if (state.isAwake) R.string.widget_title_awake_short else R.string.widget_title_asleep_short
                        )
                    )
                    bindSubtitleOrChronometer(
                        context = context,
                        views = views,
                        state = state,
                        fallbackSubtitle = context.getString(
                            if (state.isAwake) R.string.widget_subtitle_active_short else R.string.widget_subtitle_normal_short
                        )
                    )

                    val durChipText = when (state.durationMinutes) {
                        0 -> context.getString(R.string.widget_duration_infinity_symbol)
                        60 -> context.getString(R.string.widget_duration_1h)
                        120 -> context.getString(R.string.widget_duration_2h)
                        else -> context.getString(R.string.widget_duration_minutes_format, state.durationMinutes)
                    }
                    views.setTextViewText(R.id.btn_widget_cycle_duration, durChipText)
                    views.setInt(
                        R.id.btn_widget_cycle_duration,
                        "setBackgroundResource",
                        if (state.isAwake) R.drawable.bg_chip_accent else R.drawable.bg_segmented_track
                    )
                    views.setTextColor(
                        R.id.btn_widget_cycle_duration,
                        ContextCompat.getColor(
                            context,
                            if (state.isAwake) R.color.chip_accent_text else R.color.chip_unselected_text
                        )
                    )
                    val cycleContentDesc = context.getString(
                        R.string.widget_cd_cycle_duration,
                        AndroidShortcutPublisher.formatDurationLong(state.durationMinutes)
                    )
                    views.setContentDescription(R.id.btn_widget_cycle_duration_hit, cycleContentDesc)
                    views.setOnClickPendingIntent(
                        R.id.btn_widget_cycle_duration_hit,
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
                        context.getString(
                            if (state.isAwake) R.string.widget_title_keeping_awake else R.string.widget_title_normal_sleep
                        )
                    )
                    bindSubtitleOrChronometer(
                        context = context,
                        views = views,
                        state = state,
                        fallbackSubtitle = formatExpandedSubtitle(context, state)
                    )
                    views.setTextViewText(
                        R.id.txt_widget_state_pill,
                        context.getString(if (state.isAwake) R.string.widget_pill_on else R.string.widget_pill_off)
                    )
                    views.setInt(
                        R.id.txt_widget_state_pill,
                        "setBackgroundResource",
                        if (state.isAwake) R.drawable.bg_chip_accent else R.drawable.bg_segmented_track
                    )
                    views.setTextColor(
                        R.id.txt_widget_state_pill,
                        ContextCompat.getColor(
                            context,
                            if (state.isAwake) R.color.chip_accent_text else R.color.chip_unselected_text
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
                        views.setContentDescription(
                            viewId,
                            context.getString(
                                if (isSelected) R.string.widget_cd_preset_selected else R.string.widget_cd_preset_unselected,
                                AndroidShortcutPublisher.formatDurationLong(minutes)
                            )
                        )
                        views.setOnClickPendingIntent(
                            viewId,
                            createDurationPendingIntent(context, minutes)
                        )
                    }

                    // Open Settings Window Button
                    views.setOnClickPendingIntent(
                        R.id.btn_widget_open_settings,
                        createOpenSettingsPendingIntent(context)
                    )
                }
            }
            return views
        }

        private fun bindSubtitleOrChronometer(
            context: Context,
            views: RemoteViews,
            state: AwakeState,
            fallbackSubtitle: String
        ) {
            if (state.isAwake && state.sessionExpiresAtEpochMillis > 0L) {
                views.setViewVisibility(R.id.txt_widget_subtitle, View.GONE)
                views.setViewVisibility(R.id.chrono_widget_remaining, View.VISIBLE)
                views.setChronometerCountDown(R.id.chrono_widget_remaining, true)
                views.setChronometer(
                    R.id.chrono_widget_remaining,
                    state.sessionExpiresAtEpochMillis,
                    context.getString(R.string.widget_chrono_format),
                    true
                )
            } else {
                views.setChronometer(R.id.chrono_widget_remaining, 0L, null, false)
                views.setViewVisibility(R.id.chrono_widget_remaining, View.GONE)
                views.setViewVisibility(R.id.txt_widget_subtitle, View.VISIBLE)
                views.setTextViewText(R.id.txt_widget_subtitle, fallbackSubtitle)
            }
        }

        private fun formatExpandedSubtitle(context: Context, state: AwakeState): String {
            return if (state.isAwake) {
                AndroidShortcutPublisher.formatTimerSubtitle(state.durationMinutes)
            } else {
                val preset = AndroidShortcutPublisher.formatDurationShort(state.durationMinutes)
                context.getString(R.string.widget_subtitle_tap_preset, preset)
            }
        }

        private fun createTogglePendingIntent(context: Context, targetAwake: Boolean): PendingIntent {
            val toggleIntent = Intent(context, AwakeWidgetActionReceiver::class.java).apply {
                action = ACTION_WIDGET_TOGGLE
                putExtra(EXTRA_TARGET_AWAKE, targetAwake)
            }
            val requestCode = if (targetAwake) REQUEST_CODE_TOGGLE_ON else REQUEST_CODE_TOGGLE_OFF
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createCycleDurationPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AwakeWidgetActionReceiver::class.java).apply {
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
            val intent = Intent(context, AwakeWidgetActionReceiver::class.java).apply {
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

        private fun createOpenSettingsPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, SettingsDialogActivity::class.java).apply {
                action = AndroidShortcutPublisher.ACTION_OPEN_SETTINGS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
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
