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
 * Publishes and dynamically updates the 4 right-click Taskbar context menu shortcuts.
 * Re-binds `ShortcutInfo.Builder.setActivity(...)` to the currently enabled `<activity-alias>`
 * whenever the Coffee Cup icon switches between Empty and Full.
 */
class AndroidShortcutPublisher(
    private val context: Context,
    private val iconSwitcher: AndroidIconSwitcher
) : ShortcutPublisherPort {

    override fun publishShortcuts(state: AwakeState) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        val activeAlias = iconSwitcher.getActiveAliasComponent()

        val statusLabelShort = if (state.isAwake) {
            "☕ Awake (${formatDurationShort(state.durationMinutes)})"
        } else {
            "⚪ Normal Suspend"
        }
        val statusLabelLong = if (state.isAwake) {
            "☕ Awake: ${formatDurationLong(state.durationMinutes)} (Click to Suspend)"
        } else {
            "⚪ Status: Normal Suspend (Click to Keep Awake)"
        }
        val statusIconRes = if (state.isAwake) {
            R.drawable.ic_stat_coffee_full
        } else {
            R.drawable.ic_stat_coffee_empty
        }

        val timerLabelShort = "⏱️ Timer: ${formatDurationShort(state.durationMinutes)}"
        val timerLabelLong = "⏱️ Cycle Timer (${formatDurationLong(state.durationMinutes)})"

        val pulseStateText = if (state.tapSimulationEnabled) "ON" else "OFF"
        val pulseLabelShort = "🖱️ Tap Sim: $pulseStateText (${state.tapIntervalSeconds}s)"
        val pulseLabelLong = "🖱️ Tap Simulator: $pulseStateText (Every ${state.tapIntervalSeconds}s)"

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

            ShortcutInfo.Builder(context, ID_TOGGLE_PULSE)
                .setActivity(activeAlias)
                .setRank(2)
                .setShortLabel(pulseLabelShort)
                .setLongLabel(pulseLabelLong)
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_pulse))
                .setIntent(createShortcutActionIntent(ACTION_SHORTCUT_TOGGLE_TAP_SIM))
                .build(),

            ShortcutInfo.Builder(context, ID_OPEN_SETTINGS)
                .setActivity(activeAlias)
                .setRank(3)
                .setShortLabel("⚙️ Awake Settings…")
                .setLongLabel("⚙️ Keep Awake Settings…")
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_settings))
                .setIntent(createSettingsIntent())
                .build()
        )

        shortcutManager.dynamicShortcuts = shortcuts
        shortcutManager.updateShortcuts(shortcuts)
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
        const val ID_TOGGLE_PULSE = "shortcut_toggle_pulse"
        const val ID_OPEN_SETTINGS = "shortcut_open_settings"

        const val ACTION_SHORTCUT_TOGGLE_AWAKE = "com.meridian11.books.awake.action.TOGGLE_AWAKE"
        const val ACTION_SHORTCUT_CYCLE_TIMER = "com.meridian11.books.awake.action.CYCLE_TIMER"
        const val ACTION_SHORTCUT_TOGGLE_TAP_SIM = "com.meridian11.books.awake.action.TOGGLE_TAP_SIM"
        const val ACTION_OPEN_SETTINGS = "com.meridian11.books.awake.action.OPEN_SETTINGS"

        const val SHORTCUT_ACTION_ACTIVITY_CLASS =
            "com.meridian11.books.awake.ui.ShortcutActionActivity"
        const val SETTINGS_DIALOG_ACTIVITY_CLASS =
            "com.meridian11.books.awake.ui.SettingsDialogActivity"

        fun formatDurationShort(durationMinutes: Int): String = when (durationMinutes) {
            0 -> "Indefinite"
            60 -> "1h"
            120 -> "2h"
            240 -> "4h"
            else -> "${durationMinutes}m"
        }

        fun formatDurationLong(durationMinutes: Int): String = when (durationMinutes) {
            0 -> "Indefinite"
            60 -> "1 Hour"
            120 -> "2 Hours"
            240 -> "4 Hours"
            else -> "$durationMinutes Minutes"
        }
    }
}
