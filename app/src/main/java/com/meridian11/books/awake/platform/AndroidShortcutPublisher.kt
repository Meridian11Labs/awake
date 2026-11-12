package com.meridian11.books.awake.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.domain.ShortcutPublisherPort

/**
 * Publishes and dynamically updates the 3 right-click Taskbar context menu shortcuts
 * using Material Design themed vector icons (zero color emojis).
 * Re-binds `ShortcutInfo.Builder.setActivity(...)` to the currently enabled `<activity-alias>`
 * whenever the Coffee Cup icon switches between Empty and Full.
 */
class AndroidShortcutPublisher(
    private val context: Context,
    private val iconSwitcher: AndroidIconSwitcher
) : ShortcutPublisherPort {

    private data class PublishedShortcutKey(
        val activeAlias: ComponentName,
        val isAwake: Boolean,
        val durationMinutes: Int
    )

    @Volatile
    private var lastPublishedKey: PublishedShortcutKey? = null

    override fun publishShortcuts(state: AwakeState) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        val activeAlias = iconSwitcher.getActiveAliasComponent()
        val key = PublishedShortcutKey(
            activeAlias = activeAlias,
            isAwake = state.isAwake,
            durationMinutes = state.durationMinutes
        )

        val existingCount = runCatching { shortcutManager.dynamicShortcuts.size }.getOrDefault(0)
        if (lastPublishedKey == key && existingCount == 3) {
            return
        }

        val durationShort = formatDurationShort(state.durationMinutes)
        val durationLong = formatDurationLong(state.durationMinutes)

        val statusLabelShort = if (state.isAwake) {
            context.getString(R.string.shortcut_status_awake_short, durationShort)
        } else {
            context.getString(R.string.shortcut_status_suspend_short)
        }
        val statusLabelLong = if (state.isAwake) {
            context.getString(R.string.shortcut_status_awake_long, durationLong)
        } else {
            context.getString(R.string.shortcut_status_suspend_long)
        }
        val statusIconRes = if (state.isAwake) {
            R.drawable.ic_stat_coffee_full
        } else {
            R.drawable.ic_stat_coffee_empty
        }

        val timerLabelShort = context.getString(R.string.shortcut_timer_short, durationShort)
        val timerLabelLong = context.getString(R.string.shortcut_timer_long, durationLong)

        val shortcuts = listOf(
            ShortcutInfo.Builder(context, ID_STATUS_TOGGLE)
                .setActivity(activeAlias)
                .setRank(0)
                .setShortLabel(statusLabelShort)
                .setLongLabel(statusLabelLong)
                .setIcon(Icon.createWithResource(context, statusIconRes))
                .setIntent(createShortcutActionIntent(ACTION_SHORTCUT_TOGGLE_AWAKE))
                .build(),

            ShortcutInfo.Builder(context, ID_PRESET_TIMER)
                .setActivity(activeAlias)
                .setRank(1)
                .setShortLabel(timerLabelShort)
                .setLongLabel(timerLabelLong)
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_timer))
                .setIntent(createShortcutActionIntent(ACTION_SHORTCUT_CYCLE_TIMER))
                .build(),

            ShortcutInfo.Builder(context, ID_OPEN_SETTINGS)
                .setActivity(activeAlias)
                .setRank(2)
                .setShortLabel(context.getString(R.string.shortcut_open_settings_short))
                .setLongLabel(context.getString(R.string.shortcut_open_settings_long))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_settings))
                .setIntent(createSettingsIntent())
                .build()
        )

        runCatching {
            shortcutManager.dynamicShortcuts = shortcuts
            lastPublishedKey = key
        }
    }

    private fun createShortcutActionIntent(action: String): Intent {
        return Intent(action).apply {
            setComponent(ComponentName(context.packageName, SHORTCUT_ACTION_ACTIVITY_CLASS))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    private fun createSettingsIntent(): Intent {
        return Intent(ACTION_OPEN_SETTINGS).apply {
            setComponent(ComponentName(context.packageName, SETTINGS_DIALOG_ACTIVITY_CLASS))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    companion object {
        const val ID_STATUS_TOGGLE = "shortcut_status_toggle"
        const val ID_PRESET_TIMER = "shortcut_preset_timer"
        const val ID_OPEN_SETTINGS = "shortcut_open_settings"

        const val ACTION_SHORTCUT_TOGGLE_AWAKE = "com.meridian11.books.awake.action.TOGGLE_AWAKE"
        const val ACTION_SHORTCUT_CYCLE_TIMER = "com.meridian11.books.awake.action.CYCLE_TIMER"
        const val ACTION_OPEN_SETTINGS = "com.meridian11.books.awake.action.OPEN_SETTINGS"

        const val SHORTCUT_ACTION_ACTIVITY_CLASS =
            "com.meridian11.books.awake.ui.ShortcutActionActivity"
        const val SETTINGS_DIALOG_ACTIVITY_CLASS =
            "com.meridian11.books.awake.ui.SettingsDialogActivity"

        fun reportShortcutUsed(context: Context, shortcutId: String) {
            runCatching {
                context.getSystemService(ShortcutManager::class.java)?.reportShortcutUsed(shortcutId)
            }
        }

        fun formatDurationShort(durationMinutes: Int): String = when (durationMinutes) {
            0 -> "Infinite"
            60 -> "1h"
            120 -> "2h"
            240 -> "4h"
            else -> "${durationMinutes}m"
        }

        fun formatDurationLong(durationMinutes: Int): String = when (durationMinutes) {
            0 -> "Infinite"
            60 -> "1 Hour"
            120 -> "2 Hours"
            240 -> "4 Hours"
            else -> "$durationMinutes Minutes"
        }

        fun formatTimerSubtitle(durationMinutes: Int): String = when (durationMinutes) {
            0 -> "Infinite"
            60 -> "1h timer"
            120 -> "2h timer"
            else -> "${durationMinutes}m timer"
        }
    }
}
