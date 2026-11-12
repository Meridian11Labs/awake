package com.meridian11.books.awake.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import kotlinx.coroutines.launch

/**
 * Zero-window trampoline Activity invoked by right-click App Shortcuts on the
 * Googlebook Taskbar icon (Toggle Status or Cycle Duration Preset).
 */
class ShortcutActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        suppressWindowTransitions()
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            finish()
            suppressWindowTransitions()
            return
        }

        val app = applicationContext as? AwakeApp
        val action = intent?.action
        if (app != null) {
            app.appScope.launch {
                try {
                    when (action) {
                        AndroidShortcutPublisher.ACTION_SHORTCUT_TOGGLE_AWAKE -> {
                            AndroidShortcutPublisher.reportShortcutUsed(
                                this@ShortcutActionActivity,
                                AndroidShortcutPublisher.ID_STATUS_TOGGLE
                            )
                            app.controller.toggle()
                        }
                        AndroidShortcutPublisher.ACTION_SHORTCUT_CYCLE_TIMER -> {
                            AndroidShortcutPublisher.reportShortcutUsed(
                                this@ShortcutActionActivity,
                                AndroidShortcutPublisher.ID_PRESET_TIMER
                            )
                            app.controller.cycleDurationPresetAndApply()
                        }
                    }
                } finally {
                    finish()
                    suppressWindowTransitions()
                }
            }
        } else {
            finish()
            suppressWindowTransitions()
        }
    }

    private fun suppressWindowTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
